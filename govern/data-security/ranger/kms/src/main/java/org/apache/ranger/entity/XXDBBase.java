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

package org.apache.ranger.entity;

/**
 * Base JPA class with id, versionNumber and other common attributes
 *
 */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

@MappedSuperclass
@XmlRootElement
public abstract class XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	public static final int CLASS_TYPE_NONE = 0;
	private static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT+0");
	
	/**
	 * Date/Time creation of this user.
	 * <ul>
	 * </ul>
	 *
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="CREATE_TIME"   )
	protected Date createTime = getUTCDate();

	/**
	 * Date value.
	 * <ul>
	 * </ul>
	 *
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="UPDATE_TIME"   )
	protected Date updateTime = getUTCDate();

	/**
	 * Added by
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="ADDED_BY_ID"   )
	protected Long addedByUserId;


	/**
	 * Last updated by
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="UPD_BY_ID"   )
	protected Long updatedByUserId;


	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXDBBase ( ) {
	}

	public int getMyClassType( ) {
	    return CLASS_TYPE_NONE;
	}

	public String getMyDisplayValue() {
		return null;
	}

	/**
	 * This method sets the value to the member attribute <b>id</b>.
	 * You cannot set null to the attribute.
	 * @param id Value to set member attribute <b>id</b>
	 */
	public abstract void setId( Long id );

	/**
	 * Returns the value for the member attribute <b>id</b>
	 * @return Long - value of member attribute <b>id</b>.
	 */
	public abstract Long getId( );

	/**
	 * This method sets the value to the member attribute <b>createTime</b>.
	 * You cannot set null to the attribute.
	 * @param createTime Value to set member attribute <b>createTime</b>
	 */
	public void setCreateTime( Date createTime ) {
		this.createTime = createTime;
	}

	/**
	 * Returns the value for the member attribute <b>createTime</b>
	 * @return Date - value of member attribute <b>createTime</b>.
	 */
	public Date getCreateTime( ) {
		return this.createTime;
	}

	/**
	 * This method sets the value to the member attribute <b>updateTime</b>.
	 * You cannot set null to the attribute.
	 * @param updateTime Value to set member attribute <b>updateTime</b>
	 */
	public void setUpdateTime( Date updateTime ) {
		this.updateTime = updateTime;
	}

	/**
	 * Returns the value for the member attribute <b>updateTime</b>
	 * @return Date - value of member attribute <b>updateTime</b>.
	 */
	public Date getUpdateTime( ) {
		return this.updateTime;
	}

	/**
	 * This method sets the value to the member attribute <b>addedByUserId</b>.
	 * You cannot set null to the attribute.
	 * @param addedByUserId Value to set member attribute <b>addedByUserId</b>
	 */
	public void setAddedByUserId( Long addedByUserId ) {
		this.addedByUserId = addedByUserId;
	}

	/**
	 * Returns the value for the member attribute <b>addedByUserId</b>
	 * @return Long - value of member attribute <b>addedByUserId</b>.
	 */
	public Long getAddedByUserId( ) {
		return this.addedByUserId;
	}


	/**
	 * This method sets the value to the member attribute <b>updatedByUserId</b>.
	 * You cannot set null to the attribute.
	 * @param updatedByUserId Value to set member attribute <b>updatedByUserId</b>
	 */
	public void setUpdatedByUserId( Long updatedByUserId ) {
		this.updatedByUserId = updatedByUserId;
	}

	/**
	 * Returns the value for the member attribute <b>updatedByUserId</b>
	 * @return Long - value of member attribute <b>updatedByUserId</b>.
	 */
	public Long getUpdatedByUserId( ) {
		return this.updatedByUserId;
	}


	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXDBBase={";
		//`str += "id={" + id + "} ";
		str += "createTime={" + createTime + "} ";
		str += "updateTime={" + updateTime + "} ";
		str += "addedByUserId={" + addedByUserId + "} ";
		str += "updatedByUserId={" + updatedByUserId + "} ";
		str += "}";
		return str;
	}

	/**
	 * Checks for all attributes except referenced db objects
	 * @return true if all attributes match
	*/
	@Override
	public boolean equals( Object obj) {
		XXDBBase other = (XXDBBase) obj;
        	if ((this.createTime == null && other.createTime != null) || (this.createTime != null && !this.createTime.equals(other.createTime))) {
            		return false;
        	}
        	if ((this.updateTime == null && other.updateTime != null) || (this.updateTime != null && !this.updateTime.equals(other.updateTime))) {
            		return false;
        	}
        	if ((this.addedByUserId == null && other.addedByUserId != null) || (this.addedByUserId != null && !this.addedByUserId.equals(other.addedByUserId))) {
            		return false;
        	}
        	if ((this.updatedByUserId == null && other.updatedByUserId != null) || (this.updatedByUserId != null && !this.updatedByUserId.equals(other.updatedByUserId))) {
            		return false;
        	}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		return null;
	}

	private static Date getUTCDate(){
		try{
			Calendar local=Calendar.getInstance();
		    int offset = local.getTimeZone().getOffset(local.getTimeInMillis());
		    GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
		    utc.setTimeInMillis(local.getTimeInMillis());
		    utc.add(Calendar.MILLISECOND, -offset);
		    return utc.getTime();
		}catch(Exception ex){
			return null;
		}
	}
}
