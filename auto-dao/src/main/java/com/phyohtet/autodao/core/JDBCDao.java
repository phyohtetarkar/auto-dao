package com.phyohtet.autodao.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.phyohtet.autodao.AutoDaoException;

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

	public ResultSet executeQuery(String query, String[] columnNames, Object... args) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(query, columnNames);
		return stmt.executeQuery();
	}

	public void commitTransaction() {
		try {
			if (con == null) {
				throw new AutoDaoException("No database connection is specified.");
			}
			con.commit();
		} catch (Exception e) {
			throw new AutoDaoException(e);
		}

	}

	public void rollbackTransaction() {
		try {
			if (con == null) {
				throw new AutoDaoException("No database connection is specified.");
			}
			con.rollback();
		} catch (SQLException e) {
			throw new AutoDaoException(e);
		}
	}

	public void close() {
		try {
			if (con != null) {
				con.close();
				con = null;
			}
		} catch (SQLException e) {
			throw new AutoDaoException(e);
		}
	}

}
