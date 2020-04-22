package com.platform.spark.spark.product;

import org.apache.spark.sql.api.java.UDF1;

/**
 * 去除随机前缀
 * @author wulinhao
 *
 */
public class RemoveRandomPrefixUDF implements UDF1<String, String> {

	private static final Long serialVersionUID = 1L;

	@Override
	public String call(String val) throws Exception {
		String[] valSplited = val.split("_");
		return valSplited[1];
	}

}
