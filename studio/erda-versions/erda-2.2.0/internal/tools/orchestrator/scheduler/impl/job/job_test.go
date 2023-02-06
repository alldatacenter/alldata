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

package job

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func Test_appendJobTags(t *testing.T) {
	type args struct {
		labels map[string]string
	}
	tests := []struct {
		name string
		args args
		want map[string]string
	}{
		{
			name: "Test_01",
			args: args{
				labels: map[string]string{
					"A":                         "a",
					apistructs.LabelPack:        "true",
					apistructs.LabelExcludeTags: "xxx",
				},
			},
			want: map[string]string{
				"A":                         "a",
				apistructs.LabelPack:        "true",
				apistructs.LabelExcludeTags: "xxx,locked,platform",
				apistructs.LabelMatchTags:   "job,pack",
			},
		},
		{
			name: "Test_02",
			args: args{
				labels: map[string]string{
					"A":                     "a",
					apistructs.LabelJobKind: apistructs.TagBigdata,
					apistructs.LabelPack:    "true",
				},
			},
			want: map[string]string{
				"A":                         "a",
				apistructs.LabelJobKind:     apistructs.TagBigdata,
				apistructs.LabelPack:        "true",
				apistructs.LabelExcludeTags: "locked,platform",
				apistructs.LabelMatchTags:   "pack,bigdata",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := appendJobTags(tt.args.labels); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("appendJobTags() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_validate(t *testing.T) {
	type args struct {
		name string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "Test_01",
			args: args{
				name: "test01",
			},
			want: true,
		},
		{
			name: "Test_02",
			args: args{
				name: "test&02",
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := validateJobName(tt.args.name); got != tt.want {
				t.Errorf("validateJobName() = %v, want %v", got, tt.want)
			}

			if got := validateJobNamespace(tt.args.name); got != tt.want {
				t.Errorf("validateJobName() = %v, want %v", got, tt.want)
			}

			if got := validateJobFlowID(tt.args.name); got != tt.want {
				t.Errorf("validateJobName() = %v, want %v", got, tt.want)
			}
		})
	}
}
