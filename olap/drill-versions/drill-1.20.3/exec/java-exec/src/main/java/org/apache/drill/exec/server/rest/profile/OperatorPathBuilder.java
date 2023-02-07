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

import org.apache.drill.exec.proto.UserBitShared.MajorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.MinorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;

public class OperatorPathBuilder {
  private static final String OPERATOR_PATH_PATTERN = "%s-%s-%s";
  private static final String DEFAULT = "xx";
  private String major;
  private String minor;
  private String operator;

  public OperatorPathBuilder() {
    clear();
  }

  public void clear() {
    major = DEFAULT;
    minor = DEFAULT;
    operator = DEFAULT;
  }

  // Utility to left pad strings
  private String leftPad(final String text) {
    final int length = text.length();
    if (length > 2) {
      return text;
    } else {
      return String.format("00%s", text).substring(length);
    }
  }

  public OperatorPathBuilder setMajor(MajorFragmentProfile major) {
    if (major != null) {
      return setMajor(major.getMajorFragmentId());
    }
    return this;
  }

  public OperatorPathBuilder setMajor(int newMajor) {
    major = leftPad(String.valueOf(newMajor));
    return this;
  }

  public OperatorPathBuilder setMinor(MinorFragmentProfile minor) {
    if (minor != null) {
      return setMinor(minor.getMinorFragmentId());
    }
    return this;
  }

  public OperatorPathBuilder setMinor(int newMinor) {
    minor = leftPad(String.valueOf(newMinor));
    return this;
  }

  public OperatorPathBuilder setOperator(OperatorProfile op) {
    if (op != null) {
      return setOperator(op.getOperatorId());
    }
    return this;
  }

  public OperatorPathBuilder setOperator(int newOp) {
    operator = leftPad(String.valueOf(newOp));
    return this;
  }

  public String build() {
    StringBuffer sb = new StringBuffer();
    return sb.append(major).append("-").append(minor).append("-").append(operator).toString();
  }
}