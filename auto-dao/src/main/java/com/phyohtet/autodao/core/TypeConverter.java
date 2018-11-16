package com.phyohtet.autodao.core;

public interface TypeConverter<E, DBType> {

	E fromDatabase(DBType type);
	
	DBType toDatabase(E type);
	
}
