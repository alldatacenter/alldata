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

package ucauth

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPolishUnassignedAsEmptyStr(t *testing.T) {
	type args struct {
		userIDs []string
	}
	tests := []struct {
		name       string
		args       args
		wantResult []string
	}{
		{
			name: "no unassigned",
			args: args{
				userIDs: []string{"1", "2"},
			},
			wantResult: []string{"1", "2"},
		},
		{
			name: "have unassigned",
			args: args{
				userIDs: []string{"1", UnassignedUserID.String(), "2"},
			},
			wantResult: []string{"1", emptyUserID.String(), "2"},
		},
		{
			name: "non users",
			args: args{
				userIDs: []string{},
			},
			wantResult: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equalf(t, tt.wantResult, PolishUnassignedAsEmptyStr(tt.args.userIDs), "PolishUnassignedAsEmptyStr(%v)", tt.args.userIDs)
		})
	}
}
