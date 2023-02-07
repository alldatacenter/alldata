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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.Exchange;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.work.foreman.ForemanSetupException;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class Fragment implements Iterable<Fragment.ExchangeFragmentPair> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Fragment.class);

  private PhysicalOperator root;
  private ExchangeFragmentPair sendingExchange;
  private final List<ExchangeFragmentPair> receivingExchangePairs = Lists.newLinkedList();

  /**
   * Set the given operator as root operator of this fragment. If root operator is already set,
   * then this method call is a no-op.
   * @param o new root operator
   */
  public void addOperator(PhysicalOperator o) {
    if (root == null) {
      root = o;
    }
    // TODO should complain otherwise
  }

  public void addSendExchange(Exchange e, Fragment sendingToFragment) throws ForemanSetupException{
    if (sendingExchange != null) {
      throw new ForemanSetupException("Fragment was trying to add a second SendExchange.  ");
    }
    addOperator(e);
    sendingExchange = new ExchangeFragmentPair(e, sendingToFragment);
  }

  public void addReceiveExchange(Exchange e, Fragment fragment) {
    this.receivingExchangePairs.add(new ExchangeFragmentPair(e, fragment));
  }

  @Override
  public Iterator<ExchangeFragmentPair> iterator() {
    return this.receivingExchangePairs.iterator();
  }

  public List<ExchangeFragmentPair> getReceivingExchangePairs() {
    return receivingExchangePairs;
  }

  public PhysicalOperator getRoot() {
    return root;
  }

  public Exchange getSendingExchange() {
    if (sendingExchange != null) {
      return sendingExchange.exchange;
    }

    return null;
  }

  public ExchangeFragmentPair getSendingExchangePair() {
    return sendingExchange;
  }

  public class ExchangeFragmentPair {
    private Exchange exchange;
    private Fragment fragmentXchgTo;

    public ExchangeFragmentPair(Exchange exchange, Fragment fragXchgTo) {
      super();
      this.exchange = exchange;
      this.fragmentXchgTo = fragXchgTo;
    }

    public Exchange getExchange() {
      return exchange;
    }

    public Fragment getNode() {
      return fragmentXchgTo;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((exchange == null) ? 0 : exchange.hashCode());
      result = prime * result + ((fragmentXchgTo == null) ? 0 : fragmentXchgTo.hashCode());
      return result;
    }

    @Override
    public String toString() {
      return "ExchangeFragmentPair [exchange=" + exchange + "]";
    }

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((receivingExchangePairs == null) ? 0 : receivingExchangePairs.hashCode());
    result = prime * result + ((root == null) ? 0 : root.hashCode());
    result = prime * result + ((sendingExchange == null) ? 0 : sendingExchange.getExchange().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Fragment other = (Fragment) obj;
    if (receivingExchangePairs == null) {
      if (other.receivingExchangePairs != null) {
        return false;
      }
    } else if (!receivingExchangePairs.equals(other.receivingExchangePairs)) {
      return false;
    }
    if (root == null) {
      if (other.root != null) {
        return false;
      }
    } else if (!root.equals(other.root)) {
      return false;
    }
    if (sendingExchange == null) {
      if (other.sendingExchange != null) {
        return false;
      }
    } else if (!sendingExchange.equals(other.sendingExchange)) {
      return false;
    }

    return true;
  }

  public List<PhysicalOperator> getBufferedOperators(QueryContext queryContext) {
    List<PhysicalOperator> bufferedOps = new ArrayList<>();
    root.accept(new BufferedOpFinder(queryContext), bufferedOps);
    return bufferedOps;
  }

  protected static class BufferedOpFinder extends AbstractPhysicalVisitor<Void, List<PhysicalOperator>, RuntimeException> {
    private final QueryContext context;

    public BufferedOpFinder(QueryContext queryContext) {
      this.context = queryContext;
    }

    @Override
    public Void visitOp(PhysicalOperator op, List<PhysicalOperator> value)
      throws RuntimeException {
      if (op.isBufferedOperator(context)) {
        value.add(op);
      }
      visitChildren(op, value);
      return null;
    }
  }

  @Override
  public String toString() {
    return "FragmentNode [root=" + root + ", sendingExchange=" + sendingExchange + ", receivingExchangePairs="
        + receivingExchangePairs + "]";
  }
}
