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

package org.apache.ranger.plugin.model;

import org.apache.ranger.authorization.utils.StringUtil;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RangerTag extends RangerBaseModelObject {
	private static final long serialVersionUID = 1L;

	public static final short OWNER_SERVICERESOURCE = 0;
	public static final short OWNER_GLOBAL          = 1;

	public static final String OPTION_TAG_VALIDITY_PERIODS = "TAG_VALIDITY_PERIODS";

	private String                       type;
	private Short                        owner;
	private Map<String, String>          attributes;
	private Map<String, Object>          options;
	private List<RangerValiditySchedule> validityPeriods;

	public RangerTag(String guid, String type, Map<String, String> attributes, Short owner, Map<String, Object> options, List<RangerValiditySchedule> validityPeriods) {
		super();

		setGuid(guid);
		setType(type);
		setOwner(owner);
		setAttributes(attributes);
		setOwner(owner);
		setOptions(options);
		setValidityPeriods(validityPeriods);
	}

	public RangerTag(String guid, String type, Map<String, String> attributes, Short owner) {
		this(guid, type, attributes, owner, null, null);
	}

	public RangerTag(String type, Map<String, String> attributes) {
		this(null, type, attributes, OWNER_SERVICERESOURCE, null, null);
	}

	public RangerTag() {
		this(null, null, null, OWNER_SERVICERESOURCE, null, null);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public Short getOwner() {
		return this.owner;
	}

	public void setOwner(Short owner) {
		this.owner = owner;
	}

	public Map<String, Object> getOptions() { return options; }

	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	public List<RangerValiditySchedule> getValidityPeriods() { return validityPeriods; }

	public void setValidityPeriods(List<RangerValiditySchedule> validityPeriods) {
		this.validityPeriods = validityPeriods;
	}

	public void dedupStrings(Map<String, String> strTbl) {
		type       = StringUtil.dedupString(type, strTbl);
		attributes = StringUtil.dedupStringsMap(attributes, strTbl);
		options    = StringUtil.dedupStringsMapOfObject(options, strTbl);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("RangerTag={");

		super.toString(sb);

		sb.append("type={").append(type).append("} ");
		sb.append("owner={").append(owner).append("} ");

		sb.append("attributes={");
		if (attributes != null) {
			for (Map.Entry<String, String> e : attributes.entrySet()) {
				sb.append(e.getKey()).append("={");
				sb.append(e.getValue());
				sb.append("} ");
			}
		}
		sb.append("} ");

		sb.append("options={");
		if (options != null) {
			for (Map.Entry<String, Object> e : options.entrySet()) {
				sb.append(e.getKey()).append("={");
				sb.append(e.getValue());
				sb.append("} ");
			}
		}
		sb.append("} ");

		if (validityPeriods != null) {
            sb.append("validityPeriods={").append(validityPeriods).append("} ");
        }
		sb.append("options={").append(options).append("} ");

        sb.append(" }");

		return sb;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((type == null) ? 0 : type.hashCode());
		result = prime * result
				+ ((owner == null) ? 0 : owner.hashCode());
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result
				+ ((options == null) ? 0 : options.hashCode());
		result = prime * result
				+ ((validityPeriods == null) ? 0 : validityPeriods.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RangerTag other = (RangerTag) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (validityPeriods == null) {
			if (other.validityPeriods != null)
				return false;
		} else if (!validityPeriods.equals(other.validityPeriods))
			return false;
		return true;
	}
}

