/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package properties

import (
	"sort"
	"strings"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/constants"
)

// UpdateProperties updates configuration of coordinators or shuffle servers.
func UpdateProperties(properties string, kv map[constants.PropertyKey]string) string {
	oldKv := parseProperties(properties)
	for k, v := range kv {
		oldKv[k] = v
	}
	return generateProperties(oldKv)
}

// parseProperties converts string in properties format used in Java to a map.
func parseProperties(properties string) map[constants.PropertyKey]string {
	result := make(map[constants.PropertyKey]string)
	lines := strings.Split(properties, "\n")
	for _, line := range lines {
		kv := strings.Split(line, " ")
		if len(kv) < 2 {
			continue
		}
		result[constants.PropertyKey(kv[0])] = kv[1]
	}
	return result
}

// generateProperties converts a map to string in properties format used in Java.
func generateProperties(kv map[constants.PropertyKey]string) string {
	var lines []string
	for k, v := range kv {
		lines = append(lines, string(k)+" "+v)
	}
	sort.Strings(lines)
	return strings.Join(lines, "\n")
}
