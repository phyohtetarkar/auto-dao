package com.phyohtet.autodao.core;

import com.phyohtet.autodao.AutoDaoException;
import com.phyohtet.autodao.annotation.ConnectionConfig;

public class AutoDao {
	
	public static Builder builder(Class<? extends JDBCDao> clazz) {
		return new Builder(clazz);
	}
	
	public static class Builder {
		
		private Class<? extends JDBCDao> clazz;
		
		private Builder(Class<? extends JDBCDao> clazz) {
			this.clazz = clazz;
		}
		
		public JDBCDao build() {
			try {
				ConnectionConfig config = clazz.getAnnotation(ConnectionConfig.class);
				
				Class<?> impl =  Class.forName(clazz.getName().concat("Impl"));
				
				JDBCDao autoDao = (JDBCDao) impl.newInstance();
				
				if (config == null) {
					throw new AutoDaoException("Connection config not found.");
				}
				
				ConnectionConfiguration cc = new ConnectionConfiguration();
				cc.setClassName(config.driverClassName());
				cc.setUrl(config.url());
				cc.setUsername(cc.getUsername());
				cc.setPassword(config.password());
				
				autoDao.init(cc);
				
				return autoDao;
			} catch (Exception e) {
				throw new AutoDaoException(e);
			}
		}
		
	}
	
}
