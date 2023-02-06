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

package errorbox

import (
	context "context"
	reflect "reflect"
	testing "testing"

	servicehub "github.com/erda-project/erda-infra/base/servicehub"
	pb1 "github.com/erda-project/erda-proto-go/core/dop/taskerror/pb"
	pb "github.com/erda-project/erda-proto-go/core/services/errorbox/pb"
)

func Test_errorBoxService_ListErrorLog(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.TaskErrorListRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb1.ErrorLogListResponseData
		wantErr  bool
	}{
		// TODO: Add test cases.
		{
			"case 1",
			"erda.core.services.errorbox.ErrorBoxService",
			`
erda.core.services.errorbox:
`,
			args{
				context.TODO(),
				&pb.TaskErrorListRequest{
					// TODO: setup fields
				},
			},
			nil,
			true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				return
			}
			srv := hub.Service(tt.service).(pb.ErrorBoxServiceServer)
			got, err := srv.ListErrorLog(tt.args.ctx, tt.args.req)
			if !tt.wantErr && (err != nil) {
				t.Errorf("ErrorBoxService.ListErrorLog() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("ErrorBoxService.ListErrorLog() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}
