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

 package org.apache.ranger.plugin.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class HadoopException extends RuntimeException {
	private static final long serialVersionUID = 8872734935128535649L;
	
	public HashMap<String, Object> responseData;

	public HadoopException() {
		super();
	}

	public HadoopException(String message, Throwable cause) {
		super(message, cause);
	}

	public HadoopException(String message) {
		super(message);
	}

	public HadoopException(Throwable cause) {
		super(cause);
	}

	public void generateResponseDataMap(boolean connectivityStatus,
			String message, String description, Long objectId, String fieldName) {
		responseData = new HashMap<>();
		responseData.put("connectivityStatus", connectivityStatus);
		responseData.put("message", message);
		responseData.put("description", description);
		responseData.put("objectId", objectId);
		responseData.put("fieldName", fieldName);
	}

	public String getMessage(Throwable excp) {
		List<String> errList = new ArrayList<>();
		while (excp != null) {
			if (!errList.contains(excp.getMessage() + ". \n") && !errList.contains(excp.toString() + ". \n")) {
				if (excp.getMessage() != null && !(excp.getMessage().equalsIgnoreCase(""))) {
					errList.add(excp.getMessage() + ". \n");
				}
			}
			excp = excp.getCause();
		}
		return StringUtils.join(errList, "");
	}

	public HashMap<String,Object> getResponseData() {
		return responseData;
	}

	public void setReponseData(HashMap<String,Object> responseData) {
		this.responseData = responseData;
	}

}
