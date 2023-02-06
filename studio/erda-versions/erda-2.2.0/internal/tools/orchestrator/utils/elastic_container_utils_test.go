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

package utils

import (
	"errors"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/bundle"
)

func TestIsProjectECIEnable(t *testing.T) {
	type args struct {
		bdl       *bundle.Bundle
		projectID uint64
		workspace string
		orgID     uint64
		userID    string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "Test_01",
			args: args{
				bdl:       &bundle.Bundle{},
				projectID: 1,
				workspace: "TEST",
				orgID:     1,
				userID:    "2",
			},
			want: true,
		},
		{
			name: "Test_02",
			args: args{
				bdl:       &bundle.Bundle{},
				projectID: 1,
				workspace: "DEV",
				orgID:     1,
				userID:    "2",
			},
			want: false,
		},
		{
			name: "Test_03",
			args: args{
				bdl:       &bundle.Bundle{},
				projectID: 1,
				workspace: "DEV",
				orgID:     1,
				userID:    "2",
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			monkey.PatchInstanceMethod(reflect.TypeOf(tt.args.bdl), "GetProjectWorkSpaceAbilities", func(bundle *bundle.Bundle, projectID uint64, workspace string, orgID uint64, userID string) (map[string]string, error) {

				if tt.name == "Test_02" {
					return nil, nil
				}

				if tt.name == "Test_03" {
					return nil, errors.New("error")
				}

				ret := map[string]string{
					"ECI": "enable",
				}
				return ret, nil
			})

			if got := IsProjectECIEnable(tt.args.bdl, tt.args.projectID, tt.args.workspace, tt.args.orgID, tt.args.userID); got != tt.want {
				t.Errorf("IsProjectECIEnable() = %v, want %v", got, tt.want)
			}
		})
	}
}
