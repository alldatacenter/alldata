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

package org.apache.ranger.rest;

public class TagRESTConstants {
	public static final String TAGDEF_NAME_AND_VERSION = "tags";

	static final String TAGDEFS_RESOURCE         = "/tagdefs/";
	static final String TAGDEF_RESOURCE          = "/tagdef/";
	static final String TAGS_RESOURCE            = "/tags/";
	static final String TAG_RESOURCE             = "/tag/";
	static final String RESOURCES_RESOURCE       = "/resources/";
	static final String RESOURCE_RESOURCE        = "/resource/";
	static final String TAGRESOURCEMAPS_RESOURCE = "/tagresourcemaps/";
	static final String IMPORT_SERVICETAGS_RESOURCE = "/importservicetags/";
	static final String TAGRESOURCEMAP_RESOURCE  = "/tagresourcemap/";
	static final String TAGTYPES_RESOURCE        = "/types/";
	static final String TAGTYPES_LOOKUP_RESOURCE = "/types/lookup/";
	static final String TAGS_DOWNLOAD            = "/download/";
	static final String TAGS_SECURE_DOWNLOAD            = "/secure/download/";

	public static final String SERVICE_NAME_PARAM           = "serviceName";
	public static final String LAST_KNOWN_TAG_VERSION_PARAM = "lastKnownVersion";
	public static final String LAST_ACTIVATION_TIME = "lastActivationTime";
	public static final String PATTERN_PARAM                = "pattern";
}
