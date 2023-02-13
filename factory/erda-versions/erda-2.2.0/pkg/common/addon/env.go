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

package addon

import (
	"fmt"
	"os"
	"strconv"
	"strings"
)

// GetHostIndex .
func GetHostIndex() string {
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

// OverrideEnvs .
func OverrideEnvs() {
	idx := GetHostIndex()
	if len(idx) <= 0 {
		return
	}
	prefix := "N" + idx + "_"
	for _, kv := range os.Environ() {
		parts := strings.SplitN(kv, "=", 2)
		if len(parts) < 2 {
			continue
		}
		key := parts[0]
		val := parts[1]
		if strings.HasPrefix(key, prefix) && len(val) > 0 {
			nkey := key[len(prefix):]
			if len(os.Getenv(nkey)) <= 0 {
				os.Setenv(nkey, val)
			}
		}
	}
	fmt.Println("Override envs \n", strings.Join(os.Environ(), "\n"))
}
