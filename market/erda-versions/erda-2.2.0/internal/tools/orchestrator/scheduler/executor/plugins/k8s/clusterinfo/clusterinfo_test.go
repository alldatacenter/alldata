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

package clusterinfo

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestParseNetportalURL(t *testing.T) {
	var (
		testURL1 = "inet://127.0.0.1/master.mesos"
		testURL2 = "inet://127.0.0.2?ssl=on&direct=on/master.mesos/service/marathon?test=on"
	)

	netportal, err := parseNetportalURL(testURL1)
	assert.Nil(t, err)
	assert.Equal(t, "inet://127.0.0.1", netportal)

	netportal, err = parseNetportalURL(testURL2)
	assert.Nil(t, err)
	assert.Equal(t, "inet://127.0.0.2?ssl=on&direct=on", netportal)
}
