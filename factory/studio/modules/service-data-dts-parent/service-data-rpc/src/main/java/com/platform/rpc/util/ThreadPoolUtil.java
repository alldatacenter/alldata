package com.platform.rpc.util;

import java.util.concurrent.*;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 自定义线程池
 **/
public class ThreadPoolUtil {

	public static ThreadPoolExecutor makeServerThreadPool(final String serverType, int corePoolSize, int maxPoolSize) {
		ThreadPoolExecutor serverHandlerPool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(1000),
				r -> new Thread(r, "xxl-rpc, " + serverType + "-serverHandlerPool-" + r.hashCode()), (r, executor) -> {
			throw new XxlRpcException("xxl-rpc " + serverType + " Thread pool is EXHAUSTED!");
		});        // default maxThreads 300, minThreads 60

		return serverHandlerPool;
	}

}
