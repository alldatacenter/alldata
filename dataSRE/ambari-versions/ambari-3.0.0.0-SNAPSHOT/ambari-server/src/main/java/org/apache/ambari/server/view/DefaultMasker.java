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
package org.apache.ambari.server.view;

import java.io.UnsupportedEncodingException;

import org.apache.ambari.view.MaskException;
import org.apache.ambari.view.Masker;
import org.apache.commons.codec.binary.Base64;

/**
 * Provides simple masking of view parameters.
 */
public class DefaultMasker implements Masker {

  @Override
  public String mask(String value) throws MaskException {
    try {
      return Base64.encodeBase64String(value.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new MaskException("UTF-8 is not supported", e);
    }
  }

  @Override
  public String unmask(String value) throws MaskException {
    try {
      return new String(Base64.decodeBase64(value), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new MaskException("UTF-8 is not supported", e);
    }
  }
}
