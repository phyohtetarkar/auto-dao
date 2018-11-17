package com.phyohtet.autodao.core;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.phyohtet.autodao.AutoDaoException;
import com.phyohtet.autodao.annotation.ColumnInfo;
import com.phyohtet.autodao.annotation.Converter;
import com.phyohtet.autodao.annotation.Ignore;
import com.phyohtet.autodao.annotation.PrimaryKey;
import com.phyohtet.autodao.annotation.Table;

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
	
	public <T> Object insert(T t) {
		if (t == null) {
			throw new NullPointerException();
		}
		
		String sql = getInsertSql(t.getClass());
		
		try (PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			bindToStatement(stmt, t, predPrimaryKey);
			
			stmt.executeUpdate();
			
			try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
				return generatedKeys.next() ? generatedKeys.getObject(1) : 0;
			}
			
		} catch (Exception e) {
			throw new AutoDaoException(e);
		}
	}
	
	public <T> void update(T t) {
		if (t == null) {
			throw new NullPointerException();
		}
		
		String sql = getUpdateSql(t);
		
		try (PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			bindToStatement(stmt, t, f -> true);
			
			stmt.executeUpdate();
			
		} catch (Exception e) {
			throw new AutoDaoException(e);
		}
	}
	
	public <T> List<T> queryResultList(String query, Class<T> clazz, Object... args) {
		try (PreparedStatement stmt = con.prepareStatement(query)) {
			for (int i = 1; i <= args.length; i++) {
				stmt.setObject(i, args[i]);
			}
			
			try (ResultSet rs = stmt.executeQuery()) {
				List<T> list = new ArrayList<>();
				while (rs.next()) {
					list.add(bindToObject(rs, clazz));
					
				}
				return list;
			} 
		} catch (Exception e) {
			throw new AutoDaoException(e);
		}
	}

	public <T> T querySingleResult(String query, Class<T> clazz, Object... args) {
		try (PreparedStatement stmt = con.prepareStatement(query)) {
			for (int i = 1; i <= args.length; i++) {
				stmt.setObject(i, args[i]);
			}
			
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next() ? bindToObject(rs, clazz) : null;
			} 
		} catch (Exception e) {
			throw new AutoDaoException(e);
		}
	}
	
	public int queryCount(String query, Object... args) {
		try (PreparedStatement stmt = con.prepareStatement(query)) {
			for (int i = 1; i <= args.length; i++) {
				stmt.setObject(i, args[i]);
			}
			
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			} 
		} catch (Exception e) {
			throw new AutoDaoException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T bindToObject(ResultSet rs, Class<T> clazz) throws Exception {
		T obj = clazz.newInstance();

		for (Field f : clazz.getDeclaredFields()) {
			f.setAccessible(true);
			if (f.isAnnotationPresent(Ignore.class)) {
				continue;
			}

			String columnLabel = fieldToNameFunction.apply(f);
			Converter converter = f.getAnnotation(Converter.class);

			if (converter != null) {
				TypeConverter cv = converter.using().newInstance();
				f.set(obj, cv.fromDatabase(rs.getObject(columnLabel)));
			} else {
				f.set(obj, rs.getObject(columnLabel));
			}
		}

		return obj;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> void bindToStatement(PreparedStatement stmt, T t, Predicate<Field> skip) throws Exception {
		Field[] fields = t.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			f.setAccessible(true);
			
			if (skip.test(f)) {
				continue;
			}
			
			if (f.isAnnotationPresent(Ignore.class)) {
				continue;
			}
			
			Converter converter = f.getAnnotation(Converter.class);
			
			if (converter != null) {
				TypeConverter cv = converter.using().newInstance();
				stmt.setObject(i + 1, cv.toDatabase(f.get(t)));
			} else {
				stmt.setObject(i + 1, f.get(t));
			}
		}
	}
	
	private <T> String getInsertSql(Class<T> clazz) {
		Table table = clazz.getAnnotation(Table.class);
		String tableName = table != null ? table.name() : clazz.getSimpleName();
		StringBuilder sb = new StringBuilder("INSERT INTO " + tableName);

		List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
		
		String columns = fields.stream().filter(predIgnore)
			.filter(predPrimaryKey)
			.map(fieldToNameFunction)
			.collect(Collectors.joining(", ", " (", ") "));
		
		String values = fields.stream().filter(predIgnore)
				.filter(predPrimaryKey)
				.map(f -> "?")
				.collect(Collectors.joining(", ", " (", ") "));
		
		return sb.append(columns).append("VALUES").append(values).toString();
	}

	@SuppressWarnings("unchecked")
	private <T> String getUpdateSql(T t) {
		Class<T> clazz = (Class<T>) t.getClass();
		Table table = clazz.getAnnotation(Table.class);
		String tableName = table != null ? table.name() : clazz.getSimpleName();
		StringBuilder sb = new StringBuilder("UPDATE " + tableName + " SET");
		
		List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
		
		String update = fields.stream().filter(predIgnore)
			.filter(f -> !f.isAnnotationPresent(PrimaryKey.class))
			.map(fieldToNameFunction)
			.collect(Collectors.joining(" = ?, "));
		
		String where = fields.stream().filter(f -> f.isAnnotationPresent(PrimaryKey.class))
				.findFirst()
				.map(f -> {
					try {
						f.setAccessible(true);
						return " WHERE " + fieldToNameFunction.apply(f) + " = " + f.get(t);
					} catch (Exception e) {
						throw new AutoDaoException(e);
					} 
				}).orElse("");
		
		return sb.append(update).append(where).toString();
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
	
	private static final Function<Field, String> fieldToNameFunction = f -> {
		String columnName = f.getName();
		ColumnInfo columnInfo = f.getAnnotation(ColumnInfo.class);

		if (columnInfo != null) {
			columnName = columnInfo.name();
		}
		return columnName;
	};
	
	private static final Predicate<Field> predIgnore = f -> f.isAnnotationPresent(Ignore.class);
	
	private static final Predicate<Field> predPrimaryKey = f -> {
		PrimaryKey pk = f.getAnnotation(PrimaryKey.class);
		return pk == null || !pk.autoGenerate();
	};

}
