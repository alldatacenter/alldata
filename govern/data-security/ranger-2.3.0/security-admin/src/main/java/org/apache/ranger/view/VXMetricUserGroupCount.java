/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.view;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class VXMetricUserGroupCount implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	protected Long groupCount;
	protected Long userCountOfUserRole;
	protected Long userCountOfKeyAdminRole;
	protected Long userCountOfSysAdminRole;
    protected Long userCountOfKeyadminAuditorRole;
    protected Long userCountOfSysAdminAuditorRole;
	protected Long userTotalCount;
	/**
	 * Default constructor. This will set all the attributes to default value.
	*/
	public VXMetricUserGroupCount() {
	}
    public Long getUserCountOfKeyadminAuditorRole() {
	return userCountOfKeyadminAuditorRole;
    }
    public void setUserCountOfKeyadminAuditorRole(Long userCountOfKeyadminAuditorRole) {
	this.userCountOfKeyadminAuditorRole = userCountOfKeyadminAuditorRole;
    }
    public Long getUserCountOfSysAdminAuditorRole() {
	return userCountOfSysAdminAuditorRole;
    }
    public void setUserCountOfSysAdminAuditorRole(Long userCountOfSysAdminAuditorRole) {
	this.userCountOfSysAdminAuditorRole = userCountOfSysAdminAuditorRole;
    }
	/**
	 * @return the groupCount
	 */
	public Long getGroupCount() {
		return groupCount;
	}
	/**
	 * @param groupCount the groupCount to set
	 */
	public void setGroupCount(Long groupCount) {
		this.groupCount = groupCount;
	}

	/**
	 * @return the userCountOfUserRole
	 */
	public Long getUserCountOfUserRole() {
		return userCountOfUserRole;
	}
	/**
	 * @param userCountOfUserRole the userCountOfUserRole to set
	 */
	public void setUserCountOfUserRole(Long userCountOfUserRole) {
		this.userCountOfUserRole = userCountOfUserRole;
	}
	/**
	 * @return the userCountOfKeyAdminRole
	 */
	public Long getUserCountOfKeyAdminRole() {
		return userCountOfKeyAdminRole;
	}
	/**
	 * @param userCountOfKeyAdminRole the userKeyAdminRoleCount to set
	 */
	public void setUserCountOfKeyAdminRole(Long userCountOfKeyAdminRole) {
		this.userCountOfKeyAdminRole = userCountOfKeyAdminRole;
	}
	/**
	 * @return the userCountOfSysAdminRole
	 */
	public Long getUserCountOfSysAdminRole() {
		return userCountOfSysAdminRole;
	}
	/**
	 * @param userCountOfSysAdminRole the userCountOfSysAdminRole to set
	 */
	public void setUserCountOfSysAdminRole(Long userCountOfSysAdminRole) {
		this.userCountOfSysAdminRole = userCountOfSysAdminRole;
	}
    /**
     * @return the userTotalCount
     */
    public Long getUserTotalCount() {
            return userTotalCount;
    }
    /**
     * @param userTotalCount the userTotalCount to set
    */
    public void setUserTotalCount(Long userTotalCount) {
            this.userTotalCount = userTotalCount;
    }
	@Override
	public String toString() {
		return "VXMetricUserGroupCount [groupCount=" + groupCount
                                + ", userCountOfUserRole=" + userCountOfUserRole
                                + ", userCountOfKeyAdminRole=" + userCountOfKeyAdminRole
                                + ", userCountOfSysAdminRole=" + userCountOfSysAdminRole
                                + ", userCountOfKeyadminAuditorRole=" + userCountOfKeyadminAuditorRole
                                + ", userCountOfSysAdminAuditorRole=" + userCountOfSysAdminAuditorRole
                                + ", userTotalCount=" + userTotalCount+ "]";
	}
}