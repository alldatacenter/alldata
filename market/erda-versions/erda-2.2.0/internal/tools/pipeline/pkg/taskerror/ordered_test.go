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

package taskerror

import (
	"sort"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestOrderedResponses(t *testing.T) {
	now := time.Now()
	respA := &Error{
		Code: "codeA",
		Msg:  "msg of codeA",
		Ctx: ErrorContext{
			StartTime: time.Time{},
			EndTime:   now,
			Count:     0,
		},
	}
	respB := &Error{
		Code: "codeB",
		Msg:  "msg of codeB",
		Ctx: ErrorContext{
			StartTime: time.Time{},
			EndTime:   now.Add(-time.Second), // before codeA
			Count:     0,
		},
	}
	resps := OrderedErrors{respA, respB}
	sort.Sort(resps)

	assert.Equal(t, 2, len(resps))
	assert.Equal(t, "codeB", resps[0].Code)
	assert.Equal(t, "codeA", resps[1].Code)
}

func TestOrderedResponses_ConvertErrors(t *testing.T) {
	type fields struct {
		Errors OrderedErrors
	}
	tests := []struct {
		name      string
		fields    fields
		converted bool
	}{
		{
			name: "count = 1",
			fields: fields{
				Errors: OrderedErrors{
					{
						Msg: "count = 1",
						Ctx: ErrorContext{
							Count: 1,
						},
					},
				},
			},
			converted: false,
		},
		{
			name: "count = 2",
			fields: fields{
				Errors: OrderedErrors{
					{
						Msg: "count = 2",
						Ctx: ErrorContext{
							Count: 2,
						},
					},
				},
			},
			converted: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t1 *testing.T) {
			o := tt.fields.Errors
			o.ConvertErrors()
			resp := o[0]
			assert.Equal(t1, tt.converted, strings.Contains(resp.Msg, "startTime: "))
		})
	}
}
