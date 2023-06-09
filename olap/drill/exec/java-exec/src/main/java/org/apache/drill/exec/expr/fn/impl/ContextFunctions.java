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

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.ops.ContextInformation;
import org.apache.drill.exec.physical.impl.project.OutputSizeEstimateConstants;

import javax.inject.Inject;

public class ContextFunctions {

  /**
   * Implement "user", "session_user" or "system_user" function. Returns the username of the user connected to Drillbit.
   */
  @FunctionTemplate(names = {"user", "session_user", "system_user"}, scope = FunctionTemplate.FunctionScope.SIMPLE,
          isNiladic = true, outputSizeEstimate = OutputSizeEstimateConstants.USER_NAME_LENGTH)
  public static class User implements DrillSimpleFunc {
    @Output VarCharHolder out;
    @Inject ContextInformation contextInfo;
    @Inject DrillBuf buffer;
    @Workspace int queryUserBytesLength;

    @Override
    public void setup() {
      final byte[] queryUserNameBytes = contextInfo.getQueryUser().getBytes();
      buffer = buffer.reallocIfNeeded(queryUserNameBytes.length);
      queryUserBytesLength = queryUserNameBytes.length;
      buffer.setBytes(0, queryUserNameBytes);
    }

    @Override
    public void eval() {
      out.start = 0;
      out.end = queryUserBytesLength;
      out.buffer = buffer;
    }
  }

  /**
   * Implement "current_schema" function. Returns the default schema in current session.
   */
  @FunctionTemplate(name = "current_schema", scope = FunctionTemplate.FunctionScope.SIMPLE, isNiladic = true,
                    outputSizeEstimate = OutputSizeEstimateConstants.SCHEMA_LENGTH)
  public static class CurrentSchema implements DrillSimpleFunc {
    @Output VarCharHolder out;
    @Inject ContextInformation contextInfo;
    @Inject DrillBuf buffer;
    @Workspace int currentSchemaBytesLength;

    @Override
    public void setup() {
      final byte[] currentSchemaBytes = contextInfo.getCurrentDefaultSchema().getBytes();
      buffer = buffer.reallocIfNeeded(currentSchemaBytes.length);
      currentSchemaBytesLength= currentSchemaBytes.length;
      buffer.setBytes(0, currentSchemaBytes);
    }

    @Override
    public void eval() {
      out.start = 0;
      out.end = currentSchemaBytesLength;
      out.buffer = buffer;
    }
  }

  /**
   * Implement "session_id" function. Returns the unique id of the current session.
   */
  @FunctionTemplate(name = "session_id", scope = FunctionTemplate.FunctionScope.SIMPLE, isNiladic = true,
                    outputSizeEstimate = OutputSizeEstimateConstants.USER_ID_LENGTH)
  public static class SessionId implements DrillSimpleFunc {
    @Output VarCharHolder out;
    @Inject ContextInformation contextInfo;
    @Inject DrillBuf buffer;
    @Workspace int sessionIdBytesLength;

    @Override
    public void setup() {
        final byte[] sessionIdBytes = contextInfo.getSessionId().getBytes();
        buffer = buffer.reallocIfNeeded(sessionIdBytes.length);
        sessionIdBytesLength = sessionIdBytes.length;
        buffer.setBytes(0, sessionIdBytes);
    }

    @Override
    public void eval() {
        out.start = 0;
        out.end = sessionIdBytesLength;
        out.buffer = buffer;
    }
  }
}
