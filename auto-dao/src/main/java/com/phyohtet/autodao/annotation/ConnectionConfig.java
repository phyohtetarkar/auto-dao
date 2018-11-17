package com.phyohtet.autodao.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface ConnectionConfig {
	String driverClassName();
	String url();
	String username() default "";
	String password() default "";
}
