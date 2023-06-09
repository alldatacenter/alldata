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
package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.holders.NullableVarBinaryHolder;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

public class VarHelpers {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VarHelpers.class);

  public static final int compare(VarBinaryHolder left, VarCharHolder right) {
    for (int l = left.start, r = right.start; l < left.end && r < right.end; l++, r++) {
      byte leftByte = left.buffer.getByte(l);
      byte rightByte = right.buffer.getByte(r);
      if (leftByte != rightByte) {
        return ((leftByte & 0xFF) - (rightByte & 0xFF)) > 0 ? 1 : -1;
      }
    }

    int l = (left.end - left.start) - (right.end - right.start);
    if (l > 0) {
      return 1;
    } else if (l == 0) {
      return 0;
    } else {
      return -1;
    }

  }

  public static final int compare(VarCharHolder left, VarCharHolder right) {
    for (int l = left.start, r = right.start; l < left.end && r < right.end; l++, r++) {
      byte leftByte = left.buffer.getByte(l);
      byte rightByte = right.buffer.getByte(r);
      if (leftByte != rightByte) {
        return ((leftByte & 0xFF) - (rightByte & 0xFF)) > 0 ? 1 : -1;
      }
    }

    int l = (left.end - left.start) - (right.end - right.start);
    if (l > 0) {
      return 1;
    } else if (l == 0) {
      return 0;
    } else {
      return -1;
    }

  }

  public static final int compare(NullableVarBinaryHolder left, NullableVarBinaryHolder right) {
    if (left.isSet == 0) {
      if (right.isSet == 0) {
        return 0;
      }
      return -1;
    } else if (right.isSet == 0) {
      return 1;
    }

    for (int l = left.start, r = right.start; l < left.end && r < right.end; l++, r++) {
      byte leftByte = left.buffer.getByte(l);
      byte rightByte = right.buffer.getByte(r);
      if (leftByte != rightByte) {
        return ((leftByte & 0xFF) - (rightByte & 0xFF)) > 0 ? 1 : -1;
      }
    }

    int l = (left.end - left.start) - (right.end - right.start);
    if (l > 0) {
      return 1;
    } else if (l == 0) {
      return 0;
    } else {
      return -1;
    }

  }

  public static final int compare(NullableVarBinaryHolder left, NullableVarCharHolder right) {
    if (left.isSet == 0) {
      if (right.isSet == 0) {
        return 0;
      }
      return -1;
    } else if (right.isSet == 0) {
      return 1;
    }

    for (int l = left.start, r = right.start; l < left.end && r < right.end; l++, r++) {
      byte leftByte = left.buffer.getByte(l);
      byte rightByte = right.buffer.getByte(r);
      if (leftByte != rightByte) {
        return ((leftByte & 0xFF) - (rightByte & 0xFF)) > 0 ? 1 : -1;
      }
    }

    int l = (left.end - left.start) - (right.end - right.start);
    if (l > 0) {
      return 1;
    } else if (l == 0) {
      return 0;
    } else {
      return -1;
    }

  }

  public static final int compare(NullableVarCharHolder left, NullableVarCharHolder right) {
    if (left.isSet == 0) {
      if (right.isSet == 0) {
        return 0;
      }
      return -1;
    } else if (right.isSet == 0) {
      return 1;
    }

    for (int l = left.start, r = right.start; l < left.end && r < right.end; l++, r++) {
      byte leftByte = left.buffer.getByte(l);
      byte rightByte = right.buffer.getByte(r);
      if (leftByte != rightByte) {
        return ((leftByte & 0xFF) - (rightByte & 0xFF)) > 0 ? 1 : -1;
      }
    }

    int l = (left.end - left.start) - (right.end - right.start);
    if (l > 0) {
      return 1;
    } else if (l == 0) {
      return 0;
    } else {
      return -1;
    }

  }

  public static final int compare(VarBinaryHolder left, VarBinaryHolder right) {
    for (int l = left.start, r = right.start; l < left.end && r < right.end; l++, r++) {
      byte leftByte = left.buffer.getByte(l);
      byte rightByte = right.buffer.getByte(r);
      if (leftByte != rightByte) {
        return ((leftByte & 0xFF) - (rightByte & 0xFF)) > 0 ? 1 : -1;
      }
    }

    int l = (left.end - left.start) - (right.end - right.start);
    if (l > 0) {
      return 1;
    } else if (l == 0) {
      return 0;
    } else {
      return -1;
    }

  }

}
