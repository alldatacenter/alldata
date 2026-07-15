package com.platform.rpc.remoting.invoker.call;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 远程调用的类型
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
