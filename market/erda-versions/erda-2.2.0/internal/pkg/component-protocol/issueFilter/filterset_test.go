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
package issueFilter

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

func Test_filterSetValueRetriever(t *testing.T) {
	type args struct {
		filterEntity string
	}
	const f1 = "eyJwcmlvcml0aWVzIjpbIlVSR0VOVCIsIkhJR0giLCJOT1JNQUwiXX0="
	tests := []struct {
		name    string
		args    args
		want    *cptype.ExtraMap
		wantErr bool
	}{
		{
			name: "test",
			args: args{
				filterEntity: f1,
			},
			wantErr: false,
			want: &cptype.ExtraMap{
				"priorities": []string{"URGENT", "HIGH", "NORMAL"},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := FilterSetValueRetriever(tt.args.filterEntity)
			if (err != nil) != tt.wantErr {
				t.Errorf("filterSetValueRetriever() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
