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
package org.apache.ambari.server.security.unsecured.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.security.CertificateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("/cert/ca")
public class CertificateDownload {
  private static final Logger LOG = LoggerFactory.getLogger(CertificateDownload.class);
  private static CertificateManager certMan;

  @Inject
  public static void init(CertificateManager instance) {
    certMan = instance;
  }

  @GET @ApiIgnore // until documented
  @Produces({MediaType.TEXT_PLAIN})
  public String downloadCACertificateChainFile() {
    return certMan.getCACertificateChainContent();
  }

}
