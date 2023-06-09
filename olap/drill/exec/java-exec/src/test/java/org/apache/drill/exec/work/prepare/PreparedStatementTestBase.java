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
package org.apache.drill.exec.work.prepare;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserProtos;
import org.apache.drill.exec.store.ischema.InfoSchemaConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PreparedStatementTestBase extends BaseTestQuery {

  /* Helper method which creates a prepared statement for given query. */
  protected UserProtos.PreparedStatement createPrepareStmt(String query,
                                                        boolean expectFailure,
                                                        UserBitShared.DrillPBError.ErrorType errorType) throws Exception {
    UserProtos.CreatePreparedStatementResp resp = client.createPreparedStatement(query).get();

    if (expectFailure) {
      assertEquals(UserProtos.RequestStatus.FAILED, resp.getStatus());
      assertEquals(errorType, resp.getError().getErrorType());
    } else {
      String message = resp.hasError() ? resp.getError().getMessage() : "No errors";
      assertEquals(message, UserProtos.RequestStatus.OK, resp.getStatus());
    }

    return resp.getPreparedStatement();
  }

  protected void verifyMetadata(List<ExpectedColumnResult> expMetadata,
                                     List<UserProtos.ResultColumnMetadata> actMetadata) {
    assertEquals(expMetadata.size(), actMetadata.size());

    int i = 0;
    for (ExpectedColumnResult exp : expMetadata) {
      UserProtos.ResultColumnMetadata act = actMetadata.get(i++);

      assertTrue("Failed to find the expected column metadata: " + exp + ". Was: " + toString(act), exp.isEqualsTo(act));
    }
  }

  protected static class ExpectedColumnResult {
    final String columnName;
    final String type;
    final boolean nullable;
    final int displaySize;
    final int precision;
    final int scale;
    final boolean signed;
    final String className;

    ExpectedColumnResult(String columnName, String type, boolean nullable, int displaySize, int precision, int scale,
                         boolean signed, String className) {
      this.columnName = columnName;
      this.type = type;
      this.nullable = nullable;
      this.displaySize = displaySize;
      this.precision = precision;
      this.scale = scale;
      this.signed = signed;
      this.className = className;
    }

    boolean isEqualsTo(UserProtos.ResultColumnMetadata result) {
      return
          result.getCatalogName().equals(InfoSchemaConstants.IS_CATALOG_NAME) &&
              result.getSchemaName().isEmpty() &&
              result.getTableName().isEmpty() &&
              result.getColumnName().equals(columnName) &&
              result.getLabel().equals(columnName) &&
              result.getDataType().equals(type) &&
              result.getIsNullable() == nullable &&
              result.getPrecision() == precision &&
              result.getScale() == scale &&
              result.getSigned() == signed &&
              result.getDisplaySize() == displaySize &&
              result.getClassName().equals(className) &&
              result.getSearchability() == UserProtos.ColumnSearchability.ALL &&
              result.getAutoIncrement() == false &&
              result.getCaseSensitivity() == false &&
              result.getUpdatability() == UserProtos.ColumnUpdatability.READ_ONLY &&
              result.getIsAliased() == true &&
              result.getIsCurrency() == false;
    }

    @Override
    public String toString() {
      return "ExpectedColumnResult[" +
          "columnName='" + columnName + '\'' +
          ", type='" + type + '\'' +
          ", nullable=" + nullable +
          ", displaySize=" + displaySize +
          ", precision=" + precision +
          ", scale=" + scale +
          ", signed=" + signed +
          ", className='" + className + '\'' +
          ']';
    }
  }

  private static String toString(UserProtos.ResultColumnMetadata metadata) {
    return "ResultColumnMetadata[" +
        "columnName='" + metadata.getColumnName() + '\'' +
        ", type='" + metadata.getDataType() + '\'' +
        ", nullable=" + metadata.getIsNullable() +
        ", displaySize=" + metadata.getDisplaySize() +
        ", precision=" + metadata.getPrecision() +
        ", scale=" + metadata.getScale() +
        ", signed=" + metadata.getSigned() +
        ", className='" + metadata.getClassName() + '\'' +
        ']';
  }
}
