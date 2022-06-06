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
package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.ServiceRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.SecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@Experimental(
    feature = ExperimentalFeature.ORPHAN_KERBEROS_IDENTITY_REMOVAL,
    comment = "This might need to have a switch so that it can be turned off if it is found to be desctructive to certain clusters")
public class KerberosIdentityCleaner {
  private final static Logger LOG = LoggerFactory.getLogger(KerberosIdentityCleaner.class);
  private final AmbariEventPublisher eventPublisher;
  private final KerberosHelper kerberosHelper;
  private final Clusters clusters;

  @Inject
  public KerberosIdentityCleaner(AmbariEventPublisher eventPublisher, KerberosHelper kerberosHelper, Clusters clusters) {
    this.eventPublisher = eventPublisher;
    this.kerberosHelper = kerberosHelper;
    this.clusters = clusters;
  }

  public void register() {
    eventPublisher.register(this);
  }

  /**
   * Removes kerberos identities (principals and keytabs) after a component was uninstalled.
   * Keeps the identity if either the principal or the keytab is used by an other service
   */
  @Subscribe
  public void componentRemoved(ServiceComponentUninstalledEvent event) throws KerberosMissingAdminCredentialsException {
    try {
      Cluster cluster = clusters.getCluster(event.getClusterId());

      if (cluster.getSecurityType() == SecurityType.KERBEROS) {
        if (null != cluster.getUpgradeInProgress()) {
          LOG.info("Skipping removal of identities for {} since there is an upgrade in progress",
              event.getComponentName());

          return;
        }

        LOG.info("Removing identities after {}", event);
        RemovableIdentities
            .ofComponent(clusters.getCluster(event.getClusterId()), event, kerberosHelper)
            .remove(kerberosHelper);
      }
    } catch (Exception e) {
      LOG.error("Error while deleting kerberos identity after an event: " + event, e);
    }
  }

  /**
   * Removes kerberos identities (principals and keytabs) after a service was uninstalled.
   * Keeps the identity if either the principal or the keytab is used by an other service
   */
  @Subscribe
  public void serviceRemoved(ServiceRemovedEvent event) {
    try {
      Cluster cluster = clusters.getCluster(event.getClusterId());

      if (cluster.getSecurityType() == SecurityType.KERBEROS) {
        if (null != cluster.getUpgradeInProgress()) {
          LOG.info("Skipping removal of identities for {} since there is an upgrade in progress",
              event.getServiceName());

          return;
        }

        LOG.info("Removing identities after {}", event);
        RemovableIdentities
            .ofService(clusters.getCluster(event.getClusterId()), event, kerberosHelper)
            .remove(kerberosHelper);
      }
    } catch (Exception e) {
      LOG.error("Error while deleting kerberos identity after an event: " + event, e);
    }
  }
}

