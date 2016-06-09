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
import java.util.HashMap;
import java.util.Map;
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
  private boolean gError; //error in State process (global to be accessed inside threads)
  private Exception gex; //Gobal variable to save the exception object
  private boolean gbContinue;
  Map<String, Object> gSave = new HashMap();
  
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

  Future future;

  public void mustStop() {
    gbContinue = false;
    future.cancel(true);
    cleanAndFinish();
  }

  @Override
  public void run() {
    execNode = listStates.get(0);
    gbContinue = true;

    while (gbContinue) {
      gError = false;
      gex = null;

      //run the node thread
      future = poolThread.submit(() -> {
        gError = execNodeThread(execNode);
      });

      //Wait for the thread completion
      try {
        future.get(execNode.getTimeout(), TimeUnit.MILLISECONDS);
        if (future.isCancelled())
          gError = true;

      } catch (InterruptedException | ExecutionException ex) {
        log.println("  \033[31m [Error] " + ex.getMessage() + "  " + ex.getStackTrace()[1]);
        gex = ex;
        gError = true;
      } catch (TimeoutException ex) {
        future.cancel(true);
        log.println("  \033[31m [Warn] Timeout retrying more " + execNode.getRetries() + " times");
        gex = ex;
        gError = true;
      } catch (NullPointerException ex) {
        future.cancel(true);
        log.println("  \033[31m [Error] Step Not Found " + ex.getMessage() + "  " + ex.getStackTrace()[1]);
        gex = ex;
        gError = true;
      }

      execNode = getNextNode(gError);
      if (execNode == null) {
        log.println("Last Step");
        break;
      }

    }

    cleanAndFinish();

  }

  private void cleanAndFinish() {
        //Terminates the thread pool
    poolThread.shutdownNow();
    onExit(gex); //Call exit method passing the exception (if it has) for the last node excecuted

    if (!gError)
      onSuccess();
  }
  
  //Born to be wild, ops, overridable ;-D
  public void onExit(Exception ex) {
  }

  //Called only when the FSM exit without any type of error
  public void onSuccess() {
  }

  private boolean execNodeThread(Node node) {
    boolean error = false;
    try {
      execNode.onEnter();
    } catch (Exception ex) {
      gex = ex;
      if (ex instanceof InterruptedException == false)
        log.printf("  \033[31m [Exception]  %s  in %s\n", ex.toString(), ex.getStackTrace()[0]);
      error = true;
    }

    return error;
  }

  private Node getNextNode(boolean hadError) {

    Node n = null;

    //If the node running went wrong
    if (hadError) {
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

    log.printf("  \u001B[34m  Next=(%s)  Error=%s \n", n, gError);

    return n;
  }

  private Node getNodeByName(String name) {
    for (Node node : listStates) {
      if (node.getName().equals(name))
        return node;
    }
    return null;
  }

  public void setLogStream(PrintStream out) {
    this.log = out;
  }

  /**
   * Save an object, so you will be able to see its value at the end or use it in other states
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
