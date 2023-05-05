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

package apistructs

import (
	"reflect"
	"testing"
	"time"
)

func TestDevFlowRule_MakeBranchRules(t *testing.T) {
	type fields struct {
		ID          string
		Flows       []Flow
		OrgID       uint64
		OrgName     string
		ProjectID   uint64
		ProjectName string
		TimeCreated time.Time
		TimeUpdated time.Time
		Creator     string
		Updater     string
	}
	tests := []struct {
		name    string
		fields  fields
		want    []*BranchRule
		wantErr bool
	}{
		{
			name: "TestDevFlowRule",
			fields: fields{
				ID: "228667a3-5a32-42a7-9d0f-995e339e52a1",
				Flows: []Flow{
					{
						Name:             "DEV",
						FlowType:         "multi_branch",
						TargetBranch:     "develop",
						ChangeFromBranch: "develop",
						ChangeBranch:     "feature/*,bugfix/*",
						EnableAutoMerge:  true,
						AutoMergeBranch:  "next_dev",
						Artifact:         "alpha",
						Environment:      "DEV",
						StartWorkflowHints: []StartWorkflowHint{
							{
								Place:            "TASK",
								ChangeBranchRule: "feature/*",
							},
							{
								Place:            "BUG",
								ChangeBranchRule: "bugfix/*",
							},
						},
					},
					{
						Name:               "TEST",
						FlowType:           "single_branch",
						TargetBranch:       "develop",
						ChangeFromBranch:   "",
						ChangeBranch:       "",
						EnableAutoMerge:    false,
						AutoMergeBranch:    "",
						Artifact:           "beta",
						Environment:        "TEST",
						StartWorkflowHints: nil,
					},
					{
						Name:               "STAGING",
						FlowType:           "multi_branch",
						TargetBranch:       "master",
						ChangeFromBranch:   "develop",
						ChangeBranch:       "release/*",
						EnableAutoMerge:    false,
						AutoMergeBranch:    "",
						Artifact:           "rc",
						Environment:        "STAGING",
						StartWorkflowHints: nil,
					},
					{
						Name:               "PROD",
						FlowType:           "single_branch",
						TargetBranch:       "master",
						ChangeFromBranch:   "",
						ChangeBranch:       "",
						EnableAutoMerge:    false,
						AutoMergeBranch:    "",
						Artifact:           "stable",
						Environment:        "PROD",
						StartWorkflowHints: nil,
					},
				},
				OrgID:       1,
				OrgName:     "terminus",
				ProjectID:   1,
				ProjectName: "erda",
			},
			want: []*BranchRule{
				{
					ID:                0,
					ScopeType:         "project",
					ScopeID:           1,
					Desc:              "",
					Rule:              "feature/*,bugfix/*",
					IsProtect:         false,
					IsTriggerPipeline: false,
					NeedApproval:      false,
					Workspace:         "DEV",
					ArtifactWorkspace: "",
				},
				{
					ID:                0,
					ScopeType:         "project",
					ScopeID:           1,
					Desc:              "",
					Rule:              "next_dev",
					IsProtect:         false,
					IsTriggerPipeline: false,
					NeedApproval:      false,
					Workspace:         "DEV",
					ArtifactWorkspace: "",
				},
				{
					ID:                0,
					ScopeType:         "project",
					ScopeID:           1,
					Desc:              "",
					Rule:              "develop",
					IsProtect:         false,
					IsTriggerPipeline: false,
					NeedApproval:      false,
					Workspace:         "TEST",
					ArtifactWorkspace: "",
				},
				{
					ID:                0,
					ScopeType:         "project",
					ScopeID:           1,
					Desc:              "",
					Rule:              "release/*",
					IsProtect:         false,
					IsTriggerPipeline: false,
					NeedApproval:      false,
					Workspace:         "STAGING",
					ArtifactWorkspace: "",
				},
				{
					ID:                0,
					ScopeType:         "project",
					ScopeID:           1,
					Desc:              "",
					Rule:              "master",
					IsProtect:         false,
					IsTriggerPipeline: false,
					NeedApproval:      false,
					Workspace:         "PROD",
					ArtifactWorkspace: "",
				},
			},
			wantErr: false,
		},
		{
			name: "test with nil",
			fields: fields{
				ID:    "228667a3-5a32-42a7-9d0f-995e339e52a1",
				Flows: nil,
			},
			want:    nil,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			f := &DevFlowRule{
				ID:          tt.fields.ID,
				Flows:       tt.fields.Flows,
				OrgID:       tt.fields.OrgID,
				OrgName:     tt.fields.OrgName,
				ProjectID:   tt.fields.ProjectID,
				ProjectName: tt.fields.ProjectName,
				TimeCreated: tt.fields.TimeCreated,
				TimeUpdated: tt.fields.TimeUpdated,
				Creator:     tt.fields.Creator,
				Updater:     tt.fields.Updater,
			}
			got, err := f.MakeBranchRules()
			if (err != nil) != tt.wantErr {
				t.Errorf("MakeBranchRules() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("MakeBranchRules() got = %v, want %v", got, tt.want)
			}
		})
	}
}
