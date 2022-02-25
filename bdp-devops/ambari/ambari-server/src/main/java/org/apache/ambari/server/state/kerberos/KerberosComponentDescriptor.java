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
package org.apache.ambari.server.state.kerberos;

import java.util.Collection;
import java.util.Map;

/*
 * KerberosComponentDescriptor implements AbstractKerberosDescriptorContainer. It contains the data
 * related to a component which include the following properties:
 * <ul>
 * <li>name</li>
 * <li>identities</li>
 * <li>configurations</li>
 * </ul>
 * Example:
 * <pre>
 *  {
 *    "name" : "COMPONENT_NAME",
 *    "identities" : { ... },
 *    "configurations" : { ... }
 *  }
 *  </pre>
 */

/**
 * KerberosComponentDescriptor is an implementation of an AbstractKerberosDescriptorContainer that
 * encapsulates the details about an Ambari component.
 * <p/>
 * A KerberosComponentDescriptor has the following properties:
 * <ul>
 * <li>identities ({@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer})</li>
 * <li>configurations ({@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer})</li>
 * </ul>
 * <p/>
 * The following (pseudo) JSON Schema will yield a valid KerberosComponentDescriptor
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "KerberosComponentDescriptor",
 *      "description": "Describes an Ambari component",
 *      "type": "object",
 *      "properties": {
 *        "identities": {
 *          "description": "A list of Kerberos identity descriptors",
 *          "type": "array",
 *          "items": {
 *            "title": "KerberosIdentityDescriptor"
 *            "type": "{@link org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor}"
 *          }
 *        },
 *        "configurations": {
 *          "description": "A list of relevant configuration blocks",
 *          "type": "array",
 *          "items": {
 *            "title": "KerberosConfigurationDescriptor"
 *            "type": "{@link org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor}"
 *          }
 *        }
 *      }
 *   }
 * </pre>
 * <p/>
 * In this implementation,
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will hold the
 * KerberosComponentDescriptor#name value
 */
public class KerberosComponentDescriptor extends AbstractKerberosDescriptorContainer {

  /**
   * Creates a new KerberosComponentDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor} for an
   * example JSON structure that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor
   */
  public KerberosComponentDescriptor(Map<?, ?> data) {
    super(data);

    // The name for this KerberosComponentDescriptor is stored in the "name" entry in the map
    // This is not automatically set by the super classes.
    setName(getStringValue(data, "name"));
  }

  @Override
  public Collection<? extends AbstractKerberosDescriptorContainer> getChildContainers() {
    // KerberosComponentDescriptors do not have child components
    return null;
  }

  @Override
  public AbstractKerberosDescriptorContainer getChildContainer(String name) {
    // KerberosComponentDescriptors do not have child components
    return null;
  }

  @Override
  public int hashCode() {
    return 35 * super.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    return (object == this) ||
        (
            (object != null) &&
                (object.getClass() == KerberosComponentDescriptor.class) &&
                super.equals(object)
        );
  }
}
