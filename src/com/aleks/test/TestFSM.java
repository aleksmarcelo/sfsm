package com.aleks.test;

import com.aleks.sfsm.Node;
import com.aleks.sfsm.SimpleFSM;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TestFSM {

  public TestFSM() {

    final SimpleFSM fsm = new SimpleFSM() {
      @Override
      public void onExit(Exception ex) {
        System.out.println("THE END with error=" + ex);
        System.out.println("vals = " + Arrays.toString(  (int[]) this.get("val")) );
      }
    };
    

    int val[] = new int[]{5, 0};
    Node n2 = fsm.addState("T1", () -> {
      System.out.printf("First Node  pausa=%d secs\n", val[0]);
      fsm.save("val", val );
      TimeUnit.SECONDS.sleep(val[0]--);
    }).setTimeout(2000)
      .setRetries(5);

    fsm.addState(() -> {
      System.out.println("no name");
      if (val[1]++ == 0) throw new Exception("I don´t like zeros!");
    }).setNextOnError("T1");

    
    fsm.addState("T3", () -> {
      System.out.println("Third Node");
      val[0] = 7;
    }).setNext("T1");

    
    
    //Redirect the log output to the sys out
    fsm.setLogStream(System.out);
    fsm.print();
//    fsm.startInSameThread();
    fsm.start();
    
//    System.out.println("Must stop now!!");
//    fsm.mustStop();

  }

  public static void main(String[] args) throws Exception {
    new TestFSM();
    System.out.println("FIM");
  }
}
