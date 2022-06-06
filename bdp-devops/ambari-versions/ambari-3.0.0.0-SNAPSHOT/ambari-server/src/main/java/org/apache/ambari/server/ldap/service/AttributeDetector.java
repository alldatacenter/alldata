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

package org.apache.ambari.server.ldap.service;

import java.util.Map;

/**
 * Operations for detecting LDAP related settings.
 * The basis for the attribute or value detection is a set of entries returned by a search operation.
 * Individual attribute detector implementations are responsible for detecting a specific set of attributes or values
 */
public interface AttributeDetector<T> {

  /**
   * Collects potential attribute names or values from a set of result entries.
   *
   * @param entry a result entry returned by a search operation
   */
  void collect(T entry);

  /**
   * Implements the decision based on which the "best" possible attribute or value is selected.
   *
   * @return a map of the form <property-key, detected-value>
   */
  Map<String, String> detect();


}
