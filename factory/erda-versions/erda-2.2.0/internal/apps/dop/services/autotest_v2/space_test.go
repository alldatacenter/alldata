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

package autotestv2

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func Test_getChangedFields(t *testing.T) {
	type args struct {
		autoTestSpace *dao.AutoTestSpace
		req           apistructs.AutoTestSpace
	}
	tests := []struct {
		name string
		args args
		want map[string][]string
	}{
		{
			name: "test",
			args: args{
				autoTestSpace: &dao.AutoTestSpace{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
					Name:          "space",
					Description:   "not a space",
					ArchiveStatus: apistructs.TestSpaceInit,
				},
				req: apistructs.AutoTestSpace{
					ID:            1,
					Name:          "space1",
					Description:   "a space",
					ArchiveStatus: apistructs.TestSpaceInProgress,
				},
			},
			want: map[string][]string{
				"Name":          {"space", "space1"},
				"Description":   {"not a space", "a space"},
				"ArchiveStatus": {"未开始", "进行中"},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getChangedFields(tt.args.autoTestSpace, tt.args.req); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getChangedFields() = %v, want %v", got, tt.want)
			}
		})
	}
}
