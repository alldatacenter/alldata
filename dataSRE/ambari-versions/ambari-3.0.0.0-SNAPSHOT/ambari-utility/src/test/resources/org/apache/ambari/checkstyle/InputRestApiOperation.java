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
package org.apache.ambari.checkstyle;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import io.swagger.annotations.ApiOperation;

import org.apache.ambari.annotations.ApiIgnore;

/**
 * Input file for UndocumentedRestApiOperationCheck.
 */
public class InputRestApiOperation {

  @DELETE
  public void undocumentedDELETE() {
    ;
  }

  @DELETE
  @ApiOperation(value = "...")
  public void documentedDELETE() {
    ;
  }

  @DELETE
  @ApiIgnore
  public void ignoredDELETE() {
    ;
  }

  @HEAD
  public void undocumentedHEAD() {
    ;
  }

  @HEAD
  @ApiOperation(value = "...")
  public void documentedHEAD() {
    ;
  }

  @HEAD
  @ApiIgnore
  public void ignoredHEAD() {
    ;
  }

  @GET
  public void undocumentedGET() {
    ;
  }

  @GET
  @ApiOperation(value = "...")
  public void documentedGET() {
    ;
  }

  @GET
  @ApiIgnore
  public void ignoredGET() {
    ;
  }

  @OPTIONS
  public void undocumentedOPTIONS() {
    ;
  }

  @OPTIONS
  @ApiOperation(value = "...")
  public void documentedOPTIONS() {
    ;
  }

  @OPTIONS
  @ApiIgnore
  public void ignoredOPTIONS() {
    ;
  }

  @POST
  public void undocumentedPOST() {
    ;
  }

  @POST
  @ApiOperation(value = "...")
  public void documentedPOST() {
    ;
  }

  @POST
  @ApiIgnore
  public void ignoredPOST() {
    ;
  }

  @PUT
  public void undocumentedPUT() {
    ;
  }

  @PUT
  @ApiOperation(value = "...")
  public void documentedPUT() {
    ;
  }

  @PUT
  @ApiIgnore
  public void ignoredPUT() {
    ;
  }

}
