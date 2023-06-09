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
package org.apache.drill.test;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.client.LoggingResultsListener;
import org.apache.drill.exec.client.QuerySubmitter;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.user.QueryDataBatch;

public class PrintingResultsListener extends LoggingResultsListener {
  public PrintingResultsListener(DrillConfig config, QuerySubmitter.Format format, int columnWidth) {
    super(config, format, columnWidth);
  }

  @Override
  public void submissionFailed(UserException ex) {
    PrintingUtils.print(() -> {
      super.submissionFailed(ex);
      return null;
    });
  }

  @Override
  public void queryCompleted(UserBitShared.QueryResult.QueryState state) {
    PrintingUtils.print(() -> {
      super.queryCompleted(state);
      return null;
    });
  }

  @Override
  public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
    PrintingUtils.print(() -> {
      super.dataArrived(result, throttle);
      return null;
    });
  }
}
