/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.testing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.io.IOException;

public class InjectionSite {
  private final Class<?> clazz;
  private final String desc;

  public InjectionSite(final Class<?> clazz, final String desc) {
    Preconditions.checkNotNull(clazz);
    Preconditions.checkNotNull(desc);

    this.clazz = clazz;
    this.desc = desc;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (this == o) {
      return true;
    }

    if (!(o instanceof InjectionSite)) {
      return false;
    }

    final InjectionSite other = (InjectionSite) o;
    if (clazz != other.clazz) {
      return false;
    }

    if (!desc.equals(other.desc)) {
      return false;
    }

    return true;
  }

  private static final String SEPARATOR = ",";

  @Override
  public String toString() {
    return clazz.getName() + SEPARATOR + desc;
  }

  @Override
  public int hashCode() {
    return (clazz.hashCode() + 13) ^ (1 - desc.hashCode());
  }

  /**
   * Key Deserializer for InjectionSite.
   * Since JSON object keys must be strings, deserialize from a string.
   */
  public static class InjectionSiteKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(final String key, final DeserializationContext context)
      throws IOException, JsonProcessingException {
      final String[] fields = key.split(SEPARATOR);
      final Class<?> siteClass;
      try {
        siteClass = Class.forName(fields[0]);
      } catch (ClassNotFoundException e) {
        throw new IOException("Class " + fields[0] + " not found.", e);
      }
      return new InjectionSite(siteClass, fields[1]);
    }
  }
}
