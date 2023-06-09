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
package org.apache.drill.exec.udf.mapr.db;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

/**
 * This is a placeholder for the notMatches() function.
 *
 * At this time, this function can only be used in predicates. The placeholder
 * is here to prevent calcite from complaining; the function will get pushed down
 * by the storage plug-in into DB. That process will go through JsonConditionBuilder.java,
 * which will replace this function with the real OJAI equivalent to be pushed down.
 * Therefore, there's no implementation here.
 */
@FunctionTemplate(name = "ojai_notmatches",
                scope = FunctionTemplate.FunctionScope.SIMPLE,
                nulls = FunctionTemplate.NullHandling.INTERNAL)
public class NotMatchesPlaceholder implements DrillSimpleFunc {

        @Param BigIntHolder /*FieldReader*/ field;
        @Param(constant = true) VarCharHolder pattern;

        @Output BitHolder output;

        public void setup() {
        }

        public void eval() {
        }

}
