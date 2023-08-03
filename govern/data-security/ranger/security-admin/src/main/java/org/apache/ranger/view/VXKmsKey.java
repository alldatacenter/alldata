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
 * Key
 *
 */

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXKmsKey extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Name
	 */
	protected String name;
	/**
	 * Cipher
	 */
	protected String cipher;
	/**
	 * Length
	 */
	protected int length;
	/**
	 * Description
	 */
	protected String description;
	/**
	 * Version
	 */
	protected int versions;
	/**
	 * Material
	 */
	protected String material;
	/**
	 * Version Name
	 */
	protected String versionName;
	
	/**
	 * Key Created Date
	 */
	protected Long created;
	
	/**
	 * Attributes
	 */
	protected Map<String, String> attributes;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXKmsKey ( ) {	
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the cipher
	 */
	public String getCipher() {
		return cipher;
	}

	/**
	 * @param cipher the cipher to set
	 */
	public void setCipher(String cipher) {
		this.cipher = cipher;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @param length the length to set
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the version
	 */
	public int getVersions() {
		return versions;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersions(int versions) {
		this.versions = versions;
	}

	/**
	 * @return the material
	 */
	public String getMaterial() {
		return material;
	}

	/**
	 * @param material the material to set
	 */
	public void setMaterial(String material) {
		this.material = material;
	}

	/**
	 * @return the versionName
	 */
	public String getVersionName() {
		return versionName;
	}

	/**
	 * @param versionName the versionName to set
	 */
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	/**
	 * @return the created
	 */
	public Long getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Long created) {
		this.created = created;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}	

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_KMS_KEY;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXUser={";
		str += super.toString();
		str += "name={" + name + "} ";
		str += "cipher={" + cipher + "} ";
		str += "length={" + length + "} ";
		str += "description={" + description + "} ";
		str += "atrribute={" + attributes + "} ";
		str += "created={" + created.toString() + "} ";
		str += "version={" + versions + "} ";
		str += "material={" + material + "} ";
		str += "versionName={" + versionName + "} ";
		str += "}";
		return str;
	}
}
