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

package org.apache.ambari.server.controller.internal;

/**
 * Various strategies for trimming properties during blueprint deployment.
 */
public enum TrimmingStrategy {

  DEFAULT {
    @Override
    public String trim(String stringToTrim) {
      return stringToTrim.trim();
    }
  },

  DIRECTORIES {
    @Override
    public String trim(String stringToTrim) {
      return stringToTrim.replaceAll("\\s*,+\\s*", ",").trim();
    }
  },

  PASSWORD {
    @Override
    public String trim(String stringToTrim) {
      return stringToTrim;
    }
  },

  DELETE_SPACES_AT_END {
    @Override
    public String trim(String stringToTrim) {
      if (" ".equals(stringToTrim)) {
        return stringToTrim;
      }
      return stringToTrim.replaceAll("\\s+$", "");
    }
  }
  ;

  public abstract String trim(String stringToTrim);

}

