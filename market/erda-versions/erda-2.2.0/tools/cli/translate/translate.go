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

package translate

import (
	"strings"
)

var items = map[string]string{
	"session not exist":            "session expired, please login again",
	"lack of required auth header": "not login yet, please login first",
	//"failed to request http, status-code 404, content-type text/plain; charset=utf-8, raw body not found path": "http url interface not match, please check the version of CLI and dice openAPI server",
}

func Translate(err error) string {
	errStr := err.Error()
	for item, r := range items {
		if strings.Contains(errStr, item) {
			return r
		}
	}
	return errStr
}
