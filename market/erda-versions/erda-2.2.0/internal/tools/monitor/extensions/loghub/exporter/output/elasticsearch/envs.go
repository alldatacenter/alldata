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

package elasticsearch

import (
	"os"
	"strings"
)

func setEnvDefault(key, val string) {
	if len(val) > 0 {
		_, ok := os.LookupEnv(key)
		if ok {
			return
		}
		os.Setenv(key, val)
	}
}

func init() {
	env := os.Getenv("MONITOR_LOG_OUTPUT_CONFIG")
	if len(env) > 0 {
		lines := strings.Split(env, " ")
		for _, line := range lines {
			line = strings.TrimSpace(line)
			if len(line) <= 0 {
				continue
			}
			kv := strings.SplitN(line, "=", 2)
			if len(kv) == 1 {
				if strings.HasPrefix(kv[0], "http://") || strings.HasPrefix(kv[0], "https://") {
					setEnvDefault("ES_URLS", kv[0])
				}
			} else if len(kv) == 2 {
				setEnvDefault(strings.ToUpper(kv[0]), kv[1])

			}
		}
	}
}
