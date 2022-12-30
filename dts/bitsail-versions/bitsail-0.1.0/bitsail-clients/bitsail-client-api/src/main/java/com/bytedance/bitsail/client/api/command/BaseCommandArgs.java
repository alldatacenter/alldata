/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.client.api.command;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Created 2022/8/5
 */
@Getter
@Setter
public class BaseCommandArgs implements CommandArgs {

  private String mainAction;

  @Parameter(names = {"-d", "--detach"})
  private boolean detach = false;

  @DynamicParameter(names = {"-p", "--props"})
  private Map<String, String> properties = Maps.newLinkedHashMap();

  @Parameter(names = {"--engine"},
      description = "BitSail Runtime engine, eg: flink")
  private String engineName = "flink";

  @Parameter(names = {"--conf"})
  private String jobConf;

  @Parameter(names = {"--enable-kerberos"})
  private boolean enableKerberos = false;

  @Parameter(names = {"--keytab-path"})
  private String keytabPath;

  @Parameter(names = {"--principal"})
  private String principal;

  @Parameter(names = {"--krb5-conf-path"})
  private String krb5ConfPath;

  private String[] unknownOptions;
}
