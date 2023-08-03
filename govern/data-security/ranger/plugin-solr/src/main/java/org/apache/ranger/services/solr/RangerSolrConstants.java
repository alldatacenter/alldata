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

package org.apache.ranger.services.solr;

import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RangerSolrConstants {

    // Constants from RangerSolrAuthorizer
    public static final Logger PERF_SOLRAUTH_REQUEST_LOG = RangerPerfTracer.getPerfLogger("solrauth.request");
    public static final String SUPERUSER = System.getProperty("solr.authorization.superuser", "solr");

    public static final String AUTH_FIELD_PROP = "rangerAuthField";
    public static final String DEFAULT_AUTH_FIELD = "ranger_auth";
    public static final String ALL_ROLES_TOKEN_PROP = "allRolesToken";
    public static final String ENABLED_PROP = "enabled";
    public static final String MODE_PROP = "matchMode";
    public static final String DEFAULT_MODE_PROP = MatchType.DISJUNCTIVE.toString();

    public static final String ALLOW_MISSING_VAL_PROP = "allow_missing_val";
    public static final String TOKEN_COUNT_PROP = "tokenCountField";
    public static final String DEFAULT_TOKEN_COUNT_FIELD_PROP = "ranger_auth_count";
    public static final String QPARSER_PROP = "qParser";

    public static final String PROP_USE_PROXY_IP = "xasecure.solr.use_proxy_ip";
    public static final String PROP_PROXY_IP_HEADER = "xasecure.solr.proxy_ip_header";
    public static final String PROP_SOLR_APP_NAME = "xasecure.solr.app.name";

    public static final String ATTRS_ENABLED_PROP = "attrs_enabled";
    public static final String FIELD_ATTR_MAPPINGS = "field_attr_mappings";
    public static final String FIELD_FILTER_TYPE = "filter_type";
    public static final String ATTR_NAMES = "attr_names";
    public static final String PERMIT_EMPTY_VALUES = "permit_empty";
    public static final String ALL_USERS_VALUE = "all_users_value";
    public static final String ATTRIBUTE_FILTER_REGEX = "value_filter_regex";
    public static final String AND_OP_QPARSER = "andQParser";
    public static final String EXTRA_OPTS = "extra_opts";

    public enum MatchType {
        DISJUNCTIVE,
        CONJUNCTIVE
    }

    // Constants from ServiceSolrClient
    public enum RESOURCE_TYPE {
        COLLECTION, FIELD, CONFIG, ADMIN, SCHEMA;

        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum ADMIN_TYPE {
        COLLECTIONS, CORES, METRICS, SECURITY, AUTOSCALING;

        public static final List<String> VALUE_LIST = Stream.of(ADMIN_TYPE.values())
                .map(Enum::toString)
                .collect(Collectors.toList());

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum ACCESS_TYPE {
        QUERY, UPDATE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public static final String errMessage = " You can still save the repository and start creating "
            + "policies, but you would not be able to use autocomplete for "
            + "resource names. Check server logs for more info.";

    public static final String COLLECTION_KEY = "collection";
    public static final String FIELD_KEY = "field";
    public static final String CONFIG_KEY = "config";
    public static final String ADMIN_KEY = "admin";
    public static final String SCHEMA_KEY = "schema";
    public static final long LOOKUP_TIMEOUT_SEC = 5;
}
