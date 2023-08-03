/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 /**
 *
 */
package org.apache.ranger.common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.ranger.common.view.ViewBaseBean;
import org.apache.ranger.plugin.util.JsonUtilsV2;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JSONUtil {

	@Autowired
	RESTErrorUtil restErrorUtil;

	public File writeJsonToFile(ViewBaseBean viewBean, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {

		if (fileName.length() < 3) {
			fileName = "file_" + fileName;
		}

		File file = File.createTempFile(fileName, ".json");
		JsonUtilsV2.getMapper().defaultPrettyPrintingWriter().writeValue(file, viewBean);

		return file;
	}

	public Map<String, String> jsonToMap(String jsonStr) {

		try {
			return JsonUtilsV2.jsonToMap(jsonStr);

		} catch (Exception e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}

	}

	public String readMapToString(Map<?, ?> map) {

		try {
			return JsonUtilsV2.mapToJson(map);
		} catch (Exception e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}
	}
	
	public String readListToString(List<?> list) {

		try {
			return JsonUtilsV2.listToJson(list);
		} catch (Exception e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}
	}

	public String writeObjectAsString(Serializable obj) {

		try {
			return JsonUtilsV2.objToJson(obj);
		} catch (Exception e) {
			throw restErrorUtil.createRESTException(
					"Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}
	}

	public <T> T writeJsonToJavaObject(String json, Class<T> tClass) {

		try {
			return JsonUtilsV2.jsonToObj(json, tClass);
		} catch (Exception e) {
			throw restErrorUtil.createRESTException("Invalid input data: " + e.getMessage(),
					MessageEnums.INVALID_INPUT_DATA);
		}
	}

}