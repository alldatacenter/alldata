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
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/internal/tools/monitor/core/dataview/db"
	"github.com/erda-project/erda/pkg/mock"
)

func Test_provider_ExportTaskExecutor(t *testing.T) {
	type fields struct {
		ExportChannel chan string
	}
	tests := []struct {
		name   string
		fields fields
	}{
		{"case1", fields{ExportChannel: make(chan string, 1)}},
		{"case2", fields{ExportChannel: make(chan string)}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				ExportChannel: tt.fields.ExportChannel,
			}
			p.ExportTaskExecutor(time.Second * time.Duration(1))

			time.Sleep(time.Second * time.Duration(2))
		})
	}
}

func Test_provider_Init(t *testing.T) {
	type fields struct {
		Cfg             *config
		Log             logs.Logger
		Register        transport.Register
		DB              *gorm.DB
		dataViewService *dataViewService
		Tran            i18n.Translator
		bdl             *bundle.Bundle
		audit           audit.Auditor
		sys             *db.SystemViewDB
		custom          *db.CustomViewDB
		history         *db.ErdaDashboardHistoryDB
		ExportChannel   chan string
	}
	type args struct {
		ctx servicehub.Context
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{"case1", fields{
			DB:  &gorm.DB{},
			Log: mock.NewMockLogger(gomock.NewController(t)),
		}, args{ctx: mock.NewMockContext(gomock.NewController(t))}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			p := &provider{
				Cfg:             tt.fields.Cfg,
				Log:             tt.fields.Log,
				Register:        tt.fields.Register,
				DB:              tt.fields.DB,
				dataViewService: tt.fields.dataViewService,
				Tran:            tt.fields.Tran,
				bdl:             tt.fields.bdl,
				audit:           tt.fields.audit,
				sys:             tt.fields.sys,
				custom:          tt.fields.custom,
				history:         tt.fields.history,
				ExportChannel:   tt.fields.ExportChannel,
			}
			type (
				Tables struct {
					SystemBlock string `file:"system_block" default:"sp_dashboard_block_system"`
					UserBlock   string `file:"user_block" default:"sp_dashboard_block"`
				}
			)
			p.Cfg = &config{Tables: Tables{}}

			monkey.Patch(audit.GetAuditor, func(ctx servicehub.Context) audit.Auditor {
				return &audit.NopAuditor{}
			})
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()
			router := mock.NewMockRouter(ctrl)
			var r *mock.MockRouter
			var ctx *mock.MockContext
			monkey.PatchInstanceMethod(reflect.TypeOf(ctx), "Service", func(ctx *mock.MockContext, name string, options ...interface{}) interface{} {
				return router
			})

			monkey.PatchInstanceMethod(reflect.TypeOf(r), "POST", func(r *mock.MockRouter, path string, handler interface{}, options ...interface{}) {
				return
			})

			if err := p.Init(tt.args.ctx); (err != nil) != tt.wantErr {
				t.Errorf("Init() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
