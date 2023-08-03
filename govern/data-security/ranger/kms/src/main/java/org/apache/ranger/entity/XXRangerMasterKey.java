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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name="ranger_masterkey")
@XmlRootElement
public class XXRangerMasterKey extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@SequenceGenerator(name="RANGER_MASTERKEY_SEQ",sequenceName="RANGER_MASTERKEY_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="RANGER_MASTERKEY_SEQ")
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
	
	@Column(name="cipher")
	protected String cipher;

	public String getCipher() {
		return cipher;
	}

	public void setCipher(String cipher) {
		this.cipher = cipher;
	}
	
	@Column(name="bitlength")
	protected int bitLength;

	public int getBitLength() {
		return bitLength;
	}

	public void setBitLength(int bitLength) {
		this.bitLength = bitLength;
	}
		
	@Lob
	@Column(name="masterkey")
	protected String masterKey;

	public String getMasterKey() {
		return masterKey;
	}

	public void setMasterKey(String masterKey) {
		this.masterKey = masterKey;
	}		
}