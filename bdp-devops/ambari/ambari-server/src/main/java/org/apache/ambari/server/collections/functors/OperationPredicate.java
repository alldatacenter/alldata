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

package org.apache.ambari.server.collections.functors;

import org.apache.ambari.server.collections.Predicate;
import org.apache.commons.collections.Transformer;

/**
 * OperationPredicate is an abstract class providing functionality of transforming the input context
 * before executing the implementation-specific
 * {@link org.apache.commons.collections.Predicate#evaluate(Object)} method.
 */
abstract class OperationPredicate extends Predicate {
  /**
   * The {@link Transformer} to use to transform the data before evaluation
   */
  private ContextTransformer transformer = null;

  /**
   * Constructor
   *
   * @param name        the name of this {@link Predicate}
   * @param transformer the {@link Transformer} to use to transform the data before evaluation
   */
  OperationPredicate(String name, ContextTransformer transformer) {
    super(name);
    this.transformer = transformer;
  }

  /**
   * Gets the {@link Transformer}
   *
   * @return the assigned {@link ContextTransformer}
   */
  public ContextTransformer getTransformer() {
    return transformer;
  }

  /**
   * Sets the {@link Transformer}
   *
   * @param transformer a {@link ContextTransformer}
   */
  public void setTransformer(ContextTransformer transformer) {
    this.transformer = transformer;
  }


  /**
   * Gets the context key assigned to the {@link ContextTransformer}.
   * <p>
   * This key is used to identify which value from the context a passed to the
   * {@link org.apache.commons.collections.Predicate#evaluate(Object)} method
   *
   * @return a key name
   */
  public String getContextKey() {
    return (this.transformer == null) ? null : this.transformer.getKey();
  }

  @Override
  public boolean evaluate(Object o) {
    Object data = (transformer == null) ? o : transformer.transform(o);
    return evaluateTransformedData(data);
  }

  @Override
  public int hashCode() {
    return super.hashCode() + (37 * ((transformer == null) ? 0 : transformer.hashCode()));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj == null) {
      return false;
    } else if (super.equals(obj) && (obj instanceof OperationPredicate) && (hashCode() == obj.hashCode())) {
      OperationPredicate p = (OperationPredicate) obj;
      return (transformer == null) ? (p.transformer == null) : transformer.equals(p.transformer);
    } else {
      return false;
    }
  }

  /**
   * Abstract method used internally to process the {@link #evaluate(Object)} logic after the
   * input data is transformed.
   *
   * @param data the transformed data to use
   * @return the result of the evaluation (<code>true</code>, <code>false</code>)
   * @see org.apache.commons.collections.Predicate#evaluate(Object)
   */
  protected abstract boolean evaluateTransformedData(Object data);

}
