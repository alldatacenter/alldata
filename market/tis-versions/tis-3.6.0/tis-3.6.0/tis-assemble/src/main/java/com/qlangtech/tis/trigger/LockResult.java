/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */


package com.qlangtech.tis.trigger;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.data.Stat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 百岁（baisui@2dfire.com）
 *
 * @date 2016年4月26日
 */
public class LockResult {
	private String zkAddress;
	private String path;
	private String content;

	// 节点描述信息
	private String desc;

	public final List<String> childValus = new ArrayList<String>();

	private final boolean editable;

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public LockResult(boolean editable) {
		super();
		this.editable = editable;
	}

	public void addChildValue(String value) {
		this.childValus.add(value);
	}

	public boolean isEditable() {
		return this.editable;
	}

	private static final ThreadLocal<SimpleDateFormat> format = new ThreadLocal<SimpleDateFormat>() {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy/MM/dd HH:mm");
		}

	};

//	public String getCreateTime() {
////		if (stat == null) {
////			return StringUtils.EMPTY;
////		}
////		return format.get().format(new Date(stat.getCtime()));
//		// return ManageUtils.formatDateYYYYMMdd();
//	}

//	public String getUpdateTime() {
//		if (stat == null) {
//			return StringUtils.EMPTY;
//		}
//		return format.get().format(new Date(stat.getMtime()));
//		// return ManageUtils.formatDateYYYYMMdd(new Date(stat.getMtime()));
//	}

	public String getZkAddress() {
		return zkAddress;
	}

	public void setZkAddress(String zkAddress) {
		this.zkAddress = zkAddress;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
