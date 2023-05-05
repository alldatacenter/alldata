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

package loghub

import (
	"os"
)

// Init .
func Init() {
	setEnvDefault("LOGS_ES_URL", "ES_URL")
	setEnvDefault("LOGS_ES_SECURITY_ENABLE", "ES_SECURITY_ENABLE")
	setEnvDefault("LOGS_ES_SECURITY_USERNAME", "ES_SECURITY_USERNAME")
	setEnvDefault("LOGS_ES_SECURITY_PASSWORD", "ES_SECURITY_PASSWORD")

	_, ok := os.LookupEnv("CONFIG_NAME")
	if !ok {
		val, ok := os.LookupEnv("MONITOR_LOG_OUTPUT")
		if ok {
			os.Setenv("CONFIG_NAME", "conf/output/bootstrap-"+val)
		}
	}
}

func setEnvDefault(key string, from ...string) {
	_, ok := os.LookupEnv(key)
	if ok {
		return
	}
	for _, item := range from {
		val, ok := os.LookupEnv(item)
		if ok {
			os.Setenv(key, val)
			break
		}
	}
}
