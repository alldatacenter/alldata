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

 package org.apache.ranger.entity;

/**
 * Resource
 *
 */

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.RangerConstants;


@Entity
@Table(name="x_resource")
@XmlRootElement
public class XXResource extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	@Id
	@SequenceGenerator(name="X_RESOURCE_SEQ",sequenceName="X_RESOURCE_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_RESOURCE_SEQ")
	@Column(name="ID")
	protected Long id;
	@Override
	public void setId(Long id) {
		this.id=id;
	}
	@Override
	public Long getId() {
		return id;
	}
	/**
	 * Name
	 * <ul>
	 * <li>The maximum length for this attribute is <b>4000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_NAME"   , length=4000)
	protected String name;
	
	@Column(name="POLICY_NAME"   , length=500)
	protected String policyName;
	/**
	 * Description
	 * <ul>
	 * <li>The maximum length for this attribute is <b>4000</b>.
	 * </ul>
	 *
	 */
	@Column(name="DESCR"   , length=4000)
	protected String description;

	/**
	 * Status
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ResourceType
	 * </ul>
	 *
	 */
	@Column(name="RES_TYPE"  , nullable=false )
	protected int resourceType = AppConstants.RESOURCE_PATH;

	/**
	 * Id of the asset
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="ASSET_ID"  , nullable=false )
	protected Long assetId;


	/**
	 * Id of the parent
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="PARENT_ID"   )
	protected Long parentId;


	/**
	 * Path for the parent
	 * <ul>
	 * <li>The maximum length for this attribute is <b>4000</b>.
	 * </ul>
	 *
	 */
	@Column(name="PARENT_PATH"   , length=4000)
	protected String parentPath;

	/**
	 * Whether to encrypt this resource
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::BooleanValue
	 * </ul>
	 *
	 */
	@Column(name="IS_ENCRYPT"  , nullable=false )
	protected int isEncrypt = RangerConstants.BOOL_FALSE;

	/**
	 * Is recursive
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::BooleanValue
	 * </ul>
	 *
	 */
	@Column(name="IS_RECURSIVE"  , nullable=false )
	protected int isRecursive = RangerConstants.BOOL_NONE;

	/**
	 * Group to which this resource belongs to
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_GROUP"   , length=1024)
	protected String resourceGroup;

	/**
	 * Databases
	 * <ul>
	 * <li>The maximum length for this attribute is <b>10000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_DBS"   , length=10000)
	protected String databases;

	/**
	 * Tables
	 * <ul>
	 * <li>The maximum length for this attribute is <b>10000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_TABLES"   , length=10000)
	protected String tables;

	/**
	 * Column families
	 * <ul>
	 * <li>The maximum length for this attribute is <b>10000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_COL_FAMS"   , length=10000)
	protected String columnFamilies;

	/**
	 * Columns
	 * <ul>
	 * <li>The maximum length for this attribute is <b>10000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_COLS"   , length=10000)
	protected String columns;

	/**
	 * UDFs
	 * <ul>
	 * <li>The maximum length for this attribute is <b>10000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_UDFS"   , length=10000)
	protected String udfs;

	/**
	 * Resource Status
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ActiveStatus
	 * </ul>
	 *
	 */
	@Column(name="RES_STATUS"  , nullable=false )
	protected int resourceStatus = RangerConstants.STATUS_ENABLED;

	/**
	 * Table Type
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::PolicyType
	 * </ul>
	 *
	 */
	@Column(name="TABLE_TYPE"  , nullable=false )
	protected int tableType = AppConstants.POLICY_INCLUSION;

	/**
	 * Resource Status
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::PolicyType
	 * </ul>
	 *
	 */
	@Column(name="COL_TYPE"  , nullable=false )
	protected int columnType = AppConstants.POLICY_INCLUSION;
	/**
	 * Topologoies
	 * <ul>
	 * <li>The maximum length for this attribute is <b>10000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_TOPOLOGIES"   , length=10000)
	protected String topologies;
	/**
	 * SERVICENAMES
	 * <ul>
	 * <li>The maximum length for this attribute is <b>10000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RES_SERVICES"   , length=10000)
	protected String services;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXResource ( ) {
		resourceType = AppConstants.RESOURCE_PATH;
		isEncrypt = RangerConstants.BOOL_FALSE;
		isRecursive = RangerConstants.BOOL_NONE;
		resourceStatus = RangerConstants.STATUS_ENABLED;
		tableType = AppConstants.POLICY_INCLUSION;
		columnType = AppConstants.POLICY_INCLUSION;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_RESOURCE;
	}

	@Override
	public String getMyDisplayValue() {
		return getDescription( );
	}

	/**
	 * This method sets the value to the member attribute <b>name</b>.
	 * You cannot set null to the attribute.
	 * @param name Value to set member attribute <b>name</b>
	 */
	public void setName( String name ) {
		this.name = name;
	}

	public String getPolicyName() {
		return policyName;
	}

	public void setPolicyName(String policyName) {
		this.policyName = policyName;
	}

	/**
	 * Returns the value for the member attribute <b>name</b>
	 * @return String - value of member attribute <b>name</b>.
	 */
	public String getName( ) {
		return this.name;
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
	 * @return String - value of member attribute <b>topologies</b>.
	 */
	public String getTopologies() {
		return topologies;
	}

	/**
	 * This method sets the value to the member attribute <b>topologies</b>.
	 * You cannot set null to the attribute.
	 * @param topologies Value to set member attribute <b>topologies</b>
	 */
	public void setTopologies(String topologies) {
		this.topologies = topologies;
	}

	/**
	 * Returns the value for the member attribute <b>services</b>
	 * @return String - value of member attribute <b>services</b>.
	 */
	public String getServices() {
		return services;
	}

	/**
	 * This method sets the value to the member attribute <b>services</b>.
	 * You cannot set null to the attribute.
	 * @param services Value to set member attribute <b>services</b>
	 */
	public void setServices(String services) {
		this.services = services;
	}
	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXResource={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "policyName={" + policyName + "} ";
		str += "description={" + description + "} ";
		str += "resourceType={" + resourceType + "} ";
		str += "assetId={" + assetId + "} ";
		str += "parentId={" + parentId + "} ";
		str += "parentPath={" + parentPath + "} ";
		str += "isEncrypt={" + isEncrypt + "} ";
		str += "isRecursive={" + isRecursive + "} ";
		str += "resourceGroup={" + resourceGroup + "} ";
		str += "databases={" + databases + "} ";
		str += "tables={" + tables + "} ";
		str += "columnFamilies={" + columnFamilies + "} ";
		str += "columns={" + columns + "} ";
		str += "udfs={" + udfs + "} ";
		str += "resourceStatus={" + resourceStatus + "} ";
		str += "tableType={" + tableType + "} ";
		str += "columnType={" + columnType + "} ";
		str += "topologies={" + topologies + "} ";
		str += "services={" + services + "} ";
		str += "}";
		return str;
	}

	/**
	 * Checks for all attributes except referenced db objects
	 * @return true if all attributes match
	*/
	@Override
	public boolean equals( Object obj) {
		if ( !super.equals(obj) ) {
			return false;
		}
		XXResource other = (XXResource) obj;
        	if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            		return false;
        	}
        	if ((this.description == null && other.description != null) || (this.description != null && !this.description.equals(other.description))) {
            		return false;
        	}
		if( this.resourceType != other.resourceType ) return false;
        	if ((this.assetId == null && other.assetId != null) || (this.assetId != null && !this.assetId.equals(other.assetId))) {
            		return false;
        	}
        	if ((this.parentId == null && other.parentId != null) || (this.parentId != null && !this.parentId.equals(other.parentId))) {
            		return false;
        	}
        	if ((this.parentPath == null && other.parentPath != null) || (this.parentPath != null && !this.parentPath.equals(other.parentPath))) {
            		return false;
        	}
		if( this.isEncrypt != other.isEncrypt ) return false;
		if( this.isRecursive != other.isRecursive ) return false;
        	if ((this.resourceGroup == null && other.resourceGroup != null) || (this.resourceGroup != null && !this.resourceGroup.equals(other.resourceGroup))) {
            		return false;
        	}
        	if ((this.databases == null && other.databases != null) || (this.databases != null && !this.databases.equals(other.databases))) {
            		return false;
        	}
        	if ((this.tables == null && other.tables != null) || (this.tables != null && !this.tables.equals(other.tables))) {
            		return false;
        	}
        	if ((this.columnFamilies == null && other.columnFamilies != null) || (this.columnFamilies != null && !this.columnFamilies.equals(other.columnFamilies))) {
            		return false;
        	}
        	if ((this.columns == null && other.columns != null) || (this.columns != null && !this.columns.equals(other.columns))) {
            		return false;
        	}
        	if ((this.udfs == null && other.udfs != null) || (this.udfs != null && !this.udfs.equals(other.udfs))) {
            		return false;
        	}
		if( this.resourceStatus != other.resourceStatus ) return false;
		if( this.tableType != other.tableType ) return false;
		if( this.columnType != other.columnType ) return false;
		
		if ((this.topologies == null && other.topologies != null)
				|| (this.topologies != null && !this.topologies.equals(other.topologies))) {
			return false;
		}
		if ((this.services == null && other.services != null)
				|| (this.services != null && !this.services.equals(other.services))) {
			return false;
		}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "resourceType".equals(fieldName) ) {
			return "CommonEnums.ResourceType";
		}
		if( "isEncrypt".equals(fieldName) ) {
			return "CommonEnums.BooleanValue";
		}
		if( "isRecursive".equals(fieldName) ) {
			return "CommonEnums.BooleanValue";
		}
		if( "resourceStatus".equals(fieldName) ) {
			return "CommonEnums.ActiveStatus";
		}
		if( "tableType".equals(fieldName) ) {
			return "CommonEnums.PolicyType";
		}
		if( "columnType".equals(fieldName) ) {
			return "CommonEnums.PolicyType";
		}
		if( "assetType".equals(fieldName) ) {
			return "CommonEnums.AssetType";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
