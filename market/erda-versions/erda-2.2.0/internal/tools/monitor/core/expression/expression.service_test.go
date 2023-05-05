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

package expression

import (
	"context"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/expression/pb"
	alertdb "github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/pkg/encoding/jsonmap"
)

func Test_expressionService_GetTemplates(t *testing.T) {
	type fields struct {
		p *provider
	}
	type args struct {
		ctx     context.Context
		request *pb.GetTemplatesRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *pb.GetTemplatesResponse
		wantErr bool
	}{
		{
			name: "test case",
			fields: fields{
				p: &provider{
					Cfg:               nil,
					Log:               nil,
					Register:          nil,
					t:                 &i18n.NopTranslator{},
					DB:                nil,
					expressionService: nil,
				},
			},
			args: args{
				ctx: nil,
				request: &pb.GetTemplatesRequest{
					PageSize: 1,
					PageNo:   1,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		var adb *alertdb.CustomizeAlertNotifyTemplateDB
		alertExpression := monkey.PatchInstanceMethod(reflect.TypeOf(adb), "QueryCustomizeAlertTemplate", func(db *alertdb.CustomizeAlertNotifyTemplateDB) ([]*alertdb.CustomizeAlertNotifyTemplate, error) {
			return []*alertdb.CustomizeAlertNotifyTemplate{
				{
					ID:               2,
					Name:             "2233",
					CustomizeAlertID: 1,
					AlertType:        "customize",
					AlertIndex:       "customize",
					Target:           "alert",
					Trigger:          "alert",
					Title:            "ssss",
					Template:         "sssss",
					Formats:          nil,
					Version:          "3.0",
					Enable:           false,
					CreateTime:       time.Time{},
					UpdateTime:       time.Time{},
				},
			}, nil
		})
		defer alertExpression.Unpatch()
		t.Run(tt.name, func(t *testing.T) {
			e := &expressionService{
				alertDB:                        &alertdb.AlertExpressionDB{&gorm.DB{}},
				metricDB:                       &alertdb.MetricExpressionDB{&gorm.DB{}},
				customizeAlertNotifyTemplateDB: &alertdb.CustomizeAlertNotifyTemplateDB{&gorm.DB{}},
			}
			_, err := e.GetTemplates(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetAllAlertTemplate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_expressionService_GetAlertExpressions(t *testing.T) {
	type fields struct {
		p *provider
	}
	type args struct {
		ctx     context.Context
		request *pb.GetExpressionsRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test case",
			fields: fields{
				p: &provider{
					Cfg:               nil,
					Log:               nil,
					Register:          nil,
					t:                 nil,
					DB:                nil,
					expressionService: nil,
				},
			},
			args: args{
				ctx: nil,
				request: &pb.GetExpressionsRequest{
					PageNo:   1,
					PageSize: 10,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var adb *alertdb.AlertExpressionDB
			alertExpression := monkey.PatchInstanceMethod(reflect.TypeOf(adb), "GetAllAlertExpression", func(db *alertdb.AlertExpressionDB, pageNo, pageSize int64) ([]*alertdb.AlertExpression, int64, error) {
				return []*alertdb.AlertExpression{
					{
						ID:         1,
						AlertID:    1,
						Attributes: jsonmap.JSONMap{"org_name": "erda"},
						Expression: jsonmap.JSONMap{"outputs": []string{"alert"}},
						Version:    "1.0",
						Enable:     true,
						Created:    time.Time{},
						Updated:    time.Time{},
					},
				}, 1, nil
			})
			defer alertExpression.Unpatch()
			e := &expressionService{
				alertDB:                        &alertdb.AlertExpressionDB{&gorm.DB{}},
				metricDB:                       &alertdb.MetricExpressionDB{&gorm.DB{}},
				customizeAlertNotifyTemplateDB: &alertdb.CustomizeAlertNotifyTemplateDB{&gorm.DB{}},
				alertNotifyDB:                  &alertdb.AlertNotifyDB{&gorm.DB{}},
			}
			_, err := e.GetAlertExpressions(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetAlertExpressions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_expressionService_GetMetricExpressions(t *testing.T) {
	type fields struct {
		p *provider
	}
	type args struct {
		ctx     context.Context
		request *pb.GetMetricExpressionsRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test case",
			fields: fields{
				p: &provider{
					Cfg:               nil,
					Log:               nil,
					Register:          nil,
					t:                 nil,
					DB:                nil,
					expressionService: nil,
				},
			},
			args: args{
				ctx: nil,
				request: &pb.GetMetricExpressionsRequest{
					PageSize: 10,
					PageNo:   1,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			e := &expressionService{
				alertDB:                        &alertdb.AlertExpressionDB{&gorm.DB{}},
				metricDB:                       &alertdb.MetricExpressionDB{&gorm.DB{}},
				customizeAlertNotifyTemplateDB: &alertdb.CustomizeAlertNotifyTemplateDB{&gorm.DB{}},
				alertNotifyDB:                  &alertdb.AlertNotifyDB{&gorm.DB{}},
			}
			_, err := e.GetMetricExpressions(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetMetricExpressions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_expressionService_GetAlertNotifies(t *testing.T) {
	type fields struct {
		p *provider
	}
	type args struct {
		ctx     context.Context
		request *pb.GetAlertNotifiesRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test case",
			fields: fields{
				p: &provider{
					Cfg:               nil,
					Log:               nil,
					Register:          nil,
					t:                 nil,
					DB:                nil,
					expressionService: nil,
				},
			},
			args: args{
				ctx: nil,
				request: &pb.GetAlertNotifiesRequest{
					PageSize: 10,
					PageNo:   1,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var ndb *alertdb.AlertNotifyDB
			alertNotify := monkey.PatchInstanceMethod(reflect.TypeOf(ndb), "QueryAlertNotify", func(db *alertdb.AlertNotifyDB, pageNo, pageSize int64) ([]*alertdb.AlertNotify, int64, error) {
				return []*alertdb.AlertNotify{
					{
						ID:             1,
						AlertID:        12,
						NotifyKey:      "233",
						NotifyTarget:   nil,
						NotifyTargetID: "222",
						Silence:        4,
						SilencePolicy:  "fixed",
						Enable:         true,
						Created:        time.Now(),
						Updated:        time.Now(),
					},
				}, 1, nil
			})
			defer alertNotify.Unpatch()
			e := &expressionService{
				alertDB:                        &alertdb.AlertExpressionDB{&gorm.DB{}},
				metricDB:                       &alertdb.MetricExpressionDB{&gorm.DB{}},
				customizeAlertNotifyTemplateDB: &alertdb.CustomizeAlertNotifyTemplateDB{&gorm.DB{}},
				alertNotifyDB:                  &alertdb.AlertNotifyDB{&gorm.DB{}},
			}
			_, err := e.GetAlertNotifies(tt.args.ctx, tt.args.request)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetAlertNotifies() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
