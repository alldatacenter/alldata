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

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.TopologyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates whether incoming config types (form the blueprint or the cluster creation template) are valid.
 * A configuration type is considered valid if the stack based on which the cluster is to be created contains such a
 * config type.
 */
public class StackConfigTypeValidator implements TopologyValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(StackConfigTypeValidator.class);

  public StackConfigTypeValidator() {
  }

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {

    // get the config types form the request
    Set<String> incomingConfigTypes = new HashSet<>(topology.getConfiguration().getAllConfigTypes());

    if (incomingConfigTypes.isEmpty()) {
      LOGGER.debug("No config types to be checked.");
      return;
    }

    Set<String> stackConfigTypes = new HashSet<>(topology.getBlueprint().getStack().getConfiguration().getAllConfigTypes());

    // remove all "valid" config types from the incoming set
    incomingConfigTypes.removeAll(stackConfigTypes);

    if (!incomingConfigTypes.isEmpty()) {
      // there are config types in the request that are not in the stack
      String message = String.format("The following config types are not defined in the stack: %s ", incomingConfigTypes);
      LOGGER.error(message);
      throw new InvalidTopologyException(message);
    }
  }
}




