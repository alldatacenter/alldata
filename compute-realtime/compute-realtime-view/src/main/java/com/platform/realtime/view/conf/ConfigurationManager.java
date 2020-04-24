package com.platform.realtime.view.conf;

import java.io.InputStream;
import java.util.Properties;

/**
 * 配置管理组件
 * 
 * 1、配置管理组件可以复杂，也可以很简单，对于简单的配置管理组件来说，只要开发一个类，可以在第一次访问它的
 * 		时候，就从对应的properties文件中，读取配置项，并提供外界获取某个配置key对应的value的方法
 * 2、如果是特别复杂的配置管理组件，那么可能需要使用一些软件设计中的设计模式，比如单例模式、解释器模式
 * 		可能需要管理多个不同的properties，甚至是xml类型的配置文件
 * 3、我们这里的话，就是开发一个简单的配置管理组件，就可以了
 * 
 * @author wulinhao
 *
 */
public class ConfigurationManager {
	
	// Properties对象使用private来修饰，就代表了其是类私有的
	// 那么外界的代码，就不能直接通过ConfigurationManager.prop这种方式获取到Properties对象
	// 之所以这么做，是为了避免外界的代码不小心错误的更新了Properties中某个key对应的value
	// 从而导致整个程序的状态错误，乃至崩溃
	private static Properties prop = new Properties();
	
	/**
	 * 静态代码块
	 * 
	 * Java中，每一个类第一次使用的时候，就会被Java虚拟机（JVM）中的类加载器，去从磁盘上的.class文件中
	 * 加载出来，然后为每个类都会构建一个Class对象，就代表了这个类
	 * 
	 * 每个类在第一次加载的时候，都会进行自身的初始化，那么类初始化的时候，会执行哪些操作的呢？
	 * 就由每个类内部的static {}构成的静态代码块决定，我们自己可以在类中开发静态代码块
	 * 类第一次使用的时候，就会加载，加载的时候，就会初始化类，初始化类的时候就会执行类的静态代码块
	 * 
	 * 因此，对于我们的配置管理组件，就在静态代码块中，编写读取配置文件的代码
	 * 这样的话，第一次外界代码调用这个ConfigurationManager类的静态方法的时候，就会加载配置文件中的数据
	 * 
	 * 而且，放在静态代码块中，还有一个好处，就是类的初始化在整个JVM生命周期内，有且仅有一次，也就是说
	 * 配置文件只会加载一次，然后以后就是重复使用，效率比较高；不用反复加载多次
	 */
	static {
		try {
			// 通过一个“类名.class”的方式，就可以获取到这个类在JVM中对应的Class对象
			// 然后再通过这个Class对象的getClassLoader()方法，就可以获取到当初加载这个类的JVM
			// 中的类加载器（ClassLoader），然后调用ClassLoader的getResourceAsStream()这个方法
			// 就可以用类加载器，去加载类加载路径中的指定的文件
			// 最终可以获取到一个，针对指定文件的输入流（InputStream）
			InputStream in = ConfigurationManager.class
					.getClassLoader().getResourceAsStream("my.properties"); 
			
			// 调用Properties的load()方法，给它传入一个文件的InputStream输入流
			// 即可将文件中的符合“key=value”格式的配置项，都加载到Properties对象中
			// 加载过后，此时，Properties对象中就有了配置文件中所有的key-value对了
			// 然后外界其实就可以通过Properties对象获取指定key对应的value
			prop.load(in);  
		} catch (Exception e) {
			e.printStackTrace();  
		}
	}
	
	/**
	 * 获取指定key对应的value
	 * 
	 * 第一次外界代码，调用ConfigurationManager类的getProperty静态方法时，JVM内部会发现
	 * ConfigurationManager类还不在JVM的内存中
	 * 
	 * 此时JVM，就会使用自己的ClassLoader（类加载器），去对应的类所在的磁盘文件（.class文件）中
	 * 去加载ConfigurationManager类，到JVM内存中来，并根据类内部的信息，去创建一个Class对象
	 * Class对象中，就包含了类的元信息，包括类有哪些field（Properties prop）；有哪些方法（getProperty）
	 * 
	 * 加载ConfigurationManager类的时候，还会初始化这个类，那么此时就执行类的static静态代码块
	 * 此时咱们自己编写的静态代码块中的代码，就会加载my.properites文件的内容，到Properties对象中来
	 * 
	 * 下一次外界代码，再调用ConfigurationManager的getProperty()方法时，就不会再次加载类，不会再次初始化
	 * 类，和执行静态代码块了，所以也印证了，我们上面所说的，类只会加载一次，配置文件也仅仅会加载一次
	 * 
	 * @param key 
	 * @return value
	 */
	public static String getProperty(String key) {
		return prop.getProperty(key);
	}
	
	/**
	 * 获取整数类型的配置项
	 * @param key
	 * @return value
	 */
	public static Integer getInteger(String key) {
		String value = getProperty(key);
		try {
			return Integer.valueOf(value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * 获取布尔类型的配置项
	 * @param key
	 * @return value
	 */
	public static Boolean getBoolean(String key) {
		String value = getProperty(key);
		try {
			return Boolean.valueOf(value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 获取Long类型的配置项
	 * @param key
	 * @return
	 */
	public static Long getLong(String key) {
		String value = getProperty(key);
		try {
			return Long.valueOf(value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0L;
	}
	
}
