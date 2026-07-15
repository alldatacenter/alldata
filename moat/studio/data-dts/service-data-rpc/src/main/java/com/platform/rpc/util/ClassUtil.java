package com.platform.rpc.util;

import java.util.HashMap;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * Class的工具类
 **/
public class ClassUtil {

	private static final HashMap<String, Class<?>> primClasses = new HashMap<>();

	static {
		primClasses.put("boolean", boolean.class);
		primClasses.put("byte", byte.class);
		primClasses.put("char", char.class);
		primClasses.put("short", short.class);
		primClasses.put("int", int.class);
		primClasses.put("long", long.class);
		primClasses.put("float", float.class);
		primClasses.put("double", double.class);
		primClasses.put("void", void.class);
	}

	/**
	 *
	 * @param className 类名
	 * @return 反射得到类的Class
	 * @throws ClassNotFoundException
	 */
	public static Class<?> resolveClass(String className) throws ClassNotFoundException {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException ex) {
			Class<?> cl = primClasses.get(className);
			if (cl != null) {
				return cl;
			} else {
				throw ex;
			}
		}
	}

}
