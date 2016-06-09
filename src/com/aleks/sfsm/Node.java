/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aleks.sfsm;

/**
 *
 * @author Administrator
 */
public class Node implements IState {

  private Object value; //future use (perhaps...)
  private String name;  //State name
  private String next;  //Next State´s name
  private String nextOnError;  //Next State name if some error happens
  private int retries = 0; //Number of retries if an exceptions is throwed inside onEnter
  private long timeout = Long.MAX_VALUE; //Timeout

  public Node(String name, Object obj) {
    this.name = name;
    this.value = obj;
    next = null;
  }

  public Node(String name) {
    this(name, null);
  }

  public Node() {
    this(null, null);
  }

  /**
   * Responsible to 'encapsulate' the processing.
   * implementado
   *
   * @throws java.lang.Exception
   */
  @Override
  public void onEnter() throws Exception {
    throw new Exception("If you see this line, something is very very much wrong!!");
  }


  public String getName() {
    return name;
  }

//  public Node setName(String name) {
//    this.name = name;
//
//    return this;
//  }

  public long getTimeout() {
    return timeout;
  }

  public Node setTimeout(int timeout) {
    this.timeout = timeout;

    return this;
  }

  public int getRetries() {
    return retries;
  }

  public Node setRetries(int retries) {
    this.retries = retries;
    return this;
  }

  public Node setNext(String name) {
    this.next = name;
    return this;
  }

  public String getNext() {
    return next;
  }

  /////
  public String getNextOnError() {
    return nextOnError;
  }

  public Node setNextOnError(String nextOnError) {
    this.nextOnError = nextOnError;
    return this;
  }

  @Override
  public String toString() {
    return String.format("Name=%s  Next=%s  nextOnError=%s  timeout=%d  #retries=%d", name, next, nextOnError, timeout, retries);
  }



}
