package com.phyohtet.model.dao;

import java.io.IOException;

import com.phyohtet.autodao.annotation.Dao;
import com.phyohtet.autodao.annotation.Insert;
import com.phyohtet.model.Category;

@Dao
public interface CateogryDao  {
	
	@Insert
	void insert(Category category) throws IOException;
	
	public static void update() {
		
	}
}
