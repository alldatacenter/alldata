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

package org.apache.ambari.server.controller.predicate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;


/**
 * Predicate which evaluates to true if all of the predicates in a predicate
 * array evaluate to true.
 */
public class AndPredicate extends ArrayPredicate {

  public AndPredicate(Predicate... predicates) {
    super(predicates);
  }

  @Override
  public Predicate create(Predicate... predicates) {
    return instance(predicates);
  }

  public static Predicate instance(Predicate... predicates) {
    List<Predicate> predicateList = new LinkedList<>();

    // Simplify the predicate array
    for (Predicate predicate : predicates) {
      if (!(predicate instanceof AlwaysPredicate)) {
        if (predicate instanceof AndPredicate) {
          predicateList.addAll(Arrays.asList(((AndPredicate) predicate).getPredicates()));
        }
        else {
          predicateList.add(predicate);
        }
      }
    }

    return predicateList.size() == 1 ?
        predicateList.get(0) :
        new AndPredicate(predicateList.toArray(new Predicate[predicateList.size()]));
  }

  @Override
  public boolean evaluate(Resource resource) {
    Predicate[] predicates = getPredicates();
    for (Predicate predicate : predicates) {
      if (!predicate.evaluate(resource)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getOperator() {
    return "AND";
  }
}
