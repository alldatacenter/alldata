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
	"encoding/json"
	"strings"
)

// IsEncryptedValueByKey Determine whether it is an encrypted field by key
func IsEncryptedValueByKey(key string) bool {
	return strings.Contains(strings.ToLower(key), "pass") || strings.Contains(strings.ToLower(key), "secret")
}

// IsEncryptedValueByValue Determine whether it is an encrypted field by value
func IsEncryptedValueByValue(value string) bool {
	return strings.Contains(value, ErdaEncryptedValue)
}

// GetAddonConfig return unmarshal config
func GetAddonConfig(cfgStr string) (map[string]interface{}, error) {
	cfg := map[string]interface{}{}
	if err := json.Unmarshal([]byte(cfgStr), &cfg); err != nil {
		return nil, err
	}

	return cfg, nil
}
