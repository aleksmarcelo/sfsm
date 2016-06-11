package com.aleks.test;

import com.aleks.sfsm.Node;
import com.aleks.sfsm.SimpleFSM;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestFSM {

  public TestFSM() {

    final SimpleFSM fsm = new SimpleFSM() {
      @Override
      public void onExit(Exception ex) {
        System.out.println("(onExit)THE END with error=" + ex);
        System.out.println("vals = " + Arrays.toString((int[]) this.get("val")));
      }
    };

    int val[] = new int[]{5, 0};
    Node n2 = fsm.addState("T1", () -> {
      System.out.printf("First Node  pausa=%d secs\n", val[0]);
      fsm.save("val", val);
      TimeUnit.SECONDS.sleep(val[0]--);
    }).setTimeout(2000)
            .setRetries(5);

    fsm.addState(() -> {
      System.out.println("no name");
      if (val[1]++ == 0)
        throw new Exception("I don´t like zeros!");
    }).setNextOnError("T1");

    fsm.addState("T3", () -> {
      System.out.println("Third Node");
      val[0] = 7;
    }).setNext("T1");

    fsm.addState("end", () -> {
      System.out.println("** THTA´s THE END **");
    });

    //Redirect the log output to the sys out
    fsm.setLogStream(System.out);
    fsm.print();
    fsm.start();

//    System.out.println("Must stop now!!");
//    fsm.mustStop();


    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException ex) {
      Logger.getLogger(TestFSM.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    System.out.println("## Calling the 'end' state ##");
    fsm.moveToState("send");

  }

  public static void main(String[] args) throws Exception {
    new TestFSM();
    System.out.println("FIM");
  }
  
  
}
