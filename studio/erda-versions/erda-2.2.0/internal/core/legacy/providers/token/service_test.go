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

package token

import (
	"testing"

	"github.com/erda-project/erda-proto-go/core/token/pb"
)

func TestToModelToken(t *testing.T) {
	type args struct {
		userID string
		req    *pb.CreateTokenRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			args: args{
				req: &pb.CreateTokenRequest{
					Type: "AccessKey",
				},
				userID: "2",
			},
			wantErr: false,
		},
		{
			args: args{
				req: &pb.CreateTokenRequest{
					Type:  "PAT",
					Scope: "org",
				},
			},
			wantErr: false,
		},
		{
			args: args{
				req: &pb.CreateTokenRequest{
					Type: "PAT",
				},
			},
			wantErr: true,
		},
		{
			args: args{
				req: &pb.CreateTokenRequest{},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := ToModelToken(tt.args.userID, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("ToModelToken() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
