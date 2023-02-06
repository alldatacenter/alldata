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

package endpoints

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func Test_getUserMtPlanResourceRoles(t *testing.T) {
	type args struct {
		userID string
		mtPlan apistructs.TestPlan
	}
	tests := []struct {
		name      string
		args      args
		wantRoles []string
	}{
		{
			name: "no resource role",
			args: args{
				userID: "123",
				mtPlan: apistructs.TestPlan{},
			},
			wantRoles: nil,
		},
		{
			name: "mt plan owner",
			args: args{
				userID: "123",
				mtPlan: apistructs.TestPlan{OwnerID: "123"},
			},
			wantRoles: []string{apistructs.ResourceRoleOwner},
		},
		{
			name: "mt plan partner",
			args: args{
				userID: "123",
				mtPlan: apistructs.TestPlan{PartnerIDs: []string{"123"}},
			},
			wantRoles: []string{apistructs.ResourceRolePartner},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if gotRoles := getUserMtPlanResourceRoles(tt.args.userID, tt.args.mtPlan); !reflect.DeepEqual(gotRoles, tt.wantRoles) {
				t.Errorf("getUserMtPlanResourceRoles() = %v, want %v", gotRoles, tt.wantRoles)
			}
		})
	}
}
