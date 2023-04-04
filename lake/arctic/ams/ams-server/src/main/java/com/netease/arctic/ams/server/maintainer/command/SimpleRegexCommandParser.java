/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleRegexCommandParser implements CommandParser {

  private static final String ANALYZE = "ANALYZE";
  private static final String REPAIR = "REPAIR";
  private static final String THROUGH = "THROUGH";
  private static final String USE = "USE";
  private static final String OPTIMIZE = "OPTIMIZE";
  private static final String REFRESH = "REFRESH";
  private static final String FILE_CACHE = "FILE_CACHE";
  private static final String SHOW = "SHOW";
  private static final String TABLE = "TABLE";

  private static final String PROPERTY = "PROPERTY";
  private static final String ANALYZE_EXCEPTION_MESSAGE =
      "Please check if your command is correct! Pattern: ANALYZE ${table_name}";
  private static final String REPAIR_EXCEPTION_MESSAGE =
      "Please check if your command is correct! " +
          "Pattern: REPAIR ${table_name} THROUGH " +
          "[ FIND_BACK | SYNC_METADATA | ROLLBACK ${snapshot_id} | DROP_TABLE ]";
  private static final String USE_EXCEPTION_MESSAGE =
      "Please check if your command is correct! " +
          "Pattern: USE [ ${catalog_name} | ${database_name}  ]";
  private static final String OPTIMIZE_EXCEPTION_MESSAGE =
      "Please check if your command is correct! " +
          "Pattern: OPTIMIZE [ STOP | START ] ${table_name}";
  private static final String TABLE_EXCEPTION_MESSAGE =
      "Please check if your command is correct! " +
          "Pattern: TABLE ${table_name} REFRESH";
  private static final String SHOW_EXCEPTION_MESSAGE =
      "Please check if your command is correct! " +
          "Pattern: SHOW [ CATALOGS | DATABASES | TABLES ]";

  private static final String PROPERTY_EXCEPTION_MESSAGE =
      "Please check if your command is correct! " +
          "Pattern: PROPERTY [ GET xxx | SET xxx xxx]";


  private CallFactory callFactory;

  public SimpleRegexCommandParser(CallFactory callFactory) {
    this.callFactory = callFactory;
  }

  @Override
  public CallCommand parse(String line) throws IllegalCommandException {
    line = line.trim();
    if (line.endsWith(";")) {
      line = line.replace(";", "");
    }
    String[] commandSplit = line.trim().split("\\s+");
    if (commandSplit.length < 2) {
      return callFactory.generateHelpCall();
    }

    switch (commandSplit[0].toUpperCase()) {
      case ANALYZE:
        if (commandSplit.length != 2) {
          throw new IllegalCommandException(ANALYZE_EXCEPTION_MESSAGE);
        }
        return callFactory.generateAnalyzeCall(commandSplit[1]);
      case REPAIR:
        if (commandSplit.length > 5) {
          throw new IllegalCommandException(REPAIR_EXCEPTION_MESSAGE);
        }
        if (commandSplit.length < 4 || !StringUtils.equalsIgnoreCase(commandSplit[2], THROUGH)) {
          throw new IllegalCommandException(REPAIR_EXCEPTION_MESSAGE);
        }
        if (StringUtils.equalsIgnoreCase(commandSplit[3], RepairWay.ROLLBACK.name())) {
          Long snapshot = commandSplit.length != 5 ? null : Long.parseLong(commandSplit[4]);
          return callFactory.generateRepairCall(commandSplit[1], RepairWay.ROLLBACK, snapshot);
        } else {
          if (commandSplit.length != 4) {
            throw new IllegalCommandException(REPAIR_EXCEPTION_MESSAGE);
          }
          RepairWay repairWay;
          try {
            repairWay = RepairWay.valueOf(commandSplit[3].toUpperCase());
          } catch (IllegalArgumentException e) {
            throw new IllegalCommandException(REPAIR_EXCEPTION_MESSAGE);
          }
          return callFactory.generateRepairCall(commandSplit[1], repairWay, null);
        }
      case USE:
        if (commandSplit.length != 2 || commandSplit[1].split("\\.").length > 2) {
          throw new IllegalCommandException(USE_EXCEPTION_MESSAGE);
        }
        return callFactory.generateUseCall(commandSplit[1]);
      case OPTIMIZE:
        if (commandSplit.length != 3) {
          throw new IllegalCommandException(OPTIMIZE_EXCEPTION_MESSAGE);
        }
        OptimizeCall.Action optimizeAction;
        try {
          optimizeAction = OptimizeCall.Action.valueOf(commandSplit[1].toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new IllegalCommandException(OPTIMIZE_EXCEPTION_MESSAGE);
        }
        return callFactory.generateOptimizeCall(optimizeAction, commandSplit[2]);
      case TABLE:
        if (commandSplit.length == 3) {
          TableCall.TableOperation tableOperation = TableCall.TableOperation.valueOf(commandSplit[2].toUpperCase());
          return callFactory.generateTableCall(commandSplit[1], tableOperation);
        } else {
          throw new IllegalCommandException(TABLE_EXCEPTION_MESSAGE);
        }
      case SHOW:
        if (commandSplit.length != 2) {
          throw new IllegalCommandException(SHOW_EXCEPTION_MESSAGE);
        }
        ShowCall.Namespaces namespaces;
        try {
          namespaces = ShowCall.Namespaces.valueOf(commandSplit[1].toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new IllegalCommandException(SHOW_EXCEPTION_MESSAGE);
        }
        return callFactory.generateShowCall(namespaces);
      case PROPERTY:
        PropertyCall.PropertyOperate propertyOperate;
        try {
          propertyOperate = PropertyCall.PropertyOperate.valueOf(commandSplit[1].toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new IllegalCommandException(SHOW_EXCEPTION_MESSAGE);
        }

        //set
        if (propertyOperate == PropertyCall.PropertyOperate.SET) {
          if (commandSplit.length != 4) {
            throw new IllegalCommandException(PROPERTY_EXCEPTION_MESSAGE);
          }
          return callFactory.generatePropertyCall(propertyOperate, commandSplit[2], commandSplit[3]);
        }

        //get
        if (commandSplit.length != 3) {
          throw new IllegalCommandException(PROPERTY_EXCEPTION_MESSAGE);
        }
        return callFactory.generatePropertyCall(propertyOperate, commandSplit[2], null);

    }
    return callFactory.generateHelpCall();
  }

  @Override
  public String[] keywords() {
    List<String> keywordUpper = new ArrayList<>();
    keywordUpper.addAll(Arrays.asList(
        ANALYZE,
        REPAIR,
        THROUGH,
        USE,
        OPTIMIZE,
        REFRESH,
        FILE_CACHE,
        SHOW,
        TABLE));

    keywordUpper.addAll(Arrays.stream(OptimizeCall.Action.values()).map(Enum::name).collect(Collectors.toList()));
    keywordUpper.addAll(Arrays.stream(RepairWay.values()).map(Enum::name).collect(Collectors.toList()));
    keywordUpper.addAll(Arrays.stream(ShowCall.Namespaces.values()).map(Enum::name).collect(Collectors.toList()));
    keywordUpper.addAll(Arrays.stream(TableCall.TableOperation.values()).map(Enum::name).collect(Collectors.toList()));

    List<String> all =
        keywordUpper.stream().flatMap(s -> Stream.of(s, s.toLowerCase())).collect(Collectors.toList());

    return all.toArray(new String[0]);
  }
}
