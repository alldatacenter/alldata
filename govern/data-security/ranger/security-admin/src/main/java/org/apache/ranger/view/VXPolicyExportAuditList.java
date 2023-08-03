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
 * List wrapper class for VXPolicyExportAudit
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.view.VList;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class VXPolicyExportAuditList extends VList {
	private static final long serialVersionUID = 1L;
    List<VXPolicyExportAudit> vXPolicyExportAudits = new ArrayList<VXPolicyExportAudit>();

    public VXPolicyExportAuditList() {
	super();
    }

    public VXPolicyExportAuditList(List<VXPolicyExportAudit> objList) {
	super(objList);
	this.vXPolicyExportAudits = objList;
    }

    /**
     * @return the vXPolicyExportAudits
     */
    public List<VXPolicyExportAudit> getVXPolicyExportAudits() {
	return vXPolicyExportAudits;
    }

    /**
     * @param vXPolicyExportAudits
     *            the vXPolicyExportAudits to set
     */
    public void setVXPolicyExportAudits(List<VXPolicyExportAudit> vXPolicyExportAudits) {
	this.vXPolicyExportAudits = vXPolicyExportAudits;
    }

    @Override
    public int getListSize() {
	if (vXPolicyExportAudits != null) {
	    return vXPolicyExportAudits.size();
	}
	return 0;
    }

    @Override
    public List<VXPolicyExportAudit> getList() {
	return vXPolicyExportAudits;
    }

}
