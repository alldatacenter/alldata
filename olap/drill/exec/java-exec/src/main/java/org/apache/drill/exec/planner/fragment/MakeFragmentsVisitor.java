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
package org.apache.drill.exec.planner.fragment;

import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.Exchange;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.work.foreman.ForemanSetupException;

/**
 * Responsible for breaking a plan into its constituent Fragments.
 */
public class MakeFragmentsVisitor extends AbstractPhysicalVisitor<Fragment, Fragment, ForemanSetupException> {

  public final static MakeFragmentsVisitor INSTANCE = new MakeFragmentsVisitor();

  private MakeFragmentsVisitor() {
  }

  @Override
  public Fragment visitExchange(Exchange exchange, Fragment receivingFragment) throws ForemanSetupException {
    if (receivingFragment == null) {
      throw new ForemanSetupException("The simple fragmenter was called without a FragmentBuilder value. This will only happen if the initial call to SimpleFragmenter is by a" +
        " Exchange node.  This should never happen since an Exchange node should never be the root node of a plan.");
    }
    Fragment sendingFragment= getNextFragment();
    receivingFragment.addReceiveExchange(exchange, sendingFragment);
    sendingFragment.addSendExchange(exchange, receivingFragment);
    exchange.getChild().accept(this, sendingFragment);
    return sendingFragment;
  }

  @Override
  public Fragment visitOp(PhysicalOperator op, Fragment value)  throws ForemanSetupException{
    value = ensureBuilder(value);
    value.addOperator(op);
    for (PhysicalOperator child : op) {
      child.accept(this, value);
    }
    return value;
  }

  private Fragment ensureBuilder(Fragment currentFragment) throws ForemanSetupException{
    if (currentFragment != null) {
      return currentFragment;
    } else {
      return getNextFragment();
    }
  }

  public Fragment getNextFragment() {
    return new Fragment();
  }

}
