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

 package org.apache.ranger.unixusersync.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import org.apache.ranger.ugsyncutil.model.XUserInfo;

public class GetXUserListResponse {

	private int totalCount;

	@SerializedName("vXUsers")
	List<XUserInfo> xuserInfoList;
	
	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public List<XUserInfo> getXuserInfoList() {
		return xuserInfoList;
	}

	public void setXuserInfoList(List<XUserInfo> xuserInfoList) {
		this.xuserInfoList = xuserInfoList;
	}

	
	
}
