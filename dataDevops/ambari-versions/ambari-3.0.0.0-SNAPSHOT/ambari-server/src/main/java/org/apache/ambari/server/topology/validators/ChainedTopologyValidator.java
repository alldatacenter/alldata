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

import java.util.List;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.TopologyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Topology validator wrapper implementation. Executes a set of validations by calling a preconfgured set of validator implementations.
 */
public class ChainedTopologyValidator implements TopologyValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChainedTopologyValidator.class);
  private List<TopologyValidator> validators;

  public ChainedTopologyValidator(List<TopologyValidator> validators) {
    this.validators = validators;
  }

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    for (TopologyValidator validator : validators) {
      LOGGER.info("Performing topology validation: {}", validator.getClass());
      validator.validate(topology);
    }
  }
}
