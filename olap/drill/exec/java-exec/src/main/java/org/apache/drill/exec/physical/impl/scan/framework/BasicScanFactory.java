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
package org.apache.drill.exec.physical.impl.scan.framework;

import java.util.Iterator;

import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ReaderFactory;

/**
 * Basic reader builder for simple non-file readers. Includes only
 * schema negotiation, but no implicit columns. Readers are assumed
 * to be created ahead of time and passed into the framework
 * in the constructor.
 * <p>
 * This form is designed to simplify conversion of existing readers.
 * While it provides a simple first step, readers should perform
 * further conversion to create readers on the fly rather than
 * up front.
 */

public class BasicScanFactory implements ReaderFactory {

  private final Iterator<ManagedReader<SchemaNegotiator>> iterator;

  public BasicScanFactory(Iterator<ManagedReader<SchemaNegotiator>> iterator) {
    this.iterator = iterator;
  }

  @Override
  public void bind(ManagedScanFramework framework) { }

  @Override
  public ManagedReader<? extends SchemaNegotiator> next() {
    if (! iterator.hasNext()) {
      return null;
    }
    return iterator.next();
  }
}
