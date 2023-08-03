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
 * Asset
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
@Table(name="x_asset")
@XmlRootElement
public class XXAsset extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_ASSET_SEQ",sequenceName="X_ASSET_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_ASSET_SEQ")
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
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="ASSET_NAME"  , nullable=false , length=1024)
	protected String name;

	/**
	 * Description
	 * <ul>
	 * <li>The maximum length for this attribute is <b>4000</b>.
	 * </ul>
	 *
	 */
	@Column(name="DESCR"  , nullable=false , length=4000)
	protected String description;

	/**
	 * Status
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ActiveStatus
	 * </ul>
	 *
	 */
	@Column(name="ACT_STATUS"  , nullable=false )
	protected int activeStatus = RangerConstants.STATUS_DISABLED;

	/**
	 * Type of asset
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::AssetType
	 * </ul>
	 *
	 */
	@Column(name="ASSET_TYPE"  , nullable=false )
	protected int assetType = AppConstants.ASSET_UNKNOWN;

	/**
	 * Config in json format
	 * <ul>
	 * <li>The maximum length for this attribute is <b>10000</b>.
	 * </ul>
	 *
	 */
	@Column(name="CONFIG"   , length=10000)
	protected String config;

	/**
	 * Support native authorization
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="SUP_NATIVE"  , nullable=false )
	protected boolean supportNative = false;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXAsset ( ) {
		activeStatus = RangerConstants.STATUS_DISABLED;
		assetType = AppConstants.ASSET_UNKNOWN;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_ASSET;
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
	 * This method sets the value to the member attribute <b>activeStatus</b>.
	 * You cannot set null to the attribute.
	 * @param activeStatus Value to set member attribute <b>activeStatus</b>
	 */
	public void setActiveStatus( int activeStatus ) {
		this.activeStatus = activeStatus;
	}

	/**
	 * Returns the value for the member attribute <b>activeStatus</b>
	 * @return int - value of member attribute <b>activeStatus</b>.
	 */
	public int getActiveStatus( ) {
		return this.activeStatus;
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
	 * This method sets the value to the member attribute <b>config</b>.
	 * You cannot set null to the attribute.
	 * @param config Value to set member attribute <b>config</b>
	 */
	public void setConfig( String config ) {
		this.config = config;
	}

	/**
	 * Returns the value for the member attribute <b>config</b>
	 * @return String - value of member attribute <b>config</b>.
	 */
	public String getConfig( ) {
		return this.config;
	}

	/**
	 * This method sets the value to the member attribute <b>supportNative</b>.
	 * You cannot set null to the attribute.
	 * @param supportNative Value to set member attribute <b>supportNative</b>
	 */
	public void setSupportNative( boolean supportNative ) {
		this.supportNative = supportNative;
	}

	/**
	 * Returns the value for the member attribute <b>supportNative</b>
	 * @return boolean - value of member attribute <b>supportNative</b>.
	 */
	public boolean isSupportNative( ) {
		return this.supportNative;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXAsset={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "description={" + description + "} ";
		str += "activeStatus={" + activeStatus + "} ";
		str += "assetType={" + assetType + "} ";
		str += "config={" + config + "} ";
		str += "supportNative={" + supportNative + "} ";
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
		XXAsset other = (XXAsset) obj;
        	if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            		return false;
        	}
        	if ((this.description == null && other.description != null) || (this.description != null && !this.description.equals(other.description))) {
            		return false;
        	}
		if( this.activeStatus != other.activeStatus ) return false;
		if( this.assetType != other.assetType ) return false;
        	if ((this.config == null && other.config != null) || (this.config != null && !this.config.equals(other.config))) {
            		return false;
        	}
		if( this.supportNative != other.supportNative ) return false;
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "activeStatus".equals(fieldName) ) {
			return "CommonEnums.ActiveStatus";
		}
		if( "assetType".equals(fieldName) ) {
			return "CommonEnums.AssetType";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
