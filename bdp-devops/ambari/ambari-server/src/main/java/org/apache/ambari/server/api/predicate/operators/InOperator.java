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
package org.apache.ambari.server.api.predicate.operators;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.Predicate;

/**
 * IN relational operator.
 * This is a binary operator which takes a comma delimited right operand and
 * creates equals predicates with the left operand and each right operand token.
 * The equals predicates are combined with an OR predicate.
 *
 */
public class InOperator extends AbstractOperator implements RelationalOperator {

  public InOperator() {
    super(0);
  }

  @Override
  public String getName() {
    return "InOperator";
  }

  @Override
  public Predicate toPredicate(String prop, String val) throws InvalidQueryException {

    if (val == null) {
      throw new InvalidQueryException("IN operator is missing a required right operand for property " + prop);
    }

    String[] tokens = val.split(",");
    List<EqualsPredicate> listPredicates = new ArrayList<>();
    for (String token : tokens) {
      listPredicates.add(new EqualsPredicate<>(prop, token.trim()));
    }
    return listPredicates.size() == 1 ? listPredicates.get(0) :
        buildOrPredicate(listPredicates);
  }

  private OrPredicate buildOrPredicate(List<EqualsPredicate> listPredicates) {
    return new OrPredicate(listPredicates.toArray(new Predicate[listPredicates.size()]));
  }

  @Override
  public TYPE getType() {
    return TYPE.IN;
  }
}
