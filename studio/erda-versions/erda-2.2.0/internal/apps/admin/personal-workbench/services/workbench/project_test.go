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

package workbench

import (
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"

	projpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func TestProject_ListProjWbOverviewData(t *testing.T) {
	tests := []struct {
		name    string
		desc    string
		wantErr bool
		dopErr  bool
		mspErr  bool
	}{
		{
			name:    "dop_issue_query_error",
			wantErr: false,
			dopErr:  true,
			mspErr:  false,
		},
		{
			name:    "dop_msp_query_error",
			wantErr: false,
			dopErr:  false,
			mspErr:  true,
		},
		{
			name:    "query_success",
			wantErr: false,
			dopErr:  false,
			mspErr:  false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var projects []apistructs.ProjectDTO

			identity := apistructs.Identity{
				UserID: "2",
				OrgID:  "1",
			}
			bdl := &bundle.Bundle{}
			wb := New(WithBundle(bdl))

			monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetWorkbenchData", func(c *bundle.Bundle, userID string, req apistructs.WorkbenchRequest) (*apistructs.WorkbenchResponse, error) {
				if tt.dopErr {
					return nil, fmt.Errorf("error")
				}
				return &apistructs.WorkbenchResponse{}, nil
			})
			defer monkey.UnpatchAll()

			monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetMSPTenantProjects", func(c *bundle.Bundle, userID, orgID string, withStats bool, projectIds []uint64) ([]*projpb.Project, error) {
				if tt.mspErr {
					return nil, fmt.Errorf("error")
				}
				return nil, nil
			})

			_, err := wb.ListProjWbOverviewData(identity, projects)
			if (err != nil) != tt.wantErr {
				t.Errorf("ListProjWbOverviewData() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

		})
	}
}
