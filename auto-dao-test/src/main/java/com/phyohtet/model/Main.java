package com.phyohtet.model;

import java.util.List;

import com.phyohtet.autodao.core.AutoDao;
import com.phyohtet.model.dao.CateogryDao;
import com.phyohtet.model.dao.DeveloperDao;

public class Main {

	public static void main(String[] args) {
		// AutoDao initialization
		AppDatabase db = AutoDao.builder(AppDatabase.class).build();
		
		CateogryDao dao = db.cateogryDao();
		
		Category c = new Category();
		c.setName("Food");
		
		dao.insert(c);
		
		Category cc = dao.findById(c.getId());
		
		System.out.println(cc);
		
		List<Category> list = dao.findAll();
		
		System.out.println(list);
		
		DeveloperDao devDao = db.developerDao();
		
		Developer dev = new Developer("Phyo Htet", 26);
		
		devDao.insert(dev);
		
		System.out.println(devDao.findById(dev.getId()));
		
		System.out.println(devDao.findAll());
	}

}
