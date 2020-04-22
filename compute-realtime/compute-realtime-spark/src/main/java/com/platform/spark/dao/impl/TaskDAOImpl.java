package com.platform.spark.dao.impl;

import java.sql.ResultSet;

import com.platform.spark.dao.ITaskDAO;
import com.platform.spark.domain.Task;
import com.platform.spark.jdbc.JDBCHelper;

/**
 * 任务管理DAO实现类
 * @author wulinhao
 *
 */
public class TaskDAOImpl implements ITaskDAO {

	/**
	 * 根据主键查询任务
	 * @param taskid 主键
	 * @return 任务
	 */
	public Task findById(Long taskid) {
		final Task task = new Task();
		
		String sql = "select * from task where task_id=?";
		Object[] params = new Object[]{taskid};
		
		JDBCHelper jdbcHelper = JDBCHelper.getInstance();
		jdbcHelper.executeQuery(sql, params, new JDBCHelper.QueryCallback() {
			
			@Override
			public void process(ResultSet rs) throws Exception {
				if(rs.next()) {
					Long taskid = rs.getLong(1);
					String taskName = rs.getString(2);
					String createTime = rs.getString(3);
					String startTime = rs.getString(4);
					String finishTime = rs.getString(5);
					String taskType = rs.getString(6);
					String taskStatus = rs.getString(7);
					String taskParam = rs.getString(8);
					
					task.setTaskid(taskid);
					task.setTaskName(taskName); 
					task.setCreateTime(createTime); 
					task.setStartTime(startTime);
					task.setFinishTime(finishTime);
					task.setTaskType(taskType);  
					task.setTaskStatus(taskStatus);
					task.setTaskParam(taskParam);  
				}
			}
			
		});
		
		/**
		 * 说在后面的话：
		 * 
		 * 大家看到这个代码，包括后面的其他的DAO，就会发现，用JDBC进行数据库操作，最大的问题就是麻烦
		 * 你为了查询某些数据，需要自己编写大量的Domain对象的封装，数据的获取，数据的设置
		 * 造成大量很冗余的代码
		 * 
		 * 所以说，之前就是说，不建议用Scala来开发大型复杂的Spark的工程项目
		 * 因为大型复杂的工程项目，必定是要涉及很多第三方的东西的，MySQL只是最基础的，要进行数据库操作
		 * 可能还会有其他的redis、zookeeper等等
		 * 
		 * 如果你就用Scala，那么势必会造成与调用第三方组件的代码用java，那么就会变成scala+java混编
		 * 大大降低我们的开发和维护的效率
		 * 
		 * 此外，即使，你是用了scala+java混编
		 * 但是，真正最方便的，还是使用一些j2ee的开源框架，来进行第三方
		 * 技术的整合和操作，比如MySQL，那么可以用MyBatis/Hibernate，大大减少我们的冗余的代码
		 * 大大提升我们的开发速度和效率
		 * 
		 * 但是如果用了scala，那么用j2ee开源框架，进来，造成scala+java+j2ee开源框架混编
		 * 简直会造成你的spark工程的代码上的极度混乱和惨不忍睹
		 * 后期非常难以维护和交接
		 * 
		 */
		
		return task;
	}
	
}
