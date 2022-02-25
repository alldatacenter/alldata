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
package org.apache.ambari.view;

/**
 * Interface that provides default values for impersonating, such as the username and doAs parameter name.
 *
 * @deprecated  As of release 2.0, replaced by
 *              {@link URLStreamProvider#readAs(String, String, String, java.util.Map, String)}
 */
@Deprecated
public interface ImpersonatorSetting {

  /**
   * @return The parameter name used for "doAs" impersonation.
   */
  public String getDoAsParamName();

  /**
   * @return The username value that will be used for "doAs" impersonation.
   */
  public String getUsername();
}
