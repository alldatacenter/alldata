package com.platform.rpc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 端口的工具类
 **/
public class NetUtil {
	private static Logger logger = LoggerFactory.getLogger(NetUtil.class);

	/**
	 *
	 * @param defaultPort
	 * @return 可用的端口
	 */
	public static int findAvailablePort(int defaultPort) {
		int portTmp = defaultPort;
		while (portTmp < 65535) {
			if (!isPortUsed(portTmp)) {
				return portTmp;
			} else {
				portTmp++;
			}
		}
		portTmp = defaultPort--;
		while (portTmp > 0) {
			if (!isPortUsed(portTmp)) {
				return portTmp;
			} else {
				portTmp--;
			}
		}
		throw new XxlRpcException("no available port.");
	}

	/**
	 *
	 * @param port
	 * @return 可用返回true, 否则返回false
	 */
	public static boolean isPortUsed(int port) {
		boolean used;
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			used = false;
		} catch (IOException e) {
			logger.info(">>>>>>>>>>> xxl-rpc, port[{}] is in use.", port);
			used = true;
		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					logger.info("");
				}
			}
		}
		return used;
	}

}
