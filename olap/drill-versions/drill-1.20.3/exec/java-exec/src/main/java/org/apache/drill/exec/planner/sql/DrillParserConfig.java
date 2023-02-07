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
package org.apache.drill.exec.planner.sql;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserImplFactory;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.sql.parser.impl.DrillParserWithCompoundIdConverter;

public class DrillParserConfig implements SqlParser.Config {

  private final long identifierMaxLength;
  private final Quoting quotingIdentifiers;
  public final static SqlConformance DRILL_CONFORMANCE = new DrillConformance();

  public DrillParserConfig(PlannerSettings settings) {
    identifierMaxLength = settings.getIdentifierMaxLength();
    quotingIdentifiers = settings.getQuotingIdentifiers();
  }

  @Override
  public int identifierMaxLength() {
    return (int) identifierMaxLength;
  }

  @Override
  public Casing quotedCasing() {
    return Casing.UNCHANGED;
  }

  @Override
  public Casing unquotedCasing() {
    return Casing.UNCHANGED;
  }

  @Override
  public Quoting quoting() {
    return quotingIdentifiers;
  }

  @Override
  public boolean caseSensitive() {
    return false;
  }

  @Override
  public SqlConformance conformance() {
    return DRILL_CONFORMANCE;
  }

  @Override
  public boolean allowBangEqual() {
    return conformance().isBangEqualAllowed();
  }

  @Override
  public SqlParserImplFactory parserFactory() {
    return DrillParserWithCompoundIdConverter.FACTORY;
  }

}