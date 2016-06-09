package com.aleks.sfsm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For the Future  !
 * To be used with annotations
 * @author Marcelo Aleksandravicius
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) 
public @interface State {

  String name();
  
  long timestamp();
  
  String next();
  
  
}
