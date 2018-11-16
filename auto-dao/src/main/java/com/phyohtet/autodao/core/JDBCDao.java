package com.phyohtet.autodao.core;

import java.sql.Connection;
import java.sql.DriverManager;

public abstract class JDBCDao {
	
	private static final Object LOCK = new Object();
	private Connection con;
	
	public void init(ConnectionConfiguration config) throws Exception {
		synchronized (LOCK) {
			Class.forName(config.getClassName());
			con = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
			con.setAutoCommit(false);
		}
	}

	public void commitTransaction() throws Exception {
		if (con == null) {
			throw new IllegalArgumentException("No database connection is specified.");
		}
		con.commit();
		
	}
	
	public void rollbackTransaction() throws Exception {
		if (con == null) {
			throw new IllegalArgumentException("No database connection is specified.");
		}
		con.rollback();
	}
	
	public void close() throws Exception {
		if (con != null) {
			con.close();
			con = null;
		}
	}
	
}
