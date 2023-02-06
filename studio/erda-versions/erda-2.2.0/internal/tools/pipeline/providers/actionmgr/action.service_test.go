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

package actionmgr

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
)

func Test_actionsOrderByLocationIndex(t *testing.T) {
	type args struct {
		locations []string
		data      []*pb.Action
	}
	tests := []struct {
		name string
		args args
		want []*pb.Action
	}{
		{
			name: "test order",
			args: args{
				locations: []string{
					"fdp/",
					"default/",
				},
				data: []*pb.Action{
					{
						Location: "default/",
						Name:     "a",
					},
					{
						Location: "default/",
						Name:     "b",
					},
					{
						Location: "fdp/",
						Name:     "a",
					},
				},
			},
			want: []*pb.Action{
				{
					Location: "fdp/",
					Name:     "a",
				},
				{
					Location: "default/",
					Name:     "a",
				},
				{
					Location: "default/",
					Name:     "b",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := actionsOrderByLocationIndex(tt.args.locations, tt.args.data); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("actionsOrderByLocationIndex() = %v, want %v", got, tt.want)
			}
		})
	}
}
