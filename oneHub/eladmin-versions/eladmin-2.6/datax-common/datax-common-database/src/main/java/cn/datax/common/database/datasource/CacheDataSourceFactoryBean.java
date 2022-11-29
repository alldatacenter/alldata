package cn.datax.common.database.datasource;

import cn.datax.common.database.constants.DbQueryProperty;

import javax.sql.DataSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheDataSourceFactoryBean extends AbstractDataSourceFactory {

	/**
	 * 数据源缓存
	 */
	private static Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

	@Override
	public DataSource createDataSource(DbQueryProperty property) {
		String key = property.getDbType() + ":" + property.getHost()
				+ ":" + property.getPort() + ":" + property.getUsername()
				+ ":" + property.getPassword() + ":" + property.getDbName()
				+ ":" + property.getSid();
		String s = compress(key);
		DataSource dataSource = dataSourceMap.get(s);
		if (null == dataSource) {
			synchronized (CacheDataSourceFactoryBean.class) {
				dataSource = super.createDataSource(property);
				dataSourceMap.put(s, dataSource);
			}
		}
		return dataSource;
	}

	// 压缩
	public static String compress(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(str.getBytes());
		byte b[] = md.digest();
		int i;
		StringBuffer buf = new StringBuffer();
		for (int offset = 0; offset < b.length; offset++) {
			i = b[offset];
			if (i < 0)
				i += 256;
			if (i < 16)
				buf.append("0");
			buf.append(Integer.toHexString(i));
		}
//        System.out.println("MD5(" + str + ",32小写) = " + buf.toString());
//        System.out.println("MD5(" + str + ",32大写) = " + buf.toString().toUpperCase());
//        System.out.println("MD5(" + str + ",16小写) = " + buf.toString().substring(8, 24));
//        System.out.println("MD5(" + str + ",16大写) = " + buf.toString().substring(8, 24).toUpperCase());
		return buf.toString().substring(8, 24).toUpperCase();
	}
}
