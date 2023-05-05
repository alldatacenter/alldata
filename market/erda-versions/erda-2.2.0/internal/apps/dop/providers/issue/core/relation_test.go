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

package core

import (
	"testing"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
)

func TestCheck(t *testing.T) {
	type args struct {
		irc *pb.AddIssueRelationRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			args: args{
				irc: &pb.AddIssueRelationRequest{
					IssueID:       1,
					ProjectId:     1,
					Type:          "inclusion",
					RelatedIssues: []uint64{2, 3},
				},
			},
		},
		{
			args: args{
				irc: &pb.AddIssueRelationRequest{
					IssueID:       1,
					ProjectId:     1,
					Type:          "inclusion",
					RelatedIssues: []uint64{1, 2, 3},
				},
			},
			wantErr: true,
		},
		{
			args: args{
				irc: &pb.AddIssueRelationRequest{
					ProjectId:     1,
					Type:          "inclusion",
					RelatedIssues: []uint64{2, 3},
				},
			},
			wantErr: true,
		},
		{
			args: args{
				irc: &pb.AddIssueRelationRequest{
					IssueID:   1,
					ProjectId: 1,
					Type:      "inclusion",
				},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := Check(tt.args.irc); (err != nil) != tt.wantErr {
				t.Errorf("Check() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
