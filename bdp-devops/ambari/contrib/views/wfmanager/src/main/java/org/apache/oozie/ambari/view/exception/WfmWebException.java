/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view.exception;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.security.AccessControlException;
import org.json.simple.JSONObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;

public class WfmWebException extends WebApplicationException {
  private static final int STATUS = 500;
  private ErrorCode errorCode;
  private String additionalDetail = null;
  private String message;
  public WfmWebException(String message) {
    super();
    setMessage(message);
  }

  private void setMessage(String message) {
    this.message=message;
  }

  public WfmWebException(Throwable cause) {
    super(cause);
  }

  public WfmWebException(ErrorCode errorCode) {
    super();
    setMessage(errorCode.getDescription());
    this.errorCode = errorCode;
  }

  public WfmWebException(String message, Throwable cause) {
    super(cause);
    setMessage(message);
  }

  public WfmWebException(String message, ErrorCode errorCode) {
    super();
    setMessage(message);
    this.errorCode = errorCode;
  }

  public WfmWebException(String message, Throwable cause, ErrorCode errorCode) {
    super(cause);
    setMessage(message);
    this.errorCode = errorCode;
  }

  public WfmWebException(Throwable cause, ErrorCode errorCode) {
    super(cause);
    setMessage(errorCode.getDescription());
    this.errorCode = errorCode;
  }


  public void setAdditionalDetail(String additionalDetail) {
    this.additionalDetail = additionalDetail;
  }

  @Override
  public Response getResponse() {
    HashMap<String, Object> response = new HashMap<String, Object>();
    String trace = null;
    Throwable ex = this.getCause();
    if (ex != null) {
      if (ex.getStackTrace().length<1){
        trace = ExceptionUtils.getStackTrace(this);
      }else{
        trace = ExceptionUtils.getStackTrace(ex);
      }
      if (ex instanceof AccessControlException) {
        errorCode = ErrorCode.FILE_ACCESS_ACL_ERROR;
      } else if (ex instanceof IOException) {
        errorCode = ErrorCode.FILE_ACCESS_UNKNOWN_ERROR;
      }
    }else{
      trace = ExceptionUtils.getStackTrace(this);
    }
    response.put("stackTrace", trace);
    int status = errorCode != null && errorCode.isInputError() ? Response.Status.BAD_REQUEST.getStatusCode() : STATUS;
    if (errorCode != null) {
      response.put("errorCode", errorCode.getErrorCode());
      response.put("message", errorCode.getDescription());
    } else {
      if (this.getMessage()!=null){
        response.put("message", this.getMessage());
      }else if (this.getCause()!=null){
        response.put("message", this.getCause().getMessage());
      }
    }
    if (this.additionalDetail != null) {
      response.put("additionalDetail", additionalDetail);
    }
    return Response.status(status).entity(new JSONObject(response)).type(MediaType.APPLICATION_JSON).build();
  }

  @Override
  public String getMessage() {
    return message;
  }
}
