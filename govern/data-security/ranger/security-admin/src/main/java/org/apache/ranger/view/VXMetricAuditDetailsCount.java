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
public class VXMetricAuditDetailsCount implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	protected Long solrIndexCountTwoDays;
	protected VXMetricServiceCount accessEventsCountTwoDays;
	protected VXMetricServiceCount denialEventsCountTwoDays;
	protected Long solrIndexCountWeek;
	protected VXMetricServiceCount accessEventsCountWeek;
	protected VXMetricServiceCount denialEventsCountWeek;
	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXMetricAuditDetailsCount() {
	}
	/**
	 * @return the solrIndexCountTwoDays
	 */
	public Long getSolrIndexCountTwoDays() {
		return solrIndexCountTwoDays;
	}
	/**
	 * @param solrIndexCountTwoDays the solrIndexCountTwoDays to set
	 */
	public void setSolrIndexCountTwoDays(Long solrIndexCountTwoDays) {
		this.solrIndexCountTwoDays = solrIndexCountTwoDays;
	}
	/**
	 * @return the accessEventsCountTwoDays
	 */
	public VXMetricServiceCount getAccessEventsCountTwoDays() {
		return accessEventsCountTwoDays;
	}
	/**
	 * @param accessEventsCountTwoDays the accessEventsCountTwoDays to set
	 */
	public void setAccessEventsCountTwoDays(
			VXMetricServiceCount accessEventsCountTwoDays) {
		this.accessEventsCountTwoDays = accessEventsCountTwoDays;
	}
	/**
	 * @return the denialEventsCountTwoDays
	 */
	public VXMetricServiceCount getDenialEventsCountTwoDays() {
		return denialEventsCountTwoDays;
	}
	/**
	 * @param denialEventsCountTwoDays the denialEventsCountTwoDays to set
	 */
	public void setDenialEventsCountTwoDays(
			VXMetricServiceCount denialEventsCountTwoDays) {
		this.denialEventsCountTwoDays = denialEventsCountTwoDays;
	}
	/**
	 * @return the solrIndexCountWeek
	 */
	public Long getSolrIndexCountWeek() {
		return solrIndexCountWeek;
	}
	/**
	 * @param solrIndexCountWeek the solrIndexCountWeek to set
	 */
	public void setSolrIndexCountWeek(Long solrIndexCountWeek) {
		this.solrIndexCountWeek = solrIndexCountWeek;
	}
	/**
	 * @return the accessEventsCountWeek
	 */
	public VXMetricServiceCount getAccessEventsCountWeek() {
		return accessEventsCountWeek;
	}
	/**
	 * @param accessEventsCountWeek the accessEventsCountWeek to set
	 */
	public void setAccessEventsCountWeek(VXMetricServiceCount accessEventsCountWeek) {
		this.accessEventsCountWeek = accessEventsCountWeek;
	}
	/**
	 * @return the denialEventsCountWeek
	 */
	public VXMetricServiceCount getDenialEventsCountWeek() {
		return denialEventsCountWeek;
	}
	/**
	 * @param denialEventsCountWeek the denialEventsCountWeek to set
	 */
	public void setDenialEventsCountWeek(VXMetricServiceCount denialEventsCountWeek) {
		this.denialEventsCountWeek = denialEventsCountWeek;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "VXMetricAuditDetailsCount [solrIndexCountTwoDays="
				+ solrIndexCountTwoDays + ", accessEventsCountTwoDays="
				+ accessEventsCountTwoDays + ", denialEventsCountTwoDays="
				+ denialEventsCountTwoDays + ", solrIndexCountWeek="
				+ solrIndexCountWeek + ", accessEventsCountWeek="
				+ accessEventsCountWeek + ", denialEventsCountWeek="
				+ denialEventsCountWeek + "]";
	}	
}
