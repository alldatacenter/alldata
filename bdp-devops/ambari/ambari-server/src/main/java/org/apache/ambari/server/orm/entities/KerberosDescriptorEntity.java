package org.apache.ambari.server.orm.entities;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Table(name = "kerberos_descriptor")
@Entity
@NamedQuery(name = "allKerberosDescriptors",
        query = "SELECT kerberosDescriptor  FROM KerberosDescriptorEntity kerberosDescriptor")
public class KerberosDescriptorEntity {

  @Id
  @Column(name = "kerberos_descriptor_name", nullable = false, insertable = true, updatable = false,
          unique = true, length = 100)
  private String name;

  @Column(name = "kerberos_descriptor", nullable = false, insertable = true, updatable = false)
  private String kerberosDescriptorText;


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKerberosDescriptorText() {
    return kerberosDescriptorText;
  }

  public void setKerberosDescriptorText(String kerberosDescriptorText) {
    this.kerberosDescriptorText = kerberosDescriptorText;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    KerberosDescriptorEntity other = (KerberosDescriptorEntity) obj;
    return Objects.equals(name, other.name) &&
      Objects.equals(kerberosDescriptorText, other.kerberosDescriptorText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, kerberosDescriptorText);
  }
}
