/**
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
package org.apache.ranger.common;

import org.apache.hadoop.util.HttpExceptionUtils;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.stereotype.Component;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * Invalid attribute type or Invalid JSON format in JSON request body
 *
 * We get the JSON request body which is a valid json and contains attribute names expected by the Ranger Rest API,
 * but the type of attribute is not as expected.
 * or
 * We get the JSON request body which is not a valid json
 * Jersey provider(RangerJsonMappingExceptionMapper) that converts Ranger Rest API exceptions into detailed HTTP errors.
 */
@Component
@Provider
public class RangerJsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
  @Override
  public Response toResponse(JsonMappingException excp) {
    return HttpExceptionUtils.createJerseyExceptionResponse(Response.Status.BAD_REQUEST, excp);
  }
}
