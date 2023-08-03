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

package org.apache.ranger.policyengine;

import java.lang.reflect.Type;

import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * {@link JsonDeserializer} to assist {@link Gson} with selecting proper type
 * when encountering RangerAccessResource interface in the source json.
 */
public class RangerResourceDeserializer implements JsonDeserializer<RangerAccessResource> {

	private GsonBuilder gsonBuilder;

	public RangerResourceDeserializer(final GsonBuilder builder) {
		this.gsonBuilder = builder;
	}

	@Override
	public RangerAccessResource deserialize(JsonElement jsonObj, Type type, JsonDeserializationContext context) throws JsonParseException {
		return gsonBuilder.create().fromJson(jsonObj, RangerAccessResourceImpl.class);
	}
}