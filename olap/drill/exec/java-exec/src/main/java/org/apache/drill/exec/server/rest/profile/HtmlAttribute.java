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
package org.apache.drill.exec.server.rest.profile;

/**
 * Define all attributes and values that can be injected by various Wrapper classes in org.apache.drill.exec.server.rest.*
 */
public class HtmlAttribute {
  //Attributes
  public static final String CLASS = "class";
  public static final String KEY = "key";
  public static final String DATA_ORDER = "data-order";
  public static final String TITLE = "title";
  public static final String SPILLS = "spills";
  public static final String STYLE = "style";

  //Values
  public static final String CLASS_VALUE_SPILL_TAG = "spill-tag";
  public static final String CLASS_VALUE_NO_PROGRESS_TAG = "no-progress-tag";
  public static final String CLASS_VALUE_TIME_SKEW_TAG = "time-skew-tag";
  public static final String CLASS_VALUE_SCAN_WAIT_TAG = "scan-wait-tag";
  public static final String CLASS_VALUE_EST_ROWS_ANCHOR = "estRowsAnchor";
  public static final String STYLE_VALUE_CURSOR_HELP = "cursor:help;";
}