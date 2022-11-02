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
package org.apache.ambari.server.api.services;

import java.io.IOException;
import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.bind.JAXBException;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("/keys/")
public class KeyService {
  private static final Logger log = LoggerFactory.getLogger(KeyService.class);
  private static PersistKeyValueImpl persistKeyVal;

  @Inject
  public static void init(PersistKeyValueImpl instance) {
    persistKeyVal = instance;
  }

  @Path("{number}")
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public String getKeys(@PathParam("number") int number) throws IOException, JAXBException {
    Collection<String> keys = persistKeyVal.generateKeys(number);
    String result = StageUtils.jaxbToString(keys);
    log.info("Returning keys {}", result);
    return result;
  }

}
