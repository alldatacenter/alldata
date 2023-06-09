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
package org.apache.drill.common.exceptions;

import org.apache.drill.exec.proto.UserBitShared.DrillPBError;

import static org.apache.drill.common.util.DrillExceptionUtil.getThrowable;

/**
 * Wraps a DrillPBError object so we don't need to rebuilt it multiple times
 * when sending it to the client. It also gives access to the original exception
 * className and message.
 */
@SuppressWarnings("serial")
public class UserRemoteException extends UserException {

  private final DrillPBError error;

  public UserRemoteException(DrillPBError error) {
    super(error.getErrorType(), "Drill Remote Exception", getThrowable(error.getException()));
    this.error = error;
  }

  @Override
  public String getMessage() {
    return error.getMessage(); // we don't want super class to generate the error message
  }

  @Override
  public DrillPBError getOrCreatePBError(boolean verbose) {
    return error;
  }
}
