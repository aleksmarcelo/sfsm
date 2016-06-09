/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aleks.test;

import java.util.Arrays;

/**
 *
 * @author marcelosantos
 */
public class TESTE {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    // TODO code application logic here
    String s = "[net.gprs.local-ip]: [10.0.2.15]\n"
            + "[net.hostname]: [android-10889a9a69a384f0]\n"
            + "[net.qtaguid_enabled]: [0]\n"
            + "[net.tcp.default_init_rwnd]: [60]\n"
            + "[persist.sys.country]: [US]\n"
            + "[persist.sys.dalvik.vm.lib.2]: [libart.so]\n"
            + "[persist.sys.language]: [en]\n"
            + "[persist.sys.localevar]: []\n"
            + "[persist.sys.profiler_ms]: [0]\n"
            + "[persist.sys.timezone]: [GMT]";

    String t = "List of devices attached \r\n"
            + "emulator-5554          device product:sdk_google_phone_x86 model:Android_SDK_built_for_x86 device:generic_x86";
    
    
    System.out.println(Arrays.toString(t.split("[: \r\n]+")));

  }

}
