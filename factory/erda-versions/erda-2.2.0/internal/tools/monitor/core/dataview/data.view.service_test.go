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

package dataview

import (
	"context"
	"errors"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/core/monitor/dataview/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/dataview/db"
)

func Test_dataViewService_ListSystemViews(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ListSystemViewsRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ListSystemViewsResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.dataview.DataViewService",
		// 			`
		// erda.core.monitor.dataview:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.ListSystemViewsRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.ListSystemViewsResponse{
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
			srv := hub.Service(tt.service).(pb.DataViewServiceServer)
			got, err := srv.ListSystemViews(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("dataViewService.ListSystemViews() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("dataViewService.ListSystemViews() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_dataViewService_GetSystemView(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetSystemViewRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetSystemViewResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.dataview.DataViewService",
		// 			`
		// erda.core.monitor.dataview:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.GetSystemViewRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.GetSystemViewResponse{
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
			srv := hub.Service(tt.service).(pb.DataViewServiceServer)
			got, err := srv.GetSystemView(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("dataViewService.GetSystemView() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("dataViewService.GetSystemView() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_dataViewService_CreateCustomView(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CreateCustomViewRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.CreateCustomViewResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.dataview.DataViewService",
		// 			`
		// erda.core.monitor.dataview:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.CreateCustomViewRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.CreateCustomViewResponse{
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
			srv := hub.Service(tt.service).(pb.DataViewServiceServer)
			got, err := srv.CreateCustomView(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("dataViewService.CreateCustomView() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("dataViewService.CreateCustomView() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_dataViewService_GetCustomView(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetCustomViewRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetCustomViewResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.dataview.DataViewService",
		// 			`
		// erda.core.monitor.dataview:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.GetCustomViewRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.GetCustomViewResponse{
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
			srv := hub.Service(tt.service).(pb.DataViewServiceServer)
			got, err := srv.GetCustomView(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("dataViewService.GetCustomView() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("dataViewService.GetCustomView() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_dataViewService_UpdateCustomView(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.UpdateCustomViewRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.UpdateCustomViewResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.dataview.DataViewService",
		// 			`
		// erda.core.monitor.dataview:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.UpdateCustomViewRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.UpdateCustomViewResponse{
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
			srv := hub.Service(tt.service).(pb.DataViewServiceServer)
			got, err := srv.UpdateCustomView(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("dataViewService.UpdateCustomView() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("dataViewService.UpdateCustomView() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_dataViewService_DeleteCustomView(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.DeleteCustomViewRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.DeleteCustomViewResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.dataview.DataViewService",
		// 			`
		// erda.core.monitor.dataview:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.DeleteCustomViewRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.DeleteCustomViewResponse{
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
			srv := hub.Service(tt.service).(pb.DataViewServiceServer)
			got, err := srv.DeleteCustomView(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("dataViewService.DeleteCustomView() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("dataViewService.DeleteCustomView() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_getScopeScopeID(t *testing.T) {
	type args struct {
		view map[string]interface{}
	}
	tests := []struct {
		name  string
		args  args
		want  string
		want1 string
	}{
		{
			name: "normal",
			args: args{
				view: map[string]interface{}{
					"scope":   "org",
					"scopeId": "xxx",
				},
			},
			want:  "org",
			want1: "xxx",
		},
		{
			name: "no found",
			args: args{
				view: map[string]interface{}{},
			},
			want:  "",
			want1: "",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1 := getScopeScopeID(tt.args.view)
			if got != tt.want {
				t.Errorf("getScopeScopeID() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("getScopeScopeID() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}

func Test_updateFields(t *testing.T) {
	tests := []struct {
		name string
		req  *pb.UpdateCustomViewRequest
	}{
		{
			req: &pb.UpdateCustomViewRequest{
				Id:         "test-id",
				Name:       "test-name",
				Desc:       "test-desc",
				UpdateType: pb.UpdateType_ViewType.String(),
				Blocks:     nil,
				Data:       nil,
			},
		},
		{
			req: &pb.UpdateCustomViewRequest{
				Id:         "test-id",
				Name:       "test-name",
				Desc:       "test-desc",
				UpdateType: pb.UpdateType_MetaType.String(),
				Blocks:     nil,
				Data:       nil,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := fieldsForUpdate(tt.req)
			if tt.req.UpdateType == pb.UpdateType_ViewType.String() {
				if _, ok := got["UpdatedAt"]; !ok {
					t.Errorf("fieldsForUpdate() = %v, must contains UpdatedAt", got)
				}
				if _, ok := got["ViewConfig"]; !ok {
					t.Errorf("fieldsForUpdate() = %v, must contains ViewConfig", got)
				}
				if _, ok := got["DataConfig"]; !ok {
					t.Errorf("fieldsForUpdate() = %v, must contains DataConfig", got)
				}
			} else if tt.req.UpdateType == pb.UpdateType_MetaType.String() {
				if _, ok := got["Name"]; !ok {
					t.Errorf("fieldsForUpdate() = %v, must contains Name", got)
				} else if _, ok := got["Desc"]; !ok {
					t.Errorf("fieldsForUpdate() = %v, must contains Desc", got)
				}
			}
		})
	}
}

func Test_fieldsForUpdate(t *testing.T) {
	type args struct {
		req *pb.UpdateCustomViewRequest
	}
	tests := []struct {
		name string
		args args
		want map[string]interface{}
	}{
		{
			name: "case1",
			args: args{req: &pb.UpdateCustomViewRequest{UpdateType: pb.UpdateType_MetaType.String(), Name: "test", Desc: "test"}},
			want: map[string]interface{}{"Name": "test", "Desc": "test"},
		},
		{
			name: "case2",
			args: args{req: &pb.UpdateCustomViewRequest{UpdateType: pb.UpdateType_ViewType.String(), Name: "test", Desc: "test", Blocks: []*pb.Block{{W: 10, H: 10}}}},
			want: map[string]interface{}{"ViewConfig": []*pb.Block{{W: 10, H: 10}}, "Desc": "test"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := fieldsForUpdate(tt.args.req)
			if tt.args.req.UpdateType == pb.UpdateType_MetaType.String() {
				if v, ok := got["Name"]; !ok && v != tt.want["Name"] {
					t.Errorf("fieldsForUpdate() = %v, want %v", got, tt.want)
				}
				if v, ok := got["Desc"]; !ok && v != tt.want["Desc"] {
					t.Errorf("fieldsForUpdate() = %v, want %v", got, tt.want)
				}
			} else if tt.args.req.UpdateType == pb.UpdateType_ViewType.String() {
				if v, ok := got["ViewConfig"]; !ok && v != tt.want["ViewConfig"] {
					t.Errorf("fieldsForUpdate() = %v, want %v", got, tt.want)
				}
				if v, ok := got["DataConfig"]; !ok && v != tt.want["DataConfig"] {
					t.Errorf("fieldsForUpdate() = %v, want %v", got, tt.want)
				}
			}
		})
	}
}

func Test_dataViewService_ListCustomViews(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ListCustomViewsRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.ListCustomViewsRequest{
			Scope:       "test",
			ScopeID:     "test",
			Name:        "error",
			Description: "test",
			CreatorId:   []string{"1", "2"},
		}}, true},
		{"case2", args{req: &pb.ListCustomViewsRequest{
			Scope:       "test",
			ScopeID:     "test",
			Name:        "test",
			Description: "test",
			CreatorId:   []string{"1", "2"},
		}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var cvdb db.CustomViewDB
			monkey.PatchInstanceMethod(reflect.TypeOf(&cvdb), "ListByFieldsAndPage", func(cvdb *db.CustomViewDB, pageNo, pageSize int64, startTime, endTime int64, creatorId []string, fields map[string]interface{}, likeFields map[string]interface{}) ([]*db.CustomView, int64, error) {
				if v, ok := likeFields["Name"]; !ok || v == "error" {
					return nil, 0, errors.New("error")
				}
				return []*db.CustomView{{CreatorID: "1", Scope: "test", ScopeID: "test", Name: "test1", Desc: "test1"}, {CreatorID: "2", Scope: "test", ScopeID: "test", Name: "test2", Desc: "test2"}}, 2, nil
			})

			s := &dataViewService{}
			_, err := s.ListCustomViews(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("ListCustomViews() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
