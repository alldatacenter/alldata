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

package devflowrule

import (
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/apps/dop/providers/devflowrule/db"
)

func Test_provider_GetFlowByRule(t *testing.T) {
	type fields struct {
		dbClient *db.Client
	}
	type args struct {
		ctx     context.Context
		request GetFlowByRuleRequest
	}
	dbClient := &db.Client{}
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "GetDevFlowRuleByProjectID", func(dbClient *db.Client, proID uint64) (fs *db.DevFlowRule, err error) {
		return &db.DevFlowRule{
			Flows: db.JSON(`[
    {
        "name":"PROD",
        "flowType":"single_branch",
        "targetBranch":"master,support/*",
        "changeFromBranch":"",
        "changeBranch":"",
        "enableAutoMerge":false,
        "autoMergeBranch":"",
        "artifact":"stable",
        "environment":"PROD",
        "startWorkflowHints":[

        ]
    },
    {
        "name":"STAGING",
        "flowType":"single_branch",
        "targetBranch":"release/*,hotfix/*",
        "changeFromBranch":"",
        "changeBranch":"",
        "enableAutoMerge":false,
        "autoMergeBranch":"",
        "artifact":"rc",
        "environment":"STAGING",
        "startWorkflowHints":[

        ]
    },
    {
        "name":"DEV",
        "flowType":"multi_branch",
        "targetBranch":"develop",
        "changeFromBranch":"develop",
        "changeBranch":"feat/*",
        "enableAutoMerge":false,
        "autoMergeBranch":"next_dev",
        "artifact":"alpha",
        "environment":"DEV",
        "startWorkflowHints":[
            {
                "place":"TASK",
                "changeBranchRule":"feat/*"
            }
        ]
    },
    {
        "name":"TEST",
        "flowType":"multi_branch",
        "targetBranch":"develop",
        "changeFromBranch":"develop",
        "changeBranch":"feature/*,bugfix/*",
        "enableAutoMerge":false,
        "autoMergeBranch":"next_test",
        "artifact":"alpha",
        "environment":"DEV",
        "startWorkflowHints":[
            {
                "place":"TASK",
                "changeBranchRule":"feature/*"
            },
            {
                "place":"BUG",
                "changeBranchRule":"bugfix/*"
            }
        ]
    }
]`),
		}, nil
	})
	defer monkey.UnpatchAll()
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    interface{}
		wantErr bool
	}{
		{
			name:   "test with GetFlowByRule1",
			fields: fields{dbClient: dbClient},
			args: args{
				ctx: context.Background(),
				request: GetFlowByRuleRequest{
					ProjectID:    1,
					FlowType:     "multi_branch",
					ChangeBranch: "feature/123",
					TargetBranch: "develop",
				},
			},
			want:    "next_test",
			wantErr: false,
		},
		{
			name:   "test with GetFlowByRule2",
			fields: fields{dbClient: dbClient},
			args: args{
				ctx: context.Background(),
				request: GetFlowByRuleRequest{
					ProjectID:    1,
					FlowType:     "multi_branch",
					ChangeBranch: "bugfix/123",
					TargetBranch: "develop",
				},
			},
			want:    "next_test",
			wantErr: false,
		},
		{
			name:   "test with GetFlowByRule3",
			fields: fields{dbClient: dbClient},
			args: args{
				ctx: context.Background(),
				request: GetFlowByRuleRequest{
					ProjectID:    1,
					FlowType:     "multi_branch",
					ChangeBranch: "feat/123",
					TargetBranch: "develop",
				},
			},
			want:    "next_dev",
			wantErr: false,
		},
		{
			name:   "test with nil1",
			fields: fields{dbClient: dbClient},
			args: args{
				ctx: context.Background(),
				request: GetFlowByRuleRequest{
					ProjectID:    1,
					FlowType:     "multi_branch",
					ChangeBranch: "feat/123",
					TargetBranch: "master",
				},
			},
			want:    nil,
			wantErr: false,
		},
		{
			name:   "test with nil2",
			fields: fields{dbClient: dbClient},
			args: args{
				ctx: context.Background(),
				request: GetFlowByRuleRequest{
					ProjectID:    1,
					FlowType:     "single_branch",
					ChangeBranch: "feat/123",
					TargetBranch: "master",
				},
			},
			want:    nil,
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				dbClient: tt.fields.dbClient,
			}
			got, err := p.GetFlowByRule(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetFlowByRule() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got == nil {
				if tt.want != nil {
					t.Errorf("fail")
				}
			} else if got.AutoMergeBranch != tt.want.(string) {
				t.Errorf("GetFlowByRule() got = %v, want %v", got.AutoMergeBranch, tt.want)
			}
		})
	}
}
