package com.phyohtet.model.dao;

import java.util.List;

import com.phyohtet.autodao.annotation.Dao;
import com.phyohtet.autodao.annotation.Insert;
import com.phyohtet.autodao.annotation.Query;
import com.phyohtet.autodao.annotation.Update;
import com.phyohtet.model.Developer;

@Dao
public interface DeveloperDao {

	@Insert
	void insert(Developer dev);
	
	@Update
	void update(Developer dev);
	
	@Query(sql = "SELECT * FROM Developer WHERE id = ?")
	Developer findById(int id);
	
	@Query(sql = "SELECT * FROM Developer")
	List<Developer> findAll();
	
	@Query(sql = "SELECT * FROM Developer WHERE name LIKE ?")
	List<Developer> find(String name);
	
}
