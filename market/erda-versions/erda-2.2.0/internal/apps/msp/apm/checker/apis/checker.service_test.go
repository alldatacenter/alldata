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

package apis

import (
	"context"
	"reflect"
	"testing"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
)

func Test_checkerService_CreateChecker(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CreateCheckerRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.CreateCheckerResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.msp.apm.checker.CheckerService",
		// 			`
		// erda.msp.apm.checker:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.CreateCheckerRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.CreateCheckerResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.CheckerServiceServer)
			got, err := srv.CreateChecker(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("checkerService.CreateChecker() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("checkerService.CreateChecker() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_checkerService_UpdateChecker(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.UpdateCheckerRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.UpdateCheckerResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.msp.apm.checker.CheckerService",
		// 			`
		// erda.msp.apm.checker:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.UpdateCheckerRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.UpdateCheckerResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.CheckerServiceServer)
			got, err := srv.UpdateChecker(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("checkerService.UpdateChecker() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("checkerService.UpdateChecker() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_checkerService_DeleteChecker(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.UpdateCheckerRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.UpdateCheckerResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.msp.apm.checker.CheckerService",
		// 			`
		// erda.msp.apm.checker:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.UpdateCheckerRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.UpdateCheckerResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.CheckerServiceServer)
			got, err := srv.DeleteChecker(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("checkerService.DeleteChecker() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("checkerService.DeleteChecker() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_checkerService_ListCheckers(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ListCheckersRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ListCheckersResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.msp.apm.checker.CheckerService",
		// 			`
		// erda.msp.apm.checker:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.ListCheckersRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.ListCheckersResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.CheckerServiceServer)
			got, err := srv.ListCheckers(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("checkerService.ListCheckers() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("checkerService.ListCheckers() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_checkerService_DescribeCheckers(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.DescribeCheckersRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.DescribeCheckersResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.msp.apm.checker.CheckerService",
		// 			`
		// erda.msp.apm.checker:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.DescribeCheckersRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.DescribeCheckersResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.CheckerServiceServer)
			got, err := srv.DescribeCheckers(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("checkerService.DescribeCheckers() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("checkerService.DescribeCheckers() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_checkerService_DescribeChecker(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.DescribeCheckerRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.DescribeCheckerResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.msp.apm.checker.CheckerService",
		// 			`
		// erda.msp.apm.checker:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.DescribeCheckerRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.DescribeCheckerResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.CheckerServiceServer)
			got, err := srv.DescribeChecker(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("checkerService.DescribeChecker() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("checkerService.DescribeChecker() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}
