package com.phyohtet.model.dao;

import java.util.List;

import com.phyohtet.autodao.annotation.Dao;
import com.phyohtet.autodao.annotation.Insert;
import com.phyohtet.autodao.annotation.Query;
import com.phyohtet.autodao.annotation.Update;
import com.phyohtet.model.Category;

@Dao
public interface CateogryDao  {
	
	@Insert
	void insert(Category category);
	
	@Update
	void update(Category category);
	
	@Query(sql = "SELECT * FROM Category WHERE id = ?")
	Category findById(int id);
	
	@Query(sql = "SELECT * FROM Category")
	List<Category> findAll();
	
	@Query(sql = "SELECT * FROM Category WHERE name LIKE ?")
	List<Category> find(String name);
	
}
