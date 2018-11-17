package com.phyohtet.model.dao;

import java.util.List;

import com.phyohtet.autodao.annotation.Count;
import com.phyohtet.autodao.annotation.Dao;
import com.phyohtet.autodao.annotation.Insert;
import com.phyohtet.autodao.annotation.Query;
import com.phyohtet.autodao.annotation.Update;
import com.phyohtet.model.Category;

@Dao
public interface CateogryDao  {
	
	@Count(sql = "SELECT COUNT(*) FROM Category")
	int findCount();
	
	@Insert
	void insert(Category category);
	
	@Update
	void update(Category cc);
	
	@Query(sql = "SELECT * FROM Category WHERE id = ?")
	Category findById(int id);
	
	@Query(sql = "SELECT * FROM Category")
	List<Category> findAll();
	
	@Query(sql = "SELECT * FROM Category WHERE name = ? AND age > ?")
	List<Category> find(String v1, int v2);
	
	void other();
	
	public static void update() {
		
	}
}
