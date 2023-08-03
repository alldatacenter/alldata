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
 * Resource
 *
 */

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.RangerConstants;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXResource extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Name
	 */
	protected String name;
	protected String policyName;
	/**
	 * Description
	 */
	protected String description;
	/**
	 * Status
	 * This attribute is of type enum CommonEnums::ResourceType
	 */
	protected int resourceType;
	/**
	 * Id of the asset
	 */
	protected Long assetId;
	/**
	 * Id of the parent
	 */
	protected Long parentId;
	/**
	 * Path for the parent
	 */
	protected String parentPath;
	/**
	 * Whether to encrypt this resource
	 * This attribute is of type enum CommonEnums::BooleanValue
	 */
	protected int isEncrypt = RangerConstants.BOOL_FALSE;
	/**
	 * List of permissions maps
	 */
	protected List<VXPermMap> permMapList;
	/**
	 * List of audits
	 */
	protected List<VXAuditMap> auditList;
	/**
	 * Is recursive
	 * This attribute is of type enum CommonEnums::BooleanValue
	 */
	protected int isRecursive = RangerConstants.BOOL_NONE;
	/**
	 * Group to which this resource belongs to
	 */
	protected String resourceGroup;
	/**
	 * Databases
	 */
	protected String databases;
	/**
	 * Tables
	 */
	protected String tables;
	/**
	 * Column families
	 */
	protected String columnFamilies;
	/**
	 * Columns
	 */
	protected String columns;
	/**
	 * UDFs
	 */
	protected String udfs;
	/**
	 * Asset Name
	 */
	protected String assetName;
	/**
	 * Asset Type
	 */
	protected int assetType;
	/**
	 * Resource Status
	 */
	protected int resourceStatus;
	/**
	 * Table Type
	 */
	protected int tableType;
	/**
	 * Resource Status
	 */
	protected int columnType;
	/**
	 * Check parent permission
	 * This attribute is of type enum CommonEnums::BooleanValue
	 */
	protected int checkParentPermission = RangerConstants.BOOL_NONE;
	/**
	 * Topologoies
	 */
	protected String topologies;
	/**
	 * Services
	 */
	protected String services;

	/**
	 * Hive Services
	 */
	protected String hiveServices;

	/**
	 * guid
	 */
	protected String guid;
	
	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXResource ( ) {
		resourceType = AppConstants.RESOURCE_PATH;
		isEncrypt = RangerConstants.BOOL_FALSE;
		isRecursive = RangerConstants.BOOL_NONE;
		checkParentPermission = RangerConstants.BOOL_NONE;
	}

	/**
	 * This method sets the value to the member attribute <b>name</b>.
	 * You cannot set null to the attribute.
	 * @param name Value to set member attribute <b>name</b>
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * Returns the value for the member attribute <b>name</b>
	 * @return String - value of member attribute <b>name</b>.
	 */
	public String getName( ) {
		return this.name;
	}

	public String getPolicyName() {
		return policyName;
	}

	public void setPolicyName(String policyName) {
		this.policyName = policyName;
	}

	/**
	 * This method sets the value to the member attribute <b>description</b>.
	 * You cannot set null to the attribute.
	 * @param description Value to set member attribute <b>description</b>
	 */
	public void setDescription( String description ) {
		this.description = description;
	}

	/**
	 * Returns the value for the member attribute <b>description</b>
	 * @return String - value of member attribute <b>description</b>.
	 */
	public String getDescription( ) {
		return this.description;
	}

	/**
	 * This method sets the value to the member attribute <b>resourceType</b>.
	 * You cannot set null to the attribute.
	 * @param resourceType Value to set member attribute <b>resourceType</b>
	 */
	public void setResourceType( int resourceType ) {
		this.resourceType = resourceType;
	}

	/**
	 * Returns the value for the member attribute <b>resourceType</b>
	 * @return int - value of member attribute <b>resourceType</b>.
	 */
	public int getResourceType( ) {
		return this.resourceType;
	}

	/**
	 * This method sets the value to the member attribute <b>assetId</b>.
	 * You cannot set null to the attribute.
	 * @param assetId Value to set member attribute <b>assetId</b>
	 */
	public void setAssetId( Long assetId ) {
		this.assetId = assetId;
	}

	/**
	 * Returns the value for the member attribute <b>assetId</b>
	 * @return Long - value of member attribute <b>assetId</b>.
	 */
	public Long getAssetId( ) {
		return this.assetId;
	}

	/**
	 * This method sets the value to the member attribute <b>parentId</b>.
	 * You cannot set null to the attribute.
	 * @param parentId Value to set member attribute <b>parentId</b>
	 */
	public void setParentId( Long parentId ) {
		this.parentId = parentId;
	}

	/**
	 * Returns the value for the member attribute <b>parentId</b>
	 * @return Long - value of member attribute <b>parentId</b>.
	 */
	public Long getParentId( ) {
		return this.parentId;
	}

	/**
	 * This method sets the value to the member attribute <b>parentPath</b>.
	 * You cannot set null to the attribute.
	 * @param parentPath Value to set member attribute <b>parentPath</b>
	 */
	public void setParentPath( String parentPath ) {
		this.parentPath = parentPath;
	}

	/**
	 * Returns the value for the member attribute <b>parentPath</b>
	 * @return String - value of member attribute <b>parentPath</b>.
	 */
	public String getParentPath( ) {
		return this.parentPath;
	}

	/**
	 * This method sets the value to the member attribute <b>isEncrypt</b>.
	 * You cannot set null to the attribute.
	 * @param isEncrypt Value to set member attribute <b>isEncrypt</b>
	 */
	public void setIsEncrypt( int isEncrypt ) {
		this.isEncrypt = isEncrypt;
	}

	/**
	 * Returns the value for the member attribute <b>isEncrypt</b>
	 * @return int - value of member attribute <b>isEncrypt</b>.
	 */
	public int getIsEncrypt( ) {
		return this.isEncrypt;
	}

	/**
	 * This method sets the value to the member attribute <b>permMapList</b>.
	 * You cannot set null to the attribute.
	 * @param permMapList Value to set member attribute <b>permMapList</b>
	 */
	public void setPermMapList( List<VXPermMap> permMapList ) {
		this.permMapList = permMapList;
	}

	/**
	 * Returns the value for the member attribute <b>permMapList</b>
	 * @return List<VXPermMap> - value of member attribute <b>permMapList</b>.
	 */
	public List<VXPermMap> getPermMapList( ) {
		return this.permMapList;
	}

	/**
	 * This method sets the value to the member attribute <b>auditList</b>.
	 * You cannot set null to the attribute.
	 * @param auditList Value to set member attribute <b>auditList</b>
	 */
	public void setAuditList( List<VXAuditMap> auditList ) {
		this.auditList = auditList;
	}

	/**
	 * Returns the value for the member attribute <b>auditList</b>
	 * @return List<VXAuditMap> - value of member attribute <b>auditList</b>.
	 */
	public List<VXAuditMap> getAuditList( ) {
		return this.auditList;
	}

	/**
	 * This method sets the value to the member attribute <b>isRecursive</b>.
	 * You cannot set null to the attribute.
	 * @param isRecursive Value to set member attribute <b>isRecursive</b>
	 */
	public void setIsRecursive( int isRecursive ) {
		this.isRecursive = isRecursive;
	}

	/**
	 * Returns the value for the member attribute <b>isRecursive</b>
	 * @return int - value of member attribute <b>isRecursive</b>.
	 */
	public int getIsRecursive( ) {
		return this.isRecursive;
	}

	/**
	 * This method sets the value to the member attribute <b>resourceGroup</b>.
	 * You cannot set null to the attribute.
	 * @param resourceGroup Value to set member attribute <b>resourceGroup</b>
	 */
	public void setResourceGroup( String resourceGroup ) {
		this.resourceGroup = resourceGroup;
	}

	/**
	 * Returns the value for the member attribute <b>resourceGroup</b>
	 * @return String - value of member attribute <b>resourceGroup</b>.
	 */
	public String getResourceGroup( ) {
		return this.resourceGroup;
	}

	/**
	 * This method sets the value to the member attribute <b>databases</b>.
	 * You cannot set null to the attribute.
	 * @param databases Value to set member attribute <b>databases</b>
	 */
	public void setDatabases( String databases ) {
		this.databases = databases;
	}

	/**
	 * Returns the value for the member attribute <b>databases</b>
	 * @return String - value of member attribute <b>databases</b>.
	 */
	public String getDatabases( ) {
		return this.databases;
	}

	/**
	 * This method sets the value to the member attribute <b>tables</b>.
	 * You cannot set null to the attribute.
	 * @param tables Value to set member attribute <b>tables</b>
	 */
	public void setTables( String tables ) {
		this.tables = tables;
	}

	/**
	 * Returns the value for the member attribute <b>tables</b>
	 * @return String - value of member attribute <b>tables</b>.
	 */
	public String getTables( ) {
		return this.tables;
	}

	/**
	 * This method sets the value to the member attribute <b>columnFamilies</b>.
	 * You cannot set null to the attribute.
	 * @param columnFamilies Value to set member attribute <b>columnFamilies</b>
	 */
	public void setColumnFamilies( String columnFamilies ) {
		this.columnFamilies = columnFamilies;
	}

	/**
	 * Returns the value for the member attribute <b>columnFamilies</b>
	 * @return String - value of member attribute <b>columnFamilies</b>.
	 */
	public String getColumnFamilies( ) {
		return this.columnFamilies;
	}

	/**
	 * This method sets the value to the member attribute <b>columns</b>.
	 * You cannot set null to the attribute.
	 * @param columns Value to set member attribute <b>columns</b>
	 */
	public void setColumns( String columns ) {
		this.columns = columns;
	}

	/**
	 * Returns the value for the member attribute <b>columns</b>
	 * @return String - value of member attribute <b>columns</b>.
	 */
	public String getColumns( ) {
		return this.columns;
	}

	/**
	 * This method sets the value to the member attribute <b>udfs</b>.
	 * You cannot set null to the attribute.
	 * @param udfs Value to set member attribute <b>udfs</b>
	 */
	public void setUdfs( String udfs ) {
		this.udfs = udfs;
	}

	/**
	 * Returns the value for the member attribute <b>udfs</b>
	 * @return String - value of member attribute <b>udfs</b>.
	 */
	public String getUdfs( ) {
		return this.udfs;
	}

	/**
	 * This method sets the value to the member attribute <b>assetName</b>.
	 * You cannot set null to the attribute.
	 * @param assetName Value to set member attribute <b>assetName</b>
	 */
	public void setAssetName( String assetName ) {
		this.assetName = assetName;
	}

	/**
	 * Returns the value for the member attribute <b>assetName</b>
	 * @return String - value of member attribute <b>assetName</b>.
	 */
	public String getAssetName( ) {
		return this.assetName;
	}

	/**
	 * This method sets the value to the member attribute <b>assetType</b>.
	 * You cannot set null to the attribute.
	 * @param assetType Value to set member attribute <b>assetType</b>
	 */
	public void setAssetType( int assetType ) {
		this.assetType = assetType;
	}

	/**
	 * Returns the value for the member attribute <b>assetType</b>
	 * @return int - value of member attribute <b>assetType</b>.
	 */
	public int getAssetType( ) {
		return this.assetType;
	}

	/**
	 * This method sets the value to the member attribute <b>resourceStatus</b>.
	 * You cannot set null to the attribute.
	 * @param resourceStatus Value to set member attribute <b>resourceStatus</b>
	 */
	public void setResourceStatus( int resourceStatus ) {
		this.resourceStatus = resourceStatus;
	}

	/**
	 * Returns the value for the member attribute <b>resourceStatus</b>
	 * @return int - value of member attribute <b>resourceStatus</b>.
	 */
	public int getResourceStatus( ) {
		return this.resourceStatus;
	}

	/**
	 * This method sets the value to the member attribute <b>tableType</b>.
	 * You cannot set null to the attribute.
	 * @param tableType Value to set member attribute <b>tableType</b>
	 */
	public void setTableType( int tableType ) {
		this.tableType = tableType;
	}

	/**
	 * Returns the value for the member attribute <b>tableType</b>
	 * @return int - value of member attribute <b>tableType</b>.
	 */
	public int getTableType( ) {
		return this.tableType;
	}

	/**
	 * This method sets the value to the member attribute <b>columnType</b>.
	 * You cannot set null to the attribute.
	 * @param columnType Value to set member attribute <b>columnType</b>
	 */
	public void setColumnType( int columnType ) {
		this.columnType = columnType;
	}

	/**
	 * Returns the value for the member attribute <b>columnType</b>
	 * @return int - value of member attribute <b>columnType</b>.
	 */
	public int getColumnType( ) {
		return this.columnType;
	}

	/**
	 * Returns the value for the member attribute <b>topologies</b>
	 *
	 * @return String - value of member attribute <b>topologies</b>.
	 */
	public String getTopologies() {
		return topologies;
	}

	/**
	 * This method sets the value to the member attribute <b>topologies</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param topologies
	 *            Value to set member attribute <b>topologies</b>
	 */
	public void setTopologies(String topologies) {
		this.topologies = topologies;
	}

	/**
	 * Returns the value for the member attribute <b>services</b>
	 *
	 * @return String - value of member attribute <b>services</b>.
	 */
	public String getServices() {
		return services;
	}

	/**
	 * This method sets the value to the member attribute <b>services</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param services
	 *            Value to set member attribute <b>services</b>
	 */
	public void setServices(String services) {
		this.services = services;
	}


	/**
	 * This method sets the value to the member attribute <b>hiveservices</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param hiveServices
	 *            Value to set member attribute <b>hiveservices</b>
	 */
	public void setHiveServices(String hiveServices) {
		this.hiveServices = hiveServices;
	}

	/**
	 * Returns the value for the member attribute <b>hiveservices</b>
	 *
	 * @return String - value of member attribute <b>hiveservices</b>.
	 */
	public String getHiveServices() {
		return hiveServices;
	}

	/**
	 * This method sets the value to the member attribute <b>checkParentPermission</b>.
	 * You cannot set null to the attribute.
	 * @param checkParentPermission Value to set member attribute <b>checkParentPermission</b>
	 */
	public void setCheckParentPermission( int checkParentPermission ) {
		this.checkParentPermission = checkParentPermission;
	}

	/**
	 * Returns the value for the member attribute <b>checkParentPermission</b>
	 * @return int - value of member attribute <b>checkParentPermission</b>.
	 */
	public int getCheckParentPermission( ) {
		return this.checkParentPermission;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_RESOURCE;
	}

	/**
	 * Returns the value for the member attribute <b>guid</b>
	 *
	 * @return String - value of member attribute <b>guid</b>.
	 */
	public String getGuid() {
		return guid;
	}

	/**
	 * This method sets the value to the member attribute <b>guid</b>. You
	 * cannot set null to the attribute.
	 *
	 * @param guid - Value to set member attribute <b>guid</b>
	 */
	public void setGuid(String guid) {
		this.guid = guid;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXResource={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "guid={" + guid + "} ";
		str += "policyName={" + policyName + "} ";
		str += "description={" + description + "} ";
		str += "resourceType={" + resourceType + "} ";
		str += "assetId={" + assetId + "} ";
		str += "parentId={" + parentId + "} ";
		str += "parentPath={" + parentPath + "} ";
		str += "isEncrypt={" + isEncrypt + "} ";
		str += "permMapList={" + permMapList + "} ";
		str += "auditList={" + auditList + "} ";
		str += "isRecursive={" + isRecursive + "} ";
		str += "resourceGroup={" + resourceGroup + "} ";
		str += "databases={" + databases + "} ";
		str += "tables={" + tables + "} ";
		str += "columnFamilies={" + columnFamilies + "} ";
		str += "columns={" + columns + "} ";
		str += "udfs={" + udfs + "} ";
		str += "assetName={" + assetName + "} ";
		str += "assetType={" + assetType + "} ";
		str += "resourceStatus={" + resourceStatus + "} ";
		str += "tableType={" + tableType + "} ";
		str += "columnType={" + columnType + "} ";
		str += "checkParentPermission={" + checkParentPermission + "} ";
		str += "topologies={" + topologies + "} ";
		str += "services={" + services + "} ";
		str += "}";
		return str;
	}
}
