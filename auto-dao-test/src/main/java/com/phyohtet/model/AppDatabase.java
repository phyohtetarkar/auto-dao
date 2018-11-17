package com.phyohtet.model;

import com.phyohtet.autodao.annotation.AutoDao;
import com.phyohtet.autodao.annotation.ConnectionConfig;
import com.phyohtet.model.dao.CateogryDao;
import com.phyohtet.model.dao.DeveloperDao;

@AutoDao
@ConnectionConfig(
		driverClassName = "org.postgresql.Driver", 
		url = "jdbc:postgresql://localhost:5432/test",
		username = "postgres",
		password = "root")
public abstract class AppDatabase {

	public abstract CateogryDao cateogryDao();
	
	public abstract DeveloperDao developerDao();
	
}
