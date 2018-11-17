package com.phyohtet.autodao.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.phyohtet.autodao.core.TypeConverter;

@Retention(RUNTIME)
@Target(FIELD)
public @interface Converter {

	@SuppressWarnings("rawtypes")
	Class<? extends TypeConverter> using();
	
}
