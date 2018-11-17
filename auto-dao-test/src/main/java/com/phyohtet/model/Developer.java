package com.phyohtet.model;

import com.phyohtet.autodao.annotation.Entity;
import com.phyohtet.autodao.annotation.PrimaryKey;

@Entity
public class Developer {

	@PrimaryKey(autoGenerate = true)
	private int id;
	private String name;
	private int age;

	// required default constructor
	public Developer() {
	}

	public Developer(String name, int age) {
		this.name = name;
		this.age = age;
	}

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

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "Developer [id=" + id + ", name=" + name + ", age=" + age + "]";
	}

}
