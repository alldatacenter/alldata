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
package org.apache.drill.exec.exception;

import org.apache.drill.common.exceptions.DrillRuntimeException;

/**
 * An exception that is used to signal that allocation request in bytes is greater than the maximum allowed by
 * {@link org.apache.drill.exec.memory.BufferAllocator#buffer(int) allocator}.
 *
 * <p>Operators should handle this exception to split the batch and later resume the execution on the next
 * {@link RecordBatch#next() iteration}.</p>
 *
 */
public class OversizedAllocationException extends DrillRuntimeException {
  private static final long serialVersionUID = 1L;

  public OversizedAllocationException() {
    super();
  }

  public OversizedAllocationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public OversizedAllocationException(String message, Throwable cause) {
    super(message, cause);
  }

  public OversizedAllocationException(String message) {
    super(message);
  }

  public OversizedAllocationException(Throwable cause) {
    super(cause);
  }
}
