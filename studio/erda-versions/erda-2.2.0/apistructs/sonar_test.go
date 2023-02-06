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

package apistructs

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestValue(t *testing.T) {
	sonarConfigStr := `{"host":"http://localhost:9000","token":"xxx","projectKey":"application"}`
	sc := &SonarConfig{
		Host:       "http://localhost:9000",
		Token:      "xxx",
		ProjectKey: "application",
	}
	value, err := sc.Value()
	assert.NoError(t, err)
	assert.Equal(t, sonarConfigStr, value)
}

func TestScan(t *testing.T) {
	sonarConfigStr := `{"host":"http://localhost:9000","token":"xxx","projectKey":"application"}`
	sc := &SonarConfig{}
	err := sc.Scan([]byte(sonarConfigStr))
	assert.NoError(t, err)
}
