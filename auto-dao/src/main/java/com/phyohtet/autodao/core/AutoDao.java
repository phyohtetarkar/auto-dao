package com.phyohtet.autodao.core;

import com.phyohtet.autodao.AutoDaoException;
import com.phyohtet.autodao.annotation.ConnectionConfig;

public class AutoDao {
	
	public static <T> Builder<T> builder(Class<T> clazz) {
		return new Builder<T>(clazz);
	}
	
	public static class Builder<T> {
		
		private Class<T> clazz;
		
		private Builder(Class<T> clazz) {
			this.clazz = clazz;
		}
		
		@SuppressWarnings("unchecked")
		public T build() {
			try {
				ConnectionConfig config = clazz.getAnnotation(ConnectionConfig.class);
				
				Class<T> impl =  (Class<T>) Class.forName(clazz.getName().concat("Impl"));
				
				if (config == null) {
					throw new AutoDaoException("Connection config not found.");
				}
				
				ConnectionConfiguration cc = new ConnectionConfiguration();
				cc.setClassName(config.driverClassName());
				cc.setUrl(config.url());
				cc.setUsername(config.username().isEmpty() ? null : config.username());
				cc.setPassword(config.password().isEmpty() ? null : config.password());
				
				return impl.getDeclaredConstructor(ConnectionConfiguration.class).newInstance(cc);
			} catch (Exception e) {
				throw new AutoDaoException(e);
			}
		}
		
	}
	
}
