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
	"time"

	"github.com/stretchr/testify/assert"
)

func TestGetFormartStartTime(t *testing.T) {
	req := &TaskErrorListRequest{StartTime: "2021-12-01 01:01:01"}
	startTime, err := req.GetFormartStartTime()
	assert.NoError(t, err)
	assert.Equal(t, time.Month(12), startTime.Month())
}

func TestConvertToQueryParams(t *testing.T) {
	req := &TaskErrorListRequest{
		StartTime:     "2021-12-01 01:01:01",
		ResourceIDS:   []string{"1", "2"},
		ResourceTypes: []ErrorResourceType{PipelineError, RuntimeError},
	}
	params := req.ConvertToQueryParams()
	assert.Equal(t, "2021-12-01 01:01:01", params.Get("startTime"))
	assert.Equal(t,
		"resourceIds=1&resourceIds=2&resourceTypes=pipeline&resourceTypes=runtime&startTime=2021-12-01+01%3A01%3A01",
		params.Encode())
}
