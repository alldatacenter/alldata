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
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.AmbariException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * AbstractKerberosDescriptorFactory is an abstract class containing common functionality for
 * Kerberos descriptor factory classes.
 */
abstract class AbstractKerberosDescriptorFactory {

  /**
   * Parses a file containing JSON-formatted text into a (generic) Map.
   *
   * @param file a File containing the JSON-formatted text to parse
   * @return a Map of the data
   * @throws java.io.FileNotFoundException            if the specified File does not point to a valid file
   * @throws java.io.IOException                      if the specified File is not a readable file
   * @throws org.apache.ambari.server.AmbariException if the specified File does not contain valid JSON data
   */
  protected Map<String, Object> parseFile(File file) throws IOException {
    if (file == null) {
      return Collections.emptyMap();
    } else if (!file.isFile() || !file.canRead()) {
      throw new IOException(String.format("%s is not a readable file", file.getAbsolutePath()));
    } else {
      try {
        return new Gson().fromJson(new FileReader(file),
            new TypeToken<Map<String, Object>>() {
            }.getType());
      } catch (JsonSyntaxException e) {
        throw new AmbariException(String.format("Failed to parse JSON-formatted file: %s", file.getAbsolutePath()), e);
      }
    }
  }

  /**
   * Parses a JSON-formatted String into a (generic) Map.
   *
   * @param json a String containing the JSON-formatted text to parse
   * @return a Map of the data
   * @throws AmbariException if an error occurs while parsing the JSON-formatted String
   */
  protected Map<String, Object> parseJSON(String json) throws AmbariException {
    if ((json == null) || json.isEmpty()) {
      return Collections.emptyMap();
    } else {
      try {
        return new Gson().fromJson(json,
            new TypeToken<Map<String, Object>>() {
            }.getType());
      } catch (JsonSyntaxException e) {
        throw new AmbariException("Failed to parse JSON-formatted string", e);
      }
    }
  }
}
