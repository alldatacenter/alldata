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

package notify

import (
	"context"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/jinzhu/gorm"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/internal/core/messenger/notify/db"
	"github.com/erda-project/erda/internal/core/messenger/notify/model"
	"github.com/erda-project/erda/pkg/common/apis"
)

func Test_notifyService_CreateNotifyHistory(t *testing.T) {
	type fields struct {
		DB *db.DB
		L  logs.Logger
	}
	type args struct {
		ctx     context.Context
		request *pb.CreateNotifyHistoryRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test",
			fields: fields{
				DB: &db.DB{
					DB: &gorm.DB{},
					AlertNotifyIndexDB: db.AlertNotifyIndexDB{
						DB: &gorm.DB{},
					},
					NotifyHistoryDB: db.NotifyHistoryDB{
						DB: &gorm.DB{},
					},
				},
				L: nil,
			},
			args: args{
				ctx: context.Background(),
				request: &pb.CreateNotifyHistoryRequest{
					NotifyName:            "test",
					NotifyItemDisplayName: "test",
					Channel:               "mbox",
					NotifyTargets:         []*pb.NotifyTarget{},
					NotifySource:          &pb.NotifySource{},
					Status:                "success",
					ErrorMsg:              "",
					OrgID:                 1,
					NotifyTags: map[string]*structpb.Value{
						"alertId": structpb.NewNumberValue(float64(3)),
					},
					Label:       "",
					ClusterName: "",
				},
			},
		},
	}
	for _, tt := range tests {
		ns := notifyService{}
		defer monkey.UnpatchAll()
		monkey.PatchInstanceMethod(reflect.TypeOf(ns), "CreateHistoryAndIndex", func(_ notifyService, request *pb.CreateNotifyHistoryRequest) (historyId int64, err error) {
			return 3, nil
		})
		monkey.PatchInstanceMethod(reflect.TypeOf(&db.NotifyHistoryDB{}), "CreateNotifyHistory", func(_ *db.NotifyHistoryDB, request *db.NotifyHistory) (*db.NotifyHistory, error) {
			return &db.NotifyHistory{
				BaseModel:             model.BaseModel{},
				NotifyName:            "sss",
				NotifyItemDisplayName: "sss",
				Channel:               "mbox",
				TargetData:            "ssssss",
				SourceData:            "ssssss",
				Status:                "success",
				OrgID:                 3,
				SourceType:            "micro_service",
				SourceID:              "sdgiw-u9gt-sodpsdl",
				ErrorMsg:              "",
				Label:                 "",
				ClusterName:           "",
			}, nil
		})
		t.Run(tt.name, func(t *testing.T) {
			n := notifyService{
				DB: tt.fields.DB,
				L:  tt.fields.L,
			}
			_, err := n.CreateNotifyHistory(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("CreateNotifyHistory() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyService_CreateHistoryAndIndex(t *testing.T) {
	type fields struct {
		DB *db.DB
		L  logs.Logger
	}
	type args struct {
		request *pb.CreateNotifyHistoryRequest
	}
	tests := []struct {
		name          string
		fields        fields
		args          args
		wantHistoryId int64
	}{
		{
			name: "test",
			fields: fields{
				DB: &db.DB{
					DB: &gorm.DB{},
					AlertNotifyIndexDB: db.AlertNotifyIndexDB{
						DB: &gorm.DB{},
					},
					NotifyHistoryDB: db.NotifyHistoryDB{
						DB: &gorm.DB{},
					},
				},
				L: nil,
			},
			args: args{
				request: &pb.CreateNotifyHistoryRequest{
					NotifyName:            "test",
					NotifyItemDisplayName: "test",
					Channel:               "mbox",
					NotifyTargets:         []*pb.NotifyTarget{},
					NotifySource:          &pb.NotifySource{},
					Status:                "success",
					ErrorMsg:              "",
					OrgID:                 1,
					NotifyTags: map[string]*structpb.Value{
						"alertId": structpb.NewNumberValue(float64(2)),
					},
					Label:       "",
					ClusterName: "",
				},
			},
			wantHistoryId: 3,
		},
	}
	for _, tt := range tests {
		ns := notifyService{}
		defer monkey.UnpatchAll()
		monkey.PatchInstanceMethod(reflect.TypeOf(ns), "CreateHistoryAndIndex", func(_ notifyService, request *pb.CreateNotifyHistoryRequest) (historyId int64, err error) {
			return 3, nil
		})
		monkey.PatchInstanceMethod(reflect.TypeOf(&db.NotifyHistoryDB{}), "CreateNotifyHistory", func(_ *db.NotifyHistoryDB, request *db.NotifyHistory) (*db.NotifyHistory, error) {
			return &db.NotifyHistory{
				BaseModel:             model.BaseModel{},
				NotifyName:            "sss",
				NotifyItemDisplayName: "sss",
				Channel:               "mbox",
				TargetData:            "ssssss",
				SourceData:            "ssssss",
				Status:                "success",
				OrgID:                 3,
				SourceType:            "micro_service",
				SourceID:              "sdgiw-u9gt-sodpsdl",
				ErrorMsg:              "",
				Label:                 "",
				ClusterName:           "",
			}, nil
		})
		t.Run(tt.name, func(t *testing.T) {
			n := notifyService{
				DB: tt.fields.DB,
				L:  tt.fields.L,
			}
			gotHistoryId, err := n.CreateHistoryAndIndex(tt.args.request)
			if err != nil {
				t.Errorf("CreateHistoryAndIndex() error = %v", err)
				return
			}
			if gotHistoryId != tt.wantHistoryId {
				t.Errorf("CreateHistoryAndIndex() gotHistoryId = %v, want %v", gotHistoryId, tt.wantHistoryId)
			}
		})
	}
}

func Test_notifyService_QueryNotifyHistories(t *testing.T) {
	type fields struct {
		DB *db.DB
		L  logs.Logger
	}
	type args struct {
		ctx     context.Context
		request *pb.QueryNotifyHistoriesRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *pb.QueryNotifyHistoriesResponse
		wantErr bool
	}{
		{
			name: "test",
			fields: fields{
				DB: &db.DB{
					DB: &gorm.DB{},
					AlertNotifyIndexDB: db.AlertNotifyIndexDB{
						DB: &gorm.DB{},
					},
					NotifyHistoryDB: db.NotifyHistoryDB{
						DB: &gorm.DB{},
					},
				},
				L: nil,
			},
			args: args{
				ctx: context.Background(),
				request: &pb.QueryNotifyHistoriesRequest{
					PageNo:      1,
					PageSize:    10,
					Channel:     "mbox",
					NotifyName:  "sss",
					StartTime:   "1640575064106",
					EndTime:     "1643253464106",
					Label:       "",
					ClusterName: "",
					OrgID:       1,
				},
			},
		},
	}
	for _, tt := range tests {
		monkey.Patch(apis.GetOrgID, func(ctx context.Context) string {
			return "1"
		})
		defer monkey.UnpatchAll()
		monkey.PatchInstanceMethod(reflect.TypeOf(&db.NotifyHistoryDB{}), "QueryNotifyHistories", func(_ *db.NotifyHistoryDB, request *model.QueryNotifyHistoriesRequest) ([]db.NotifyHistory, int64, error) {
			return []db.NotifyHistory{
				{
					BaseModel:             model.BaseModel{},
					NotifyName:            "test",
					NotifyItemDisplayName: "test",
					Channel:               "mbox",
					TargetData:            "this is test",
					SourceData:            "this is test",
					Status:                "success",
					OrgID:                 1,
					SourceType:            "micro_service",
					SourceID:              "93ty9sohfovspdncm",
					ErrorMsg:              "",
					Label:                 "",
					ClusterName:           "",
				},
			}, 1, nil
		})
		t.Run(tt.name, func(t *testing.T) {
			n := notifyService{
				DB: tt.fields.DB,
				L:  tt.fields.L,
			}
			_, err := n.QueryNotifyHistories(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("QueryNotifyHistories() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyService_GetNotifyStatus(t *testing.T) {
	type fields struct {
		DB *db.DB
		L  logs.Logger
	}
	type args struct {
		ctx     context.Context
		request *pb.GetNotifyStatusRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *pb.GetNotifyStatusResponse
		wantErr bool
	}{
		{
			name: "test",
			fields: fields{
				DB: &db.DB{
					DB: &gorm.DB{},
					AlertNotifyIndexDB: db.AlertNotifyIndexDB{
						DB: &gorm.DB{},
					},
					NotifyHistoryDB: db.NotifyHistoryDB{
						DB: &gorm.DB{},
					},
				},
				L: nil,
			},
			args: args{
				ctx: context.Background(),
				request: &pb.GetNotifyStatusRequest{
					StartTime: "1640575064106",
					EndTime:   "1643253464106",
					ScopeType: "micro_service",
					ScopeId:   "fjew9gh0wd-fpfmdwsd-f",
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		monkey.Patch(apis.GetOrgID, func(ctx context.Context) string {
			return "1"
		})
		monkey.PatchInstanceMethod(reflect.TypeOf(&db.NotifyHistoryDB{}), "FilterStatus", func(_ *db.NotifyHistoryDB, request *model.FilterStatusRequest) ([]*model.FilterStatusResult, error) {
			return []*model.FilterStatusResult{
				{
					Status: "success",
					Count:  13,
				},
				{
					Status: "failed",
					Count:  3,
				},
			}, nil
		})
		t.Run(tt.name, func(t *testing.T) {
			n := notifyService{
				DB: tt.fields.DB,
				L:  tt.fields.L,
			}
			_, err := n.GetNotifyStatus(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNotifyStatus() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyService_GetNotifyHistogram(t *testing.T) {
	type fields struct {
		DB *db.DB
		L  logs.Logger
	}
	type args struct {
		ctx     context.Context
		request *pb.GetNotifyHistogramRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *pb.GetNotifyHistogramResponse
		wantErr bool
	}{
		{
			name: "test",
			fields: fields{
				DB: &db.DB{
					DB: &gorm.DB{},
					AlertNotifyIndexDB: db.AlertNotifyIndexDB{
						DB: &gorm.DB{},
					},
					NotifyHistoryDB: db.NotifyHistoryDB{
						DB: &gorm.DB{},
					},
				},
				L: nil,
			},
			args: args{
				ctx: context.Background(),
				request: &pb.GetNotifyHistogramRequest{
					StartTime: "1642678216000",
					EndTime:   "1642681816000",
					ScopeId:   "1",
					Points:    30,
					Statistic: "channel",
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		monkey.Patch(apis.GetOrgID, func(ctx context.Context) string {
			return "1"
		})
		monkey.PatchInstanceMethod(reflect.TypeOf(&db.NotifyHistoryDB{}), "QueryNotifyValue", func(_ *db.NotifyHistoryDB, key string, orgId int, scopeType, scopeId string, interval, startTime, endTime int64) ([]*model.NotifyValue, error) {
			return []*model.NotifyValue{
				{
					Field:     "mbox",
					Count:     1,
					RoundTime: time.Now(),
				},
			}, nil
		})
		t.Run(tt.name, func(t *testing.T) {
			n := notifyService{
				DB: tt.fields.DB,
				L:  tt.fields.L,
			}
			_, err := n.GetNotifyHistogram(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetNotifyHistogram() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_notifyService_QueryAlertNotifyHistories(t *testing.T) {
	type fields struct {
		DB *db.DB
		L  logs.Logger
	}
	type args struct {
		ctx     context.Context
		request *pb.QueryAlertNotifyHistoriesRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *pb.QueryAlertNotifyHistoriesResponse
		wantErr bool
	}{
		{
			name: "test",
			fields: fields{
				DB: &db.DB{
					DB: &gorm.DB{},
					AlertNotifyIndexDB: db.AlertNotifyIndexDB{
						DB: &gorm.DB{},
					},
					NotifyHistoryDB: db.NotifyHistoryDB{
						DB: &gorm.DB{},
					},
				},
				L: nil,
			},
			args: args{
				ctx: context.Background(),
				request: &pb.QueryAlertNotifyHistoriesRequest{
					ScopeType:  "org",
					ScopeID:    "1",
					NotifyName: "",
					Status:     "success",
					Channel:    "mbox",
					AlertID:    0,
					PageNo:     1,
					PageSize:   20,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		monkey.Patch(apis.GetOrgID, func(ctx context.Context) string {
			return "1"
		})
		monkey.PatchInstanceMethod(reflect.TypeOf(&db.AlertNotifyIndexDB{}), "QueryAlertNotifyHistories", func(_ *db.AlertNotifyIndexDB, request *model.QueryAlertNotifyIndexRequest) ([]db.AlertNotifyIndex, int64, error) {
			return []db.AlertNotifyIndex{
				{
					ID:         1,
					NotifyID:   1,
					NotifyName: "test",
					Status:     "success",
					Channel:    "mbox",
					ScopeType:  "org",
					ScopeID:    "1",
					OrgID:      1,
					CreatedAt:  time.Now(),
					SendTime:   time.Now(),
				},
			}, 1, nil
		})
		t.Run(tt.name, func(t *testing.T) {
			n := notifyService{
				DB: tt.fields.DB,
				L:  tt.fields.L,
			}
			_, err := n.QueryAlertNotifyHistories(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("QueryAlertNotifyHistories() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
