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
package org.apache.drill.exec.rpc;

/**
 * Context to help initializing encryption related configurations for a connection.
 * <ul>
 * <li>encryptionEnabled  - identifies if encryption is required or not </li>
 * <li>maxWrappedSize     - maximum size of the encoded packet that is sent over wire.
 *                          Recommended Maximum value is {@link RpcConstants#MAX_RECOMMENDED_WRAPPED_SIZE}</li>
 * <li>wrapSizeLimit      - Maximum size of plain buffer to be send to wrap call which will produce encrypted buffer
 *                          <= maxWrappedSize. Get's set after SASL negotiation.</li>
 *</ul>
 */
public class EncryptionContextImpl implements EncryptionContext {
  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EncryptionContextImpl.class);

  private boolean encryptionEnabled;

  private int maxWrappedSize;

  private int wrapSizeLimit;

  EncryptionContextImpl() {
    this.encryptionEnabled = false;
    this.maxWrappedSize = 65536;
    this.wrapSizeLimit = 0;
  }

  EncryptionContextImpl(EncryptionContext context) {
    this.encryptionEnabled = context.isEncryptionEnabled();
    this.maxWrappedSize = context.getMaxWrappedSize();
    this.wrapSizeLimit = context.getWrapSizeLimit();
  }

  @Override
  public boolean isEncryptionEnabled() {
    return encryptionEnabled;
  }

  @Override
  public void setEncryption(boolean encryptionEnabled) {
    this.encryptionEnabled = encryptionEnabled;
  }

  @Override
  public int getMaxWrappedSize() {
    return maxWrappedSize;
  }

  @Override
  public void setMaxWrappedSize(int maxWrappedSize) {
    this.maxWrappedSize = maxWrappedSize;
  }

  @Override
  public String getEncryptionCtxtString() {
    return toString();
  }

  @Override
  public void setWrapSizeLimit(int wrapSizeLimit) {
    this.wrapSizeLimit = wrapSizeLimit;
  }

  @Override
  public int getWrapSizeLimit() {
    return wrapSizeLimit;
  }

  private String getEncryptionString() {
    return encryptionEnabled ? "enabled" : "disabled";
  }

  @Override
  public String toString() {
    return ("Encryption: " + getEncryptionString() + " , MaxWrappedSize: " + maxWrappedSize + " , " +
        "WrapSizeLimit: " + wrapSizeLimit).intern();
  }
}