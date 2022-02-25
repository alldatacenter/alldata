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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.ambari.server.AmbariException;

import com.google.inject.Singleton;

/**
 * KerberosServiceDescriptorFactory is a factory class used to create KerberosServiceDescriptor
 * instances using various sources of data.
 */
@Singleton
public class KerberosServiceDescriptorFactory extends AbstractKerberosDescriptorFactory {


  /**
   * Creates a Collection of KerberosServiceDescriptors parsed from a JSON-formatted file.
   * <p/>
   * The file is expected to be formatted as follows:
   * <pre>
   * {
   *    "services" : [
   *      ... (zero or more service descriptor blocks) ...
   *    ]
   * }
   * </pre>
   *
   * @param file a JSON-formatted file containing this service-level descriptor data
   * @return an array of KerberosServiceDescriptor objects
   * @throws java.io.FileNotFoundException if the specified File does not point to a valid file
   * @throws IOException                   if the specified File is not a readable file
   * @throws AmbariException               if the specified File does not contain valid JSON data
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  public KerberosServiceDescriptor[] createInstances(File file) throws IOException {
    try {
      return createInstances(parseFile(file));
    } catch (AmbariException e) {
      throw new AmbariException(String.format("An error occurred processing the JSON-formatted file: %s", file.getAbsolutePath()), e);
    }
  }

  /**
   * Creates a Collection of KerberosServiceDescriptors parsed from a JSON-formatted String.
   * <p/>
   * The String is expected to be formatted as follows:
   * <pre>
   * {
   *    "services" : [
   *      ... (zero or more service descriptor blocks) ...
   *    ]
   * }
   * </pre>
   *
   * @param json a JSON-formatted String containing this service-level descriptor data
   * @return an array of KerberosServiceDescriptor objects
   * @throws AmbariException if an error occurs while processing the JSON-formatted String
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  public KerberosServiceDescriptor[] createInstances(String json) throws AmbariException {
    try {
      return createInstances(parseJSON(json));
    } catch (AmbariException e) {
      throw new AmbariException("An error occurred processing the JSON-formatted string", e);
    }
  }

  /**
   * Creates a Collection of KerberosServiceDescriptors parsed from a Map of data.
   * <p/>
   * The Map is expected to be formatted as follows:
   * <pre>
   * "services" => [
   *   ... (zero or more Maps containing service descriptor data) ...
   * ]
   * </pre>
   *
   * @param map a Map containing this service-level descriptor data
   * @return an array of KerberosServiceDescriptor objects
   * @throws org.apache.ambari.server.AmbariException if an error occurs while processing the Map
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  public KerberosServiceDescriptor[] createInstances(Map<String, Object> map) throws AmbariException {
    ArrayList<KerberosServiceDescriptor> descriptors = new ArrayList<>();

    if (map != null) {
      Object servicesData = map.get("services");

      if (servicesData == null) {
        throw new AmbariException("Missing top-level \"services\" property in service-level Kerberos descriptor data");
      } else if (servicesData instanceof Collection) {
        for (Object serviceData : (Collection) servicesData) {
          if (serviceData instanceof Map) {
            descriptors.add(new KerberosServiceDescriptor((Map) serviceData));
          }
        }
      } else {
        throw new AmbariException(String.format("Unexpected top-level \"services\" type in service-level Kerberos descriptor data: %s",
            servicesData.getClass().getName()));
      }
    }

    return descriptors.toArray(new KerberosServiceDescriptor[descriptors.size()]);
  }

  /**
   * Creates the requested KerberosServiceDescriptor parsed from a JSON-formatted file.
   * <p/>
   * The file is expected to be formatted as follows:
   * <pre>
   * {
   *    "services" : [
   *      ... (zero or more service descriptor blocks) ...
   *    ]
   * }
   * </pre>
   * <p/>
   * Because of this one or more services may exist.  This method parses through the definitions to
   * return the a KerberosServiceDescriptor for the requested service
   *
   * @param file a JSON-formatted file containing this service-level descriptor data
   * @param name a String containing the nae of the desired service
   * @return a KerberosServiceDescriptor object or null if a descriptor for the named service is not
   * available
   * @throws java.io.FileNotFoundException if the specified File does not point to a valid file
   * @throws IOException                   if the specified File is not a readable file
   * @throws AmbariException               if the specified File does not contain valid JSON data
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  public KerberosServiceDescriptor createInstance(File file, String name) throws IOException {
    try {
      return createInstance(parseFile(file), name);
    } catch (AmbariException e) {
      throw new AmbariException(String.format("An error occurred processing the JSON-formatted file: %s", file.getAbsolutePath()), e);
    }
  }

  /**
   * Creates the requested KerberosServiceDescriptor parsed from a Map of data.
   * <p/>
   * The Map is expected to be formatted as follows:
   * <pre>
   * "services" => [
   *   ... (zero or more Maps containing service descriptor data) ...
   * ]
   * </pre>
   * <p/>
   * Because of this one or more services may exist.  This method parses through the definitions to
   * return the a KerberosServiceDescriptor for the requested service
   *
   * @param map  a Map containing this service-level descriptor data
   * @param name a String containing the nae of the desired service
   * @return a KerberosServiceDescriptor object or null if a descriptor for the named service is not
   * available
   * @throws org.apache.ambari.server.AmbariException if an error occurs while processing the Map
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  public KerberosServiceDescriptor createInstance(Map<String, Object> map, String name) throws AmbariException {
    KerberosServiceDescriptor descriptor = null;

    if ((map != null) && (name != null)) {
      Object servicesData = map.get("services");

      if (servicesData == null) {
        throw new AmbariException("Missing top-level \"services\" property in service-level Kerberos descriptor data");
      } else if (servicesData instanceof Collection) {
        for (Object serviceData : (Collection) servicesData) {
          if (serviceData instanceof Map) {
            Map<?, ?> serviceDataMap = (Map<?, ?>) serviceData;

            if (name.equalsIgnoreCase((String) serviceDataMap.get("name"))) {
              descriptor = new KerberosServiceDescriptor(serviceDataMap);
              break;
            }
          }
        }
      } else {
        throw new AmbariException(String.format("Unexpected top-level \"services\" type in service-level Kerberos descriptor data: %s",
            servicesData.getClass().getName()));
      }
    }

    return descriptor;
  }


  /**
   * Creates a new KerberosServiceDescriptor
   *
   * @param name a String declaring this service's name
   * @param json a JSON-formatted String containing this service's descriptor data
   * @throws AmbariException if an error occurs while parsing the JSON-formatted String
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  public KerberosServiceDescriptor createInstance(String name, String json) throws AmbariException {
    return new KerberosServiceDescriptor(name, parseJSON(json));
  }

  /**
   * Creates a new KerberosServiceDescriptor
   *
   * @param name a String declaring this service's name
   * @param map  a Map of values use to populate the data for the new instance
   * @throws AmbariException if an error occurs while parsing the JSON-formatted String
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  public KerberosServiceDescriptor createInstance(String name, Map<?, ?> map) throws AmbariException {
    return new KerberosServiceDescriptor(name, map);
  }
}
