package com.platform.spark.test;

/**
 * 单例模式Demo
 * 
 * @author wulinhao
 *
 */
public class Singleton {

	// 首先必须有一个私有的静态变量，来引用自己即将被创建出来的单例
	private static Singleton instance = null;
	
	/**
	 * 其次，必须对自己的构造方法使用private进行私有化
	 * 这样，才能保证，外界的代码不能随意的创建类的实例
	 */
	private Singleton() {
		
	}
	
	/**
	 * 最后，需要有一个共有的，静态方法
	 * 这个方法，负责创建唯一的实例，并且返回这个唯一的实例
	 * 
	 * 必须考虑到可能会出现的多线程并发访问安全的问题
	 * 就是说，可能会有多个线程同时过来获取单例，那么可能会导致创建多次单例
	 * 所以，这个方法，通常需要进行多线程并发访问安全的控制
	 * 
	 * 首先，就是，说到多线程并发访问安全的控制，大家觉得最简单的就是在方法上加入synchronized关键词
	 * public static synchronized Singleton getInstance()方法
	 * 但是这样做有一个很大的问题
	 * 在第一次调用的时候，的确是可以做到避免多个线程并发访问创建多个实例的问题
	 * 但是在第一次创建完实例以后，就会出现以后的多个线程并发访问这个方法的时候，就会在方法级别进行同步
	 * 导致并发性能大幅度降低
	 * 
	 * @return
	 */
	public static Singleton getInstance() {
		// 两步检查机制
		// 首先第一步，多个线程过来的时候，判断instance是否为null
		// 如果为null再往下走
		if(instance == null) {
			// 在这里，进行多个线程的同步
			// 同一时间，只能有一个线程获取到Singleton Class对象的锁
			// 进入后续的代码
			// 其他线程，都是只能够在原地等待，获取锁
			synchronized(Singleton.class) {
				// 只有第一个获取到锁的线程，进入到这里，会发现是instance是null
				// 然后才会去创建这个单例
				// 此后，线程，哪怕是走到了这一步，也会发现instance已经不是null了
				// 就不会反复创建一个单例
				if(instance == null) {
					instance = new Singleton();
				}
			}
		}
		return instance;
	}
	
}
