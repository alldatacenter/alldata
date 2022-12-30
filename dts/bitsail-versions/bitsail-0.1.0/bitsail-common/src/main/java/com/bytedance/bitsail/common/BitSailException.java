/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Files: alibaba/DataX (https://github.com/alibaba/DataX)
 * Copyright: Copyright 1999-2022 Alibaba Group Holding Ltd.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.common;

import com.bytedance.bitsail.common.exception.ErrorCode;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @desc:
 */
public class BitSailException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private ErrorCode errorCode;
  private String errorMessage;

  public BitSailException(ErrorCode errorCode, String errorMessage) {
    super(errorCode.toString() + " - " + errorMessage);
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  private BitSailException(ErrorCode errorCode, String errorMessage, Throwable cause) {
    super(errorCode.toString() + " - " + getMessage(errorMessage) + " - " + getMessage(cause), cause);
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
  }

  public static BitSailException asBitSailException(ErrorCode errorCode, String message) {
    return new BitSailException(errorCode, message);
  }

  public static BitSailException asBitSailException(ErrorCode errorCode, String message, Throwable cause) {
    if (cause instanceof BitSailException) {
      return (BitSailException) cause;
    }
    return new BitSailException(errorCode, message, cause);
  }

  public static BitSailException asBitSailException(ErrorCode errorCode, Throwable cause) {
    if (cause instanceof BitSailException) {
      return (BitSailException) cause;
    }
    return new BitSailException(errorCode, getMessage(cause), cause);
  }

  private static String getMessage(Object obj) {
    if (obj == null) {
      return "";
    }

    if (obj instanceof Throwable) {
      StringWriter str = new StringWriter();
      PrintWriter pw = new PrintWriter(str);
      ((Throwable) obj).printStackTrace(pw);
      return str.toString();
    } else {
      return obj.toString();
    }
  }

  public ErrorCode getErrorCode() {
    return this.errorCode;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }
}
