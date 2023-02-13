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
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"strings"

	"github.com/sirupsen/logrus"
)

const JacksonExpressPrefix = "$."

// use jackson-path command to parse json
func Jackson(jsonInput, filter string) (interface{}, error) {
	f, err := ioutil.TempFile("", "input")
	if err != nil {
		return nil, err
	}
	defer os.Remove(f.Name())
	f.WriteString(jsonInput)
	jq := fmt.Sprintf(`jackson-path -f '%s' -p '%s' -u`, f.Name(), filter)
	wrappedJQ := exec.Command("/bin/sh", "-c", jq)
	output, err := wrappedJQ.CombinedOutput()
	if err != nil {
		logrus.Errorf("jackson failed, filter: %s, input: %s, err: %v", filter, jsonInput, err)
		return "", fmt.Errorf("jackson failed, filter: %s, err: %v, reason: %s", filter, err, string(output))
	}
	return strings.TrimSpace(string(output)), nil
}
