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

import org.apache.drill.exec.proto.UserBitShared.MinorFragmentProfile;

import org.apache.drill.shaded.guava.com.google.common.base.Predicate;
import org.apache.drill.shaded.guava.com.google.common.base.Predicates;

interface Filters {
  final static Predicate<MinorFragmentProfile> hasOperators = new Predicate<MinorFragmentProfile>() {
    public boolean apply(MinorFragmentProfile arg0) {
      return arg0.getOperatorProfileCount() != 0;
    }
  };

  final static Predicate<MinorFragmentProfile> hasTimes = new Predicate<MinorFragmentProfile>() {
    public boolean apply(MinorFragmentProfile arg0) {
      return arg0.hasStartTime() && arg0.hasEndTime();
    }
  };

  final static Predicate<MinorFragmentProfile> hasOperatorsAndTimes = Predicates.and(Filters.hasOperators, Filters.hasTimes);

  final static Predicate<MinorFragmentProfile> missingOperatorsOrTimes = Predicates.not(hasOperatorsAndTimes);
}
