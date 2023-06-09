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
package org.apache.drill.exec.server.rest;

/**
 * Holds various constants used by WebServer components.
 */
public final class WebServerConstants {

  private WebServerConstants() {}

  public static final String REDIRECT_QUERY_PARM = "redirect";
  public static final String WEBSERVER_ROOT_PATH = "/";

  // Main Login page which help to choose between Form and Spnego authentication
  public static final String MAIN_LOGIN_RESOURCE_NAME = "mainLogin";
  public static final String MAIN_LOGIN_RESOURCE_PATH = WEBSERVER_ROOT_PATH + MAIN_LOGIN_RESOURCE_NAME;

  // Login page for FORM authentication
  public static final String FORM_LOGIN_RESOURCE_NAME = "login";
  public static final String FORM_LOGIN_RESOURCE_PATH = WEBSERVER_ROOT_PATH + FORM_LOGIN_RESOURCE_NAME;

  // Login page for SPNEGO authentication
  public static final String SPENGO_LOGIN_RESOURCE_NAME = "spnegoLogin";
  public static final String SPENGO_LOGIN_RESOURCE_PATH = WEBSERVER_ROOT_PATH + SPENGO_LOGIN_RESOURCE_NAME;

  // Logout page
  public static final String LOGOUT_RESOURCE_NAME = "logout";
  public static final String LOGOUT_RESOURCE_PATH = WEBSERVER_ROOT_PATH + LOGOUT_RESOURCE_NAME;

  // Name of the CSRF protection token attribute
  public static final String CSRF_TOKEN = "csrfToken";
}
