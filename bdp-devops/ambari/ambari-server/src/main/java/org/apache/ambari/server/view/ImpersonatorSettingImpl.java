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
package org.apache.ambari.server.view;

import org.apache.ambari.view.ImpersonatorSetting;
import org.apache.ambari.view.ViewContext;

/**
 * Class that provides default values for impersonating, such as the username and doAs parameter name.
 */
public class ImpersonatorSettingImpl implements ImpersonatorSetting {
  private String doAsParamName;
  private String username;

  public static final String DEFAULT_DO_AS_PARAM = "doAs";

  /**
   * This constructor will use DEFAULT_DO_AS_PARAM and currently logged on user.
   * @param context View context object that will provide the currently logged on user.
   */
  public ImpersonatorSettingImpl(ViewContext context) {
    // Default values
    this.username = context.getUsername();
    this.doAsParamName = DEFAULT_DO_AS_PARAM;
  }

  /**
   * @param context View context object that will provide the currently logged on user.
   * @param doAsParamName The parameter name used for "doAs" impersonation.
   */
  public ImpersonatorSettingImpl(ViewContext context, String doAsParamName) {
    this.username = context.getUsername();
    this.doAsParamName = doAsParamName;
  }

  /**
   * @param username Username value that will be used for "doAs" impersonation.
   * @param doAsParamName The parameter name used for "doAs" impersonation.
   */
  public ImpersonatorSettingImpl(String username, String doAsParamName) {
    this.username = username;
    this.doAsParamName = doAsParamName;
  }

  /**
   * @return The parameter name used for "doAs" impersonation.
   */
  @Override
  public String getDoAsParamName() { return this.doAsParamName; }

  /**
   * @return The username value that will be used for "doAs" impersonation.
   */
  @Override
  public String getUsername() { return this.username; }
}
