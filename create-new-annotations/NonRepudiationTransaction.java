package com.im.imc.nonrepudiation.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation, any method that needs to be logged for non repudiation should be annotated with this (Interfaces can be annotated)
 * 
 * @author zfattuhi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NonRepudiationTransaction {

}
