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

import org.apache.ambari.server.topology.TopologyValidator;

import com.google.common.collect.ImmutableList;

public class TopologyValidatorFactory {
  List<TopologyValidator> validators;

  public TopologyValidatorFactory() {
    validators = ImmutableList.of(
      new RequiredConfigPropertiesValidator(),
      new RequiredPasswordValidator(),
      new HiveServiceValidator(),
      new StackConfigTypeValidator(),
      new UnitValidator(UnitValidatedProperty.ALL),
      new NameNodeHaValidator());
  }

  public TopologyValidator createConfigurationValidatorChain() {
    return new ChainedTopologyValidator(validators);
  }

}
