/**
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
package org.apache.ambari.server.state.repository;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

/**
 * Represents the data inside of the {@code stack_packages.json}.
 */
public class StackPackage {

  /**
   * The upgrade dependencies, if any. Will be {@code null} if none.
   */
  @SerializedName("upgrade-dependencies")
  public UpgradeDependencies upgradeDependencies;

  /**
   * The upgrade dependencies between services.
   */
  public static class UpgradeDependencies {
    Map<String, List<String>> dependencies;
  }

  /**
   * A deserializer which extracts the "upgrade-dependencies" key from the
   * stack_packages.json.
   */
  public static class UpgradeDependencyDeserializer
      implements JsonDeserializer<UpgradeDependencies> {
    /**
     * {@inheritDoc}
     */
    @Override
    public UpgradeDependencies deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
      Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
      Map<String, List<String>> data = context.deserialize(json, mapType);

      UpgradeDependencies upgradeDependencies = new UpgradeDependencies();
      upgradeDependencies.dependencies = data;
      return upgradeDependencies;
    }
  }
}
