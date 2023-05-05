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

package workList

import (
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/list"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
)

func TestWorkList_GenProjKvColumnInfo(t *testing.T) {
	type fields struct{}
	type args struct {
		proj      apistructs.WorkbenchProjOverviewItem
		q         workbench.IssueUrlQueries
		mspParams map[string]interface{}
	}
	tests := []struct {
		name        string
		fields      fields
		args        args
		wantKvs     []list.KvInfo
		wantColumns map[string]interface{}
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{
				proj: apistructs.WorkbenchProjOverviewItem{
					ProjectDTO: apistructs.ProjectDTO{
						Type: "fake",
					},
				},
				q:         workbench.IssueUrlQueries{},
				mspParams: nil,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			l := &WorkList{}
			l.GenProjKvColumnInfo(tt.args.proj, tt.args.q, tt.args.mspParams)
		})
	}
}
