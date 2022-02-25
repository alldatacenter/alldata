/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology.validators;

import javax.inject.Inject;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service implementation dealing with topology validation.
 * It's intended to manage cluster topology validation by grouping validators into different sets as it's imposed by the
 * callee logic.
 *
 * Ideally this service should be used as instead of directly use validator implementations.
 */
public class TopologyValidatorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyValidatorService.class);

  @Inject
  private TopologyValidatorFactory topologyValidatorFactory;

  public TopologyValidatorService() {
  }

  public void validateTopologyConfiguration(ClusterTopology clusterTopology) throws InvalidTopologyException {
    LOGGER.info("Validating cluster topology: {}", clusterTopology);
    topologyValidatorFactory.createConfigurationValidatorChain().validate(clusterTopology);
  }

}






