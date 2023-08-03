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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.ranger.unixusersync.poc;




public class RangerJSONParser {

	/*
 private static final Log LOG = LogFactory.getLog(RangerJSONParser.class);

 private static JSONObject jObj = null;

 private static String jsonstr = "";


 public  JSONObject getJSONFromUrl(String url) {

     try {

		Client client = Client.create();
		WebResource webResource = client
		   .resource(url);

		ClientResponse response = webResource.accept("application/json")
                  .get(ClientResponse.class);

		if (response.getStatus() != 200) {
		   throw new RuntimeException("Failed : HTTP error code : "
			+ response.getStatus());
		}

		jsonstr = response.getEntity(String.class);
		jObj = new JSONObject(jsonstr);

	    } catch (Exception e) {
		LOG.error("XaSecure JSON Parser:Error parsing data" , e);
		}

	return jObj;
    }

    */

}
