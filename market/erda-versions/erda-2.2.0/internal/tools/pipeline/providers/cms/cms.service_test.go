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

package cms

import (
	"context"
	"reflect"
	"testing"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
)

func Test_cmsService_CreateNs(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CmsCreateNsRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.CmsCreateNsResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.pipeline.cms.CmsService",
		//			`
		//erda.core.pipeline.cms:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.CmsCreateNsRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.CmsCreateNsResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
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
			srv := hub.Service(tt.service).(pb.CmsServiceServer)
			got, err := srv.CreateNs(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("cmsService.CreateNs() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("cmsService.CreateNs() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_cmsService_ListCmsNs(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CmsListNsRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.CmsListNsResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.pipeline.cms.CmsService",
		//			`
		//erda.core.pipeline.cms:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.CmsListNsRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.CmsListNsResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
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
			srv := hub.Service(tt.service).(pb.CmsServiceServer)
			got, err := srv.ListCmsNs(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("cmsService.ListCmsNs() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("cmsService.ListCmsNs() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_cmsService_UpdateCmsNsConfigs(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CmsNsConfigsUpdateRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.CmsNsConfigsUpdateResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.pipeline.cms.CmsService",
		//			`
		//erda.core.pipeline.cms:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.CmsNsConfigsUpdateRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.CmsNsConfigsUpdateResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
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
			srv := hub.Service(tt.service).(pb.CmsServiceServer)
			got, err := srv.UpdateCmsNsConfigs(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("cmsService.UpdateCmsNsConfigs() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("cmsService.UpdateCmsNsConfigs() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_cmsService_DeleteCmsNsConfigs(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CmsNsConfigsDeleteRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.CmsNsConfigsDeleteResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.pipeline.cms.CmsService",
		//			`
		//erda.core.pipeline.cms:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.CmsNsConfigsDeleteRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.CmsNsConfigsDeleteResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
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
			srv := hub.Service(tt.service).(pb.CmsServiceServer)
			got, err := srv.DeleteCmsNsConfigs(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("cmsService.DeleteCmsNsConfigs() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("cmsService.DeleteCmsNsConfigs() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_cmsService_GetCmsNsConfigs(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CmsNsConfigsGetRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.CmsNsConfigsGetResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.core.pipeline.cms.CmsService",
		//			`
		//erda.core.pipeline.cms:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.CmsNsConfigsGetRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.CmsNsConfigsGetResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
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
			srv := hub.Service(tt.service).(pb.CmsServiceServer)
			got, err := srv.GetCmsNsConfigs(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("cmsService.GetCmsNsConfigs() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("cmsService.GetCmsNsConfigs() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}
