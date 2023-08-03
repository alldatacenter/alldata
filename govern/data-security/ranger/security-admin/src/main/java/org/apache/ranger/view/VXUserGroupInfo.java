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

 package org.apache.ranger.view;

/**
 * UserGroupInfo
 *
 */

import java.util.List;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXUserGroupInfo extends VXDataObject implements java.io.Serializable  {
	
	private static final long serialVersionUID = 1L;
	
	VXUser xuserInfo;
	List<VXGroup> xgroupInfo;
	
	public VXUserGroupInfo ( ) {
	}

	public VXUser getXuserInfo() {
		return xuserInfo;
	}

	public void setXuserInfo(VXUser xuserInfo) {
		this.xuserInfo = xuserInfo;
	}

	public List<VXGroup> getXgroupInfo() {
		return xgroupInfo;
	}

	public void setXgroupInfo(List<VXGroup> xgroupInfo) {
		this.xgroupInfo = xgroupInfo;
	}

}