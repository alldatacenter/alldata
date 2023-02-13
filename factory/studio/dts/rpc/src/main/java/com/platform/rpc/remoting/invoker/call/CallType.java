package com.platform.rpc.remoting.invoker.call;

/**
 *
 * @author AllDataDC
 * @Date: 2022/9/16 11:14
 * @Description: 远程调用的类型
 **/
public enum CallType {


	SYNC,

	FUTURE,

	CALLBACK,

	ONEWAY;


	public static CallType match(String name, CallType defaultCallType) {
		for (CallType item : CallType.values()) {
			if (item.name().equals(name)) {
				return item;
			}
		}
		return defaultCallType;
	}

}
