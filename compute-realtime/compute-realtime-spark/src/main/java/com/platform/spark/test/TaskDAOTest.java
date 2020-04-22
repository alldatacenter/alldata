package com.platform.spark.test;

import com.platform.spark.dao.ITaskDAO;
import com.platform.spark.dao.factory.DAOFactory;
import com.platform.spark.domain.Task;

/**
 * 任务管理DAO测试类
 * @author wulinhao
 *
 */
public class TaskDAOTest {
	
	public static void main(String[] args) {
		ITaskDAO taskDAO = DAOFactory.getTaskDAO();
		Task task = taskDAO.findById(2L);
		System.out.println(task.getTaskName());  
	}
	
}
