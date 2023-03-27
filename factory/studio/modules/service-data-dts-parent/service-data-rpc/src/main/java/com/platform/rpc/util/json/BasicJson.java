package com.platform.rpc.util.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * Json的父类
 **/
public class BasicJson {

	private static final BasicJsonReader basicJsonReader = new BasicJsonReader();
	private static final BasicJsonwriter basicJsonwriter = new BasicJsonwriter();

	/**
	 * object to json
	 *
	 * @param object
	 * @return
	 */
	public static String toJson(Object object) {
		return basicJsonwriter.toJson(object);
	}

	/**
	 * parse json to map
	 *
	 * @param json
	 * @return only for filed type "null、ArrayList、LinkedHashMap、String、Long、Double、..."
	 */
	public static Map<String, Object> parseMap(String json) {
		return basicJsonReader.parseMap(json);
	}

	/**
	 * json to List
	 *
	 * @param json
	 * @return
	 */
	public static List<Object> parseList(String json) {
		return basicJsonReader.parseList(json);
	}


	public static void main(String[] args) {
		Map<String, Object> result = new HashMap<>();
		result.put("code", 200);
		result.put("msg", "success");
		result.put("arr", Arrays.asList("111", "222"));
		result.put("float", 1.11f);
		result.put("temp", null);

		String json = toJson(result);
		System.out.println(json);

		Map<String, Object> mapObj = parseMap(json);
		System.out.println(mapObj);

		List<Object> listInt = parseList("[111,222,33]");
		System.out.println(listInt);

	}
}
