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
package org.apache.drill.exec.testing;

import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The base class for all types of injections (currently, pause and exception).
 */
public abstract class Injection {

  private static final Logger logger = LoggerFactory.getLogger(Injection.class);

  protected final String address;  // the address of the drillbit on which to inject
  protected final int port; // user port of the drillbit; useful when there are multiple drillbits on same machine
  protected final Class<?> siteClass; // the class where the injection should happen
  protected final String desc; // description of the injection site; useful for multiple exception injections in a single class
  private final AtomicInteger nSkip; // the number of times to skip the injection; starts >= 0
  private final AtomicInteger nFire;  // the number of times to do the injection, after any skips; starts > 0
  private final long msPause; // duration of the injection (only applies to pause injections)

  protected Injection(final String address, final int port, final String siteClass, final String desc,
                      final int nSkip, final int nFire, final long msPause) throws InjectionConfigurationException {
    if (desc == null || desc.isEmpty()) {
      throw new InjectionConfigurationException("Injection desc is null or empty.");
    }

    if (nSkip < 0) {
      throw new InjectionConfigurationException("Injection nSkip is not non-negative.");
    }

    if (nFire <= 0) {
      throw new InjectionConfigurationException("Injection nFire is non-positive.");
    }
    try {
      this.siteClass = Class.forName(siteClass);
    } catch (ClassNotFoundException e) {
      throw new InjectionConfigurationException("Injection siteClass not found.", e);
    }

    this.address = address;
    this.port = port;
    this.desc = desc;
    this.nSkip = new AtomicInteger(nSkip);
    this.nFire = new AtomicInteger(nFire);
    this.msPause = msPause;
  }

  /**
   * This function checks if it is the right time for the injection to happen.
   *
   * @return if the injection should be injected now
   */
  protected boolean injectNow() {
    if(logger.isDebugEnabled()) {
      logger.debug(toString());
    }
    return nSkip.decrementAndGet() < 0 && nFire.decrementAndGet() >= 0;
  }

  public String getDesc() {
    return desc;
  }

  public Class<?> getSiteClass() {
    return siteClass;
  }

  // If the address is null, the injection must happen on every drillbit that reaches the specified site.
  public final boolean isValidForBit(final DrillbitEndpoint endpoint) {
    return address == null ||
      (address.equals(endpoint.getAddress()) && port == endpoint.getUserPort());
  }

  public long getMsPause() {
    return msPause;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Injection.class.getSimpleName() + "[", "]")
      .add("address='" + address + "'")
      .add("port=" + port)
      .add("siteClass=" + siteClass)
      .add("desc='" + desc + "'")
      .add("nSkip=" + nSkip)
      .add("nFire=" + nFire)
      .add("msPause=" + msPause)
      .toString();
  }
}
