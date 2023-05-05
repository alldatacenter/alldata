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

package edgereporter

import (
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgereporter/db"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func Test_pipelineFilterIn(t *testing.T) {
	type args struct {
		pipelines []spec.PipelineBase
		fn        func(p *spec.PipelineBase) bool
	}
	tests := []struct {
		name string
		args args
		want []spec.PipelineBase
	}{
		{
			name: "",
			args: args{
				pipelines: []spec.PipelineBase{
					{
						ID:     1,
						Status: "Success",
					},
					{
						ID:     2,
						Status: "Running",
					},
					{
						ID:     3,
						Status: "Analyzed",
					},
					{
						ID:     4,
						Status: "Failed",
					},
				},
				fn: func(p *spec.PipelineBase) bool {
					return p.Status.IsEndStatus()
				},
			},
			want: []spec.PipelineBase{
				{
					ID:     1,
					Status: "Success",
				},
				{
					ID:     4,
					Status: "Failed",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := pipelineFilterIn(tt.args.pipelines, tt.args.fn); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("pipelineFilterIn() = %v, want %v", got, tt.want)
			}
		})
	}
}

////go:generate mockgen -destination=./reporter_logs_test.go -package edgereporter github.com/erda-project/erda-infra/base/logs Logger
func Test_provider_doTaskReporter(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	logger := NewMockLogger(ctrl)

	logger.EXPECT().Infof("begin do task report, taskID: %d", uint64(1)).Return()

	var (
		bdl      *bundle.Bundle
		dbClient *dbclient.Client
	)
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "PipelineCallback",
		func(_ *bundle.Bundle, _ apistructs.PipelineCallbackRequest, openapiAddr, token string) error {
			return nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "GetPipelineTask",
		func(_ *dbclient.Client, _ interface{}) (spec.PipelineTask, error) {
			return spec.PipelineTask{ID: 1}, nil
		})
	defer monkey.UnpatchAll()

	type fields struct {
		bdl      *bundle.Bundle
		dbClient *db.Client
		Log      logs.Logger
		config   *config
	}
	type args struct {
		ctx    context.Context
		taskID uint64
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test doTaskReporter",
			fields: fields{
				bdl:      bdl,
				dbClient: &db.Client{Client: dbClient},
				Log:      logger,
				config:   &config{},
			},
			args: args{
				ctx:    context.Background(),
				taskID: 1,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				bdl:          tt.fields.bdl,
				dbClient:     tt.fields.dbClient,
				Log:          tt.fields.Log,
				Cfg:          tt.fields.config,
				EdgeRegister: &edgepipeline_register.MockEdgeRegister{},
			}
			if err := p.doTaskReporter(tt.args.ctx, tt.args.taskID); (err != nil) != tt.wantErr {
				t.Errorf("doTaskReporter() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_provider_doPipelineReporter(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	logger := NewMockLogger(ctrl)

	logger.EXPECT().Infof("begin do pipeline report, pipelineID: %d", uint64(1)).Return()

	var (
		bdl      *bundle.Bundle
		dbClient *dbclient.Client
		DB       *db.Client
	)
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "PipelineCallback",
		func(_ *bundle.Bundle, _ apistructs.PipelineCallbackRequest, openapiAddr, token string) error {
			return nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "GetPipeline",
		func(_ *dbclient.Client, _ interface{}, _ ...dbclient.SessionOption) (spec.Pipeline, error) {
			return spec.Pipeline{}, nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "ListPipelineStageByPipelineID",
		func(_ *dbclient.Client, _ uint64, _ ...dbclient.SessionOption) ([]spec.PipelineStage, error) {
			return []spec.PipelineStage{}, nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "ListPipelineTasksByPipelineID",
		func(_ *dbclient.Client, _ uint64, _ ...dbclient.SessionOption) ([]spec.PipelineTask, error) {
			return []spec.PipelineTask{}, nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(DB), "UpdatePipelineEdgeReportStatus",
		func(_ *db.Client, _ uint64, _ apistructs.EdgeReportStatus) error {
			return nil
		})
	defer monkey.UnpatchAll()

	type fields struct {
		bdl      *bundle.Bundle
		dbClient *db.Client
		Cfg      *config
		Log      logs.Logger
	}
	type args struct {
		ctx        context.Context
		pipelineID uint64
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test doPipelineReporter",
			fields: fields{
				bdl:      bdl,
				dbClient: &db.Client{Client: dbClient},
				Cfg:      &config{},
				Log:      logger,
			},
			args: args{
				ctx:        context.Background(),
				pipelineID: 1,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				bdl:          tt.fields.bdl,
				dbClient:     tt.fields.dbClient,
				Cfg:          tt.fields.Cfg,
				Log:          tt.fields.Log,
				EdgeRegister: &edgepipeline_register.MockEdgeRegister{},
			}
			if err := p.doPipelineReporter(tt.args.ctx, tt.args.pipelineID); (err != nil) != tt.wantErr {
				t.Errorf("doPipelineReporter() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
