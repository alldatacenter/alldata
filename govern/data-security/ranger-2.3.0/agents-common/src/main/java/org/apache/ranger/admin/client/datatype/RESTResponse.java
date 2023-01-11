/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.admin.client.datatype;

import java.util.List;

import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.plugin.util.JsonUtilsV2;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse;


@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RESTResponse implements java.io.Serializable {
	private static final Logger LOG = LoggerFactory.getLogger(RESTResponse.class);

	/**
	 * values for statusCode
	 */
	public static final int STATUS_SUCCESS         = 0;
	public static final int STATUS_ERROR           = 1;
	public static final int STATUS_VALIDATION      = 2;
	public static final int STATUS_WARN            = 3;
	public static final int STATUS_INFO            = 4;
	public static final int STATUS_PARTIAL_SUCCESS = 5;
	public static final int ResponseStatus_MAX     = 5;

	private int           httpStatusCode;
	private int           statusCode;
	private String        msgDesc;
	private List<Message> messageList;


	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(int httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getMsgDesc() {
		return msgDesc;
	}

	public void setMsgDesc(String msgDesc) {
		this.msgDesc = msgDesc;
	}

	public List<Message> getMessageList() {
		return messageList;
	}

	public void setMessageList(List<Message> messageList) {
		this.messageList = messageList;
	}
	
	public String getMessage() {
		return StringUtil.isEmpty(msgDesc) ? ("HTTP " + httpStatusCode) : msgDesc;
	}

	public static RESTResponse fromClientResponse(ClientResponse response) {
		RESTResponse ret = null;

		String jsonString = response == null ? null : response.getEntity(String.class);
		int    httpStatus = response == null ? 0 : response.getStatus();

		if(! StringUtil.isEmpty(jsonString)) {
			ret = RESTResponse.fromJson(jsonString);
		}

		if(ret == null) {
			ret = new RESTResponse();
		}

		ret.setHttpStatusCode(httpStatus);

		return ret;
	}

	public String toJson() {
		try {
			return JsonUtilsV2.objToJson(this);
		} catch (Exception e) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("toJson() failed", e);
			}
		}

		return "";
	}

	public static RESTResponse fromJson(String jsonString) {
		try {
			return JsonUtilsV2.jsonToObj(jsonString, RESTResponse.class);
		} catch (Exception e) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("fromJson('" + jsonString + "') failed", e);
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return toJson();
	}

	@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Message implements java.io.Serializable {
		private String name;
		private String rbKey;
		private String message;
		private Long   objectId;
		private String fieldName;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getRbKey() {
			return rbKey;
		}
		public void setRbKey(String rbKey) {
			this.rbKey = rbKey;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public Long getObjectId() {
			return objectId;
		}
		public void setObjectId(Long objectId) {
			this.objectId = objectId;
		}
		public String getFieldName() {
			return fieldName;
		}
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public String toJson() {
			try {
				return JsonUtilsV2.objToJson(this);
			} catch (Exception e) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("toJson() failed", e);
				}
			}
			
			return "";
		}

		public static RESTResponse fromJson(String jsonString) {
			try {
				return JsonUtilsV2.jsonToObj(jsonString, RESTResponse.class);
			} catch (Exception e) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("fromJson('" + jsonString + "') failed", e);
				}
			}
			
			return null;
		}

		@Override
		public String toString() {
			return toJson();
		}
	}
}
