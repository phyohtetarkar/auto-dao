package com.phyohtet.model;

import com.phyohtet.autodao.annotation.Entity;
import com.phyohtet.autodao.annotation.PrimaryKey;

@Entity
public class Category {

	@PrimaryKey(autoGenerate = true)
	private int id;
	private String name;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Category [id=" + id + ", name=" + name + "]";
	}
	
}
