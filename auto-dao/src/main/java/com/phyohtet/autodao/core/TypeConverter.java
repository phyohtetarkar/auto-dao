package com.phyohtet.autodao.core;

public interface TypeConverter<E, DB> {

	E fromDatabase(DB db);
	
	DB toDatabase(E e);
	
}
