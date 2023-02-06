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

package jsonpath

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"strings"
)

func JQ(jsonInput, filter string) (interface{}, error) {
	f, err := ioutil.TempFile("", "input")
	if err != nil {
		return nil, err
	}
	defer os.Remove(f.Name())
	f.WriteString(jsonInput)
	filter = strings.ReplaceAll(filter, `"`, `\"`)
	filter = filter + " | select(.!=null) | tojson"
	jq := fmt.Sprintf(`jq -c -j "%s" '%s'`, filter, f.Name())
	wrappedJQ := exec.Command("/bin/sh", "-c", jq)
	output, err := wrappedJQ.CombinedOutput()
	if err != nil {
		return "", fmt.Errorf("jq failed, filter: %s, err: %v, reason: %s", filter, err, string(output))
	}
	if len(output) > 0 {
		var o interface{}
		if err := json.Unmarshal(output, &o); err != nil {
			return nil, err
		}
		return o, nil
	}
	return nil, nil
}
