/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aleks.sfsm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Classe representará o núcleo do funcionamento da máquina de estado
 *
 *
 * @author Marcelo Aleksandravicius
 */
public class SimpleFSM extends Thread {

  ExecutorService poolThread = Executors.newSingleThreadExecutor();

  ArrayList<Node> listStates;

  private Node execNode;  //Node beeing executed
  private Node lastNode = null; //Used to change the name of the next state, pointing to the next on the list
  PrintStream log;
  private Exception gex; //Gobal variable to save the exception object
  private boolean gbContinue;
  private Map<String, Object> gSave = new ConcurrentHashMap();
  private Future future = null;
  private Node jumpToNode = null;

  /**
   * Creates a new Simple Finite State Machine
   */
  public SimpleFSM() {
    listStates = new ArrayList();
    log = new PrintStream(new NullOutputStream()); //Outstream to '/dev/null
  }

  /**
   * Adiciona um estado a ser executado
   *
   * @param node
   */
  private Node addNode(Node node) {

    //if the last is pointing to null, make it point to this one. 
    if (lastNode != null && lastNode.getNext() == null)
      lastNode.setNext(node.getName());

    //Add the state in the list
    listStates.add(node);

    lastNode = node;

    //Return the node to continue the set up, if necessary
    return node;
  }

  public Node addState(IState stateEnter) {
    return addState("STATE_" + listStates.size(), stateEnter);
  }

  public Node addState(String name, IState stateEnter) {
    //Add the state in the list
    Node node = new Node(name) {
      @Override
      public void onEnter() throws Exception {
        stateEnter.onEnter();
      }
    };

    //Return the node to continue the set up, if necessary
    return addNode(node);
  }

  /**
   * Print all the states and its parameters
   */
  public void print() {
    System.out.println("States:");
    System.out.println("--------");
    listStates.forEach((Node u) -> {
      System.out.println(u.getName() + "  ---> " + u);
    });
    System.out.println("\n");
  }

  /**
   * Starts the nodes without a new thread
   */
  public void startInSameThread() {
    run();
  }

  @Override
  public synchronized void start() {
    gbContinue = true;
    super.start();
  }

  public void moveToState(String state) {
    jumpToNode = getNodeByName(state);
    if (future != null)
      future.cancel(true);

  }

  public void mustStop() {
    gbContinue = false;
    if (future != null)
      future.cancel(true);
    gex = new CancellationException("Stoped forced");

    poolThread.shutdownNow();
  }

  @Override
  public void run() {
    execNode = listStates.get(0);

    while (gbContinue) {
      gex = null;

      try {

        //run the node thread
        future = poolThread.submit(() -> {

          //Treats the interrupt exception inside the thread
          try {
            execNodeThread(execNode);
          } catch (Exception ex) {
            gex = ex;
          }
        });

        //Wait for the thread completion
        future.get(execNode.getTimeout(), TimeUnit.MILLISECONDS);
//        if (future.isCancelled())
//          System.err.println("It must be treated outside!!!");

      } catch (InterruptedException | ExecutionException ex) {
        log.println("  \033[31m [Error] " + ex.getMessage() + "  " + ex.getStackTrace()[1]);
        gex = ex;
//         = true;
      } catch (TimeoutException ex) {
        future.cancel(true);
        log.println("  \033[31m [Warn] Timeout retrying more " + execNode.getRetries() + " times");
        gex = ex;
      } catch (NullPointerException ex) {
        future.cancel(true);
        log.println("  \033[31m [Error] Step Not Found " + ex.getMessage() + "  " + ex.getStackTrace()[1]);
        gex = ex;
      } catch (CancellationException ex) {
        gex = ex;
      }

      if (jumpToNode != null) {
        execNode = jumpToNode;
        jumpToNode = null;
        continue;
      }
      
      execNode = getNextNode();
      if (execNode == null) {
        log.println("## Last Step ##");
        break;
      }

    }

    cleanAndFinish();

  }

  private void cleanAndFinish() {
    //Terminates the thread pool
    poolThread.shutdownNow();
    onExit(gex); //Call exit method passing the exception (if it has) for the last node excecuted

    //Without exception
    if (gex == null)
      onSuccess();
  }

  //Born to be wild, ops, overridable ;-D
  public void onExit(Exception ex) {
  }

  //Called only when the FSM exit without any type of error
  public void onSuccess() {
  }

  /**
   * Encapsulate the call to the step code, in this way, in the future, we can
   * pre call and post call the work code.
   *
   * @param node
   * @throws Exception
   */
  private void execNodeThread(Node node) throws Exception {

    execNode.onEnter();

  }

  private Node getNextNode() {

    Node n = null;

    //If the node running went wrong
    if (gex != null) {
      int retries = execNode.getRetries();
      if (retries > 0) {
        execNode.setRetries(--retries);
        n = execNode;
      } else if (execNode.getNextOnError() != null)
        n = getNodeByName(execNode.getNextOnError());

    } else
      if (execNode.getNext() == null)
        n = null;
      else
        n = getNodeByName(execNode.getNext());

    log.printf("\u001B[34m   %s  Error=%s \n", n, gex);

    return n;
  }

  private Node getNodeByName(String name) {
    for (Node node : listStates) {
      if (node.getName().equalsIgnoreCase(name))
        return node;
    }
    System.err.printf("State [%s] not found !\n", name);
    return null;
  }

  public void setLogStream(PrintStream out) {
    this.log = out;
  }

  /**
   * Save an object, so you will be able to see its value at the end or use it
   * in other states
   *
   * @param str The key
   * @param value Some object
   */
  public void save(String str, Object value) {
    gSave.put(str, value);
  }

  public Object get(String key) {
    return gSave.get(key);
  }

  /**
   * Writes to null
   */
  public class NullOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
    }
  }

}
