package com.platform.rpc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * IP的工具类
 **/
public class IpUtil {
	private static final Logger logger = LoggerFactory.getLogger(IpUtil.class);

	private static final String ANYHOST_VALUE = "0.0.0.0";
	private static final String LOCALHOST_VALUE = "127.0.0.1";
	private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");


	private static volatile InetAddress LOCAL_ADDRESS = null;


	private static InetAddress toValidAddress(InetAddress address) {
		if (address instanceof Inet6Address) {
			Inet6Address v6Address = (Inet6Address) address;
			if (isPreferIPV6Address()) {
				return normalizeV6Address(v6Address);
			}
		}
		if (isValidV4Address(address)) {
			return address;
		}
		return null;
	}

	/**
	 *
	 * @return
	 */
	private static boolean isPreferIPV6Address() {
		return Boolean.getBoolean("java.net.preferIPv6Addresses");
	}

	/**
	 *
	 * @param address
	 * @return 判断是不是有效的IPV4的地址
	 */
	private static boolean isValidV4Address(InetAddress address) {
		if (address == null || address.isLoopbackAddress()) {
			return false;
		}
		String name = address.getHostAddress();
		return (name != null && IP_PATTERN.matcher(name).matches() && !ANYHOST_VALUE.equals(name) && !LOCALHOST_VALUE
				.equals(name));
	}

	/**
	 *
	 * @param address
	 * @return 返回IPV6的地址
	 */
	private static InetAddress normalizeV6Address(Inet6Address address) {
		String addr = address.getHostAddress();
		int i = addr.lastIndexOf('%');
		if (i > 0) {
			try {
				return InetAddress.getByName(addr.substring(0, i) + '%' + address.getScopeId());
			} catch (UnknownHostException e) {
				logger.debug("Unknown IPV6 address: ", e);
			}
		}
		return address;
	}

	/**
	 *
	 * @return 返回本地的IP
	 */
	private static InetAddress getLocalAddress0() {
		InetAddress localAddress = null;
		try {
			localAddress = InetAddress.getLocalHost();
			InetAddress addressItem = toValidAddress(localAddress);
			if (addressItem != null) {
				return addressItem;
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}

		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (null == interfaces) {
				return localAddress;
			}
			while (interfaces.hasMoreElements()) {
				try {
					NetworkInterface network = interfaces.nextElement();
					if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
						continue;
					}
					Enumeration<InetAddress> addresses = network.getInetAddresses();
					while (addresses.hasMoreElements()) {
						try {
							InetAddress addressItem = toValidAddress(addresses.nextElement());
							if (addressItem != null) {
								try {
									if (addressItem.isReachable(100)) {
										return addressItem;
									}
								} catch (IOException e) {
									// ignore
								}
							}
						} catch (Throwable e) {
							logger.error(e.getMessage(), e);
						}
					}
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
		return localAddress;
	}


	/**
	 *
	 * @return 返回本地的IP
	 */
	public static InetAddress getLocalAddress() {
		if (LOCAL_ADDRESS != null) {
			return LOCAL_ADDRESS;
		}
		InetAddress localAddress = getLocalAddress0();
		LOCAL_ADDRESS = localAddress;
		return localAddress;
	}

	/**
	 *
	 * @return 返回本地的IP
	 */
	public static String getIp() {
		return getLocalAddress().getHostAddress();
	}

	/**
	 *
	 * @param port
	 * @return 返回本地IP:port
	 */
	public static String getIpPort(int port) {
		String ip = getIp();
		return getIpPort(ip, port);
	}

	/**
	 *
	 * @param ip
	 * @param port
	 * @return ip:port
	 */
	public static String getIpPort(String ip, int port) {
		if (ip == null) {
			return null;
		}
		return ip.concat(":").concat(String.valueOf(port));
	}

	/**
	 * @param address
	 * @return ip和port的数组
	 */
	public static Object[] parseIpPort(String address) {
		String[] array = address.split(":");

		String host = array[0];
		int port = Integer.parseInt(array[1]);

		return new Object[]{host, port};
	}
}
