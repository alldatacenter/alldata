/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.stack.upgrade;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.KerberosDetails;
import org.apache.ambari.server.serveraction.kerberos.KDCType;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.SecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * The {@link SecurityCondition} class is used to represent that the cluster has
 * been configured for a specific type of security model.
 *
 * @see SecurityType
 */
@XmlType(name = "security")
@XmlAccessorType(XmlAccessType.FIELD)
public final class SecurityCondition extends Condition {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityCondition.class);

  /**
   * The type of security which much be enabled.
   */
  @XmlAttribute(name = "type")
  public SecurityType securityType;

  /**
   * The type of Kerberos when the type of security is
   * {@link SecurityType#KERBEROS}.
   */
  @XmlAttribute(name = "kdc-type", required = false)
  public KDCType kdctype;

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("type", securityType)
        .add("kdcType", kdctype)
        .omitNullValues().toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSatisfied(UpgradeContext upgradeContext) {
    Cluster cluster = upgradeContext.getCluster();

    // if the security types don't match, then don't process any further
    if (cluster.getSecurityType() != securityType) {
      return false;
    }

    switch( securityType ) {
      case KERBEROS:
        // if KDC type is specified, then match on it
        if( null != kdctype ) {
          try {
            KerberosDetails kerberosDetails = upgradeContext.getKerberosDetails();
            return kerberosDetails.getKdcType() == kdctype;
          } catch (AmbariException | KerberosInvalidConfigurationException kerberosException) {
            LOG.error(
                "Unable to determine if this upgrade condition is met because there was a problem parsing the Kerberos configruations for the KDC Type",
                kerberosException);
            return false;
          }
        }

        // security type matches Kerberos, return true
        return true;
      case NONE:
        // nothing to do here is the security type is NONE
        return true;
      default:
        // nothing to do here if it's an unknown type
        return true;
    }
  }
}

