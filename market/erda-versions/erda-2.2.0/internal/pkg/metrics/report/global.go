// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package report

import (
	"os"
	"strconv"
	"strings"
)

type GlobalLabel map[string]string

var globalLabels = GlobalLabel{
	"_meta":   "true",
	"_custom": "true",
}

func getEnvKey(key, index string) string {
	if len(index) > 0 {
		return "N" + index + "_" + key
	}
	return key
}

func getHostIndex() string {
	hostname, err := os.Hostname()
	if err == nil {
		idx := strings.LastIndex(hostname, "-")
		if idx > 0 {
			_, err = strconv.Atoi(hostname[idx+1:])
			if err == nil {
				return hostname[idx+1:]
			}
		}
	}
	return ""
}

func init() {
	hostIndex := getHostIndex()
	for key, tag := range map[string]string{
		"DICE_ORG_NAME":         "org_name",
		"DICE_ORG_ID":           "org_id",
		"DICE_CLUSTER_TYPE":     "cluster_type",
		"DICE_IS_EDGE":          "is_edge",
		"DICE_CLUSTER_NAME":     "cluster_name",
		"HOST_IP":               "host_ip",
		"DICE_COMPONENT":        "component",
		"DICE_VERSION":          "version",
		"ADDON_TYPE":            "addon_type",
		"ADDON_ID":              "addon_id",
		"DICE_PROJECT_NAME":     "project_name",
		"DICE_PROJECT":          "project_id",
		"DICE_APPLICATION_NAME": "application_name",
		"DICE_APPLICATION":      "application_id",
		"DICE_SERVICE_NAME":     "service_name",
		"DICE_WORKSPACE":        "workspace",
		"DICE_RUNTIME_NAME":     "runtime_name",
		"TERMINUS_KEY":          "terminus_key",
	} {
		key = getEnvKey(key, hostIndex)
		val := os.Getenv(key)
		if len(val) > 0 {
			globalLabels[tag] = val
		}
	}
	for key, tag := range map[string]string{
		"CLUSTER_NAME": "cluster_name",
		"DICE_ADDON":   "addon_id",
	} {
		if len(globalLabels[tag]) <= 0 {
			key = getEnvKey(key, hostIndex)
			val := os.Getenv(key)
			if len(val) > 0 {
				globalLabels[tag] = val
			}
		}
	}

	type match struct {
		keys     []string
		scope    string
		scopeKey string
	}
loop:
	for _, m := range []*match{
		{
			keys: []string{"TERMINUS_KEY"}, scope: "micro_service",
		},
		{
			keys: []string{"ADDON_ID"}, scope: "addon",
		},
		{
			keys: []string{"DICE_COMPONENT"}, scope: "dice", scopeKey: "cluster_name",
		},
		{
			keys: []string{"DICE_ORG_ID"}, scope: "org",
		},
	} {
		var value string
		for _, key := range m.keys {
			key = getEnvKey(key, hostIndex)
			val := os.Getenv(key)
			if len(val) <= 0 {
				continue loop
			}
			if len(value) <= 0 {
				value = val
			}
		}
		globalLabels["_metric_scope"] = m.scope
		if len(m.scopeKey) > 0 {
			if len(globalLabels[m.scopeKey]) > 0 {
				globalLabels["_metric_scope_id"] = globalLabels[m.scopeKey]
			} else {
				globalLabels["_metric_scope_id"] = os.Getenv(getEnvKey(m.scopeKey, hostIndex))
			}
		} else {
			globalLabels["_metric_scope_id"] = value
		}
		break
	}
}
