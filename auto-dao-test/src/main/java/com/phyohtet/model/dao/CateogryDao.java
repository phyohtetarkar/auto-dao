package com.phyohtet.model.dao;

import com.phyohtet.autodao.annotation.Dao;
import com.phyohtet.autodao.annotation.Query;
import com.phyohtet.model.Category;

@Dao
public interface CateogryDao  {
	
	@Query
	void insert(Category category);
	
	public static void update() {
		
	}
}
