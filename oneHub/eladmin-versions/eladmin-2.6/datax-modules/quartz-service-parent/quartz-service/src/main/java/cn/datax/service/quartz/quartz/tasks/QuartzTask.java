package cn.datax.service.quartz.quartz.tasks;

import org.springframework.stereotype.Component;

@Component("quartzTask")
public class QuartzTask {
	
	public void withParams(String params) {
		System.out.println("执行有参方法：" + params);
	}

	public void withoutParams() {
		System.out.println("执行无参方法");
	}
}