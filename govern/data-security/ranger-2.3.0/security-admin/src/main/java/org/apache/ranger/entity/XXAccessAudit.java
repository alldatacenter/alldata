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
 * Access Audit
 *
 */

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name="xa_access_audit")
@XmlRootElement
public class XXAccessAudit extends XXAccessAuditBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Column(name="SEQ_NUM")
	protected long sequenceNumber;

	@Column(name="EVENT_COUNT")
	protected long eventCount;

	//event duration in ms
	@Column(name="EVENT_DUR_MS")
	protected long eventDuration;

	public long getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	public long getEventCount() {
		return eventCount;
	}
	public void setEventCount(long eventCount) {
		this.eventCount = eventCount;
	}
	public long getEventDuration() {
		return eventDuration;
	}
	public void setEventDuration(long eventDuration) {
		this.eventDuration = eventDuration;
	}

	@Column(name="tags")
	protected String tags;
	
	/**
	 * @return the tags
	 */
	public String getTags() {
		return tags;
	}
	/**
	 * @param tags the tags to set
	 */
	public void setTags(String tags) {
		this.tags = tags;
	}
	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = super.toString();
		str += "sequenceNumber={" + sequenceNumber + "}";
		str += "eventCount={" + eventCount + "}";
		str += "eventDuration={" + eventDuration + "}";
		str += "tags={" + tags + "}";
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
		XXAccessAudit other = (XXAccessAudit) obj;
		if ((this.tags == null && other.tags != null) || (this.tags != null && !this.tags.equals(other.tags))) {
			return false;
		}
		return true;
	}

}
