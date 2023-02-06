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

package table

import (
	"reflect"
	"testing"
	"time"

	"github.com/alecthomas/assert"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
)

func TestGetIterations(t *testing.T) {
	tt := []struct {
		state map[string]interface{}
		want  []uint64
	}{
		{
			state: nil, want: nil,
		},
		{
			state: map[string]interface{}{"foo": "bar"}, want: nil,
		},
		{
			state: map[string]interface{}{"iteration": "bar"}, want: nil,
		},
		{
			state: map[string]interface{}{"iteration": []string{"1", "2"}}, want: nil,
		},
		{
			state: map[string]interface{}{"iteration": []uint64{1, 2}}, want: []uint64{1, 2},
		},
		{
			state: map[string]interface{}{"iteration": []interface{}{float64(1), float64(2)}}, want: []uint64{1, 2},
		},
	}
	for _, v := range tt {
		if !reflect.DeepEqual(v.want, getIterations(v.state)) {
			t.Error("fail")
		}
	}
}

func Test_ConvertSortData(t *testing.T) {
	req := apistructs.TestPlanV2PagingRequest{}
	c := &cptype.Component{
		State: map[string]interface{}{
			"sorterData": SortData{
				Field: "passRate",
				Order: OrderAscend,
			},
		},
	}
	err := convertSortData(&req, c)
	assert.NoError(t, err)
	want := apistructs.TestPlanV2PagingRequest{
		OrderBy: "pass_rate",
		Asc:     true,
	}
	assert.Equal(t, want, req)

	c = &cptype.Component{
		State: map[string]interface{}{
			"sorterData": SortData{
				Field: "executeTime",
				Order: OrderDescend,
			},
		},
	}
	err = convertSortData(&req, c)
	assert.NoError(t, err)
	want = apistructs.TestPlanV2PagingRequest{
		OrderBy: "execute_time",
		Asc:     false,
	}
	assert.Equal(t, want, req)
}

func Test_executeTime(t *testing.T) {
	nowTime := time.Now()
	var data = []*apistructs.TestPlanV2{
		{
			ExecuteTime: nil,
		},
		{
			ExecuteTime: &nowTime,
		},
	}
	want := []string{
		"",
		nowTime.Format("2006-01-02 15:04:05"),
	}
	for i := range data {
		executeTime := convertExecuteTime(data[i])
		assert.Equal(t, executeTime, want[i])
	}
}
