package com.phyohtet.model;

import com.phyohtet.autodao.annotation.AutoDao;
import com.phyohtet.autodao.core.JDBCDao;
import com.phyohtet.model.dao.CateogryDao;

@AutoDao
public abstract class AppDatabase extends JDBCDao {

	public abstract void otherMethod1();
	
	public abstract String otherMethod2();
	
	public abstract CateogryDao cateogryDao();
	
}
