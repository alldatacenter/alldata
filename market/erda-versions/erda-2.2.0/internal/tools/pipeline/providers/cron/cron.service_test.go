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

package cron

import (
	"context"
	"fmt"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"
	"github.com/xormplus/xorm"
	"google.golang.org/protobuf/types/known/timestamppb"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	. "github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/daemon"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
)

type daemonInterface struct {
}

func (d daemonInterface) AddIntoPipelineCrond(cron *db.PipelineCron) error {
	return nil
}

func (d daemonInterface) DeleteFromPipelineCrond(cron *db.PipelineCron) error {
	return nil
}

func (d daemonInterface) ReloadCrond(ctx context.Context) ([]string, error) {
	panic("implement me")
}

func (d daemonInterface) CrondSnapshot() []string {
	panic("implement me")
}

func (d daemonInterface) WithPipelineFunc(createPipelineFunc daemon.CreatePipelineFunc) {
	panic("implement me")
}

func Test_provider_CronCreate(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CronCreateRequest
	}
	tests := []struct {
		name    string
		args    args
		want    *pb.CronCreateResponse
		wantErr bool
	}{
		{
			name: "test nil req",
			args: args{
				ctx: context.Background(),
				req: &pb.CronCreateRequest{},
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "test normal req",
			args: args{
				ctx: context.Background(),
				req: &pb.CronCreateRequest{
					PipelineSource:  "test",
					PipelineYmlName: "test",
					PipelineYml: `version: 1.1
cron_compensator:
  enable: true
  latest_first: true
  stop_if_latter_executed: true
stages: []
`,
				},
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}

			patch := monkey.Patch(Transaction, func(dbClient *db.Client, do func(option mysqlxorm.SessionOption) error) error {
				return nil
			})
			defer patch.Unpatch()

			got, err := s.CronCreate(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("CronCreate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("CronCreate() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_pbCronToDBCron(t *testing.T) {
	stringTime := "2017-08-30 16:40:41"
	loc, _ := time.LoadLocation("UTC")
	parseTime, _ := time.ParseInLocation("2006-01-02 15:04:05", stringTime, loc)

	type args struct {
		pbCron *Cron
	}
	tests := []struct {
		name       string
		args       args
		wantResult *db.PipelineCron
		wantErr    bool
	}{
		{
			name: "test match",
			args: args{
				pbCron: &Cron{
					ID:                     1,
					TimeCreated:            timestamppb.New(parseTime),
					TimeUpdated:            timestamppb.New(parseTime),
					ApplicationID:          1,
					Branch:                 "test",
					CronExpr:               "test",
					CronStartTime:          timestamppb.New(parseTime),
					PipelineYmlName:        "test",
					BasePipelineID:         1,
					Enable:                 wrapperspb.Bool(true),
					PipelineYml:            "test",
					ConfigManageNamespaces: []string{"1"},
					UserID:                 "test",
					OrgID:                  1,
					PipelineDefinitionID:   "test",
					PipelineSource:         "test",
					Secrets: map[string]string{
						"test": "test",
					},
					Extra: &CronExtra{
						PipelineYml: "test",
						ClusterName: "test",
						Labels: map[string]string{
							"test": "test",
						},
						NormalLabels: map[string]string{
							"test": "test",
						},
						Envs: map[string]string{
							"test": "test",
						},
						ConfigManageNamespaces: []string{"test"},
						IncomingSecrets:        map[string]string{"test": "test"},
						CronStartFrom:          timestamppb.New(parseTime),
						Version:                "v2",
						Compensator: &CronCompensator{
							Enable:               wrapperspb.Bool(true),
							LatestFirst:          wrapperspb.Bool(true),
							StopIfLatterExecuted: wrapperspb.Bool(true),
						},
						LastCompensateAt: timestamppb.New(parseTime),
					},
					IsEdge: wrapperspb.Bool(false),
				},
			},
			wantErr: false,
			wantResult: &db.PipelineCron{
				ID:              1,
				TimeCreated:     parseTime,
				TimeUpdated:     parseTime,
				PipelineSource:  apistructs.PipelineSource("test"),
				PipelineYmlName: "test",
				CronExpr:        "test",
				Enable:          &[]bool{true}[0],
				Extra: db.PipelineCronExtra{
					PipelineYml: "test",
					ClusterName: "test",
					FilterLabels: map[string]string{
						"test": "test",
					},
					NormalLabels: map[string]string{
						"test": "test",
					},
					Envs: map[string]string{
						"test": "test",
					},
					ConfigManageNamespaces: []string{"test"},
					IncomingSecrets: map[string]string{
						"test": "test",
					},
					CronStartFrom: &parseTime,
					Version:       "v2",
					Compensator: &apistructs.CronCompensator{
						Enable:               true,
						LatestFirst:          true,
						StopIfLatterExecuted: true,
					},
					LastCompensateAt: &parseTime,
				},
				ApplicationID:        1,
				Branch:               "test",
				BasePipelineID:       1,
				PipelineDefinitionID: "test",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotResult, err := pbCronToDBCron(tt.args.pbCron)
			assert.NoError(t, err)
			assert.EqualValues(t, *gotResult, *tt.wantResult)
		})
	}
}

func Test_provider_InsertOrUpdatePipelineCron(t *testing.T) {
	type args struct {
		new_ *db.PipelineCron
		ops  []mysqlxorm.SessionOption
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test update v1 cron",
			args: args{
				new_: &db.PipelineCron{
					ApplicationID:   1,
					Branch:          "test",
					PipelineYmlName: "test",
				},
			},
		},
		{
			name: "test source and ymlName",
			args: args{
				new_: &db.PipelineCron{
					PipelineSource:  "test",
					PipelineYmlName: "test",
				},
			},
		},
		{
			name: "test create",
			args: args{
				new_: &db.PipelineCron{
					CronExpr: "test",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}

			var engine xorm.Engine

			var dbClient db.Client
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "GetDBClient", func(dbClient *db.Client) *xorm.Engine {
				return &engine
			})
			defer patch2.Unpatch()

			patch3 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "UpdatePipelineCron", func(dbClient *db.Client, id interface{}, cron *db.PipelineCron, ops ...mysqlxorm.SessionOption) error {
				assert.Equal(t, id, uint64(1))
				return nil
			})
			defer patch3.Unpatch()

			patch4 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "CreatePipelineCron", func(bClient *db.Client, cron *db.PipelineCron, ops ...mysqlxorm.SessionOption) error {
				return nil
			})
			defer patch4.Unpatch()

			patch5 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "IsCronExist", func(dbClient *db.Client, cron *db.PipelineCron, ops ...mysqlxorm.SessionOption) (bool bool, err error) {
				assert.Equal(t, cron.ApplicationID, tt.args.new_.ApplicationID)
				assert.Equal(t, cron.Branch, tt.args.new_.Branch)
				assert.Equal(t, cron.PipelineYmlName, tt.args.new_.PipelineYmlName)
				cron.ID = 1

				if cron.ApplicationID > 0 {
					return true, nil
				}
				if cron.PipelineSource != "" {
					return true, nil
				}
				return false, nil
			})
			defer patch5.Unpatch()

			s.dbClient = &dbClient
			if err := s.InsertOrUpdatePipelineCron(tt.args.new_, tt.args.ops...); (err != nil) != tt.wantErr {
				t.Errorf("InsertOrUpdatePipelineCron() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_provider_disable(t *testing.T) {
	type fields struct{}
	type args struct {
		cron   *db.PipelineCron
		option mysqlxorm.SessionOption
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test update v1 cron",
			args: args{
				cron: &db.PipelineCron{
					ApplicationID:   1,
					Branch:          "test",
					PipelineYmlName: "test",
				},
			},
		},
		{
			name: "test source and ymlName",
			args: args{
				cron: &db.PipelineCron{
					PipelineSource:  "test",
					PipelineYmlName: "test",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}

			var engine xorm.Engine

			var dbClient db.Client
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "GetDBClient", func(dbClient *db.Client) *xorm.Engine {
				return &engine
			})
			defer patch2.Unpatch()

			patch3 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "UpdatePipelineCronWillUseDefault", func(dbClient *db.Client, id interface{}, cron *db.PipelineCron, columns []string, ops ...mysqlxorm.SessionOption) error {
				assert.Equal(t, id, uint64(1))
				return nil
			})
			defer patch3.Unpatch()

			patch4 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "IsCronExist", func(dbClient *db.Client, cron *db.PipelineCron, ops ...mysqlxorm.SessionOption) (bool bool, err error) {
				cron.ID = 1

				if cron.ApplicationID > 0 {
					return true, nil
				}
				if cron.PipelineSource != "" {
					return true, nil
				}
				return false, nil
			})
			defer patch4.Unpatch()

			s.dbClient = &dbClient
			if err := s.disable(tt.args.cron, tt.args.option); (err != nil) != tt.wantErr {
				t.Errorf("disable() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_provider_operate(t *testing.T) {
	type args struct {
		cronID uint64
		enable bool
	}

	type result struct {
		cron db.PipelineCron
		bool bool
		err  error
	}

	tests := []struct {
		result  result
		name    string
		args    args
		want    *Cron
		wantErr bool
	}{
		{
			name: "cron not find",
			result: result{
				bool: false,
			},
			args: args{
				cronID: 1,
				enable: true,
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}

			var dbClient db.Client
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "GetPipelineCron", func(dbClient *db.Client, id interface{}, ops ...mysqlxorm.SessionOption) (cron db.PipelineCron, bool bool, err error) {
				assert.Equal(t, id, tt.args.cronID)
				return tt.result.cron, tt.result.bool, tt.result.err
			})
			defer patch2.Unpatch()
			s.dbClient = &dbClient

			got, err := s.operate(tt.args.cronID, tt.args.enable)
			if (err != nil) != tt.wantErr {
				t.Errorf("operate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("operate() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_CronGet(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CronGetRequest
	}

	type result struct {
		cron db.PipelineCron
		bool bool
		err  error
	}

	tests := []struct {
		name    string
		result  result
		args    args
		want    *pb.CronGetResponse
		wantErr bool
	}{
		{
			name: "cron not find",
			result: result{
				bool: false,
			},
			args: args{
				ctx: nil,
				req: &pb.CronGetRequest{
					CronID: 1,
				},
			},
			want:    &pb.CronGetResponse{},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}

			var dbClient db.Client
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "GetPipelineCron", func(dbClient *db.Client, id interface{}, ops ...mysqlxorm.SessionOption) (cron db.PipelineCron, bool bool, err error) {
				assert.Equal(t, id, tt.args.req.CronID)
				return tt.result.cron, tt.result.bool, tt.result.err
			})
			defer patch2.Unpatch()
			s.dbClient = &dbClient

			got, err := s.CronGet(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("CronGet() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("CronGet() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_CronUpdate(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.CronUpdateRequest
	}

	type result struct {
		cron db.PipelineCron
		bool bool
		err  error
	}

	tests := []struct {
		name    string
		result  result
		args    args
		want    *pb.CronUpdateResponse
		wantErr bool
	}{
		{
			name: "cron not find",
			result: result{
				bool: true,
			},
			args: args{
				ctx: nil,
				req: &pb.CronUpdateRequest{
					CronID: 1,
				},
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}

			var dbClient db.Client
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "GetPipelineCron", func(dbClient *db.Client, id interface{}, ops ...mysqlxorm.SessionOption) (cron db.PipelineCron, bool bool, err error) {
				assert.Equal(t, id, tt.args.req.CronID)
				return tt.result.cron, tt.result.bool, tt.result.err
			})
			defer patch2.Unpatch()
			s.dbClient = &dbClient

			got, err := s.CronUpdate(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("CronUpdate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("CronUpdate() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_cronDelete(t *testing.T) {
	type args struct {
		req    *pb.CronDeleteRequest
		option mysqlxorm.SessionOption
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test to edge",
			args: args{
				req: &pb.CronDeleteRequest{
					CronID: 1,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}

			var dbClient db.Client
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "GetPipelineCron", func(dbClient *db.Client, id interface{}, ops ...mysqlxorm.SessionOption) (cron db.PipelineCron, bool bool, err error) {
				cron.ID = tt.args.req.CronID
				return cron, true, nil
			})
			defer patch2.Unpatch()

			patch3 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "DeletePipelineCron", func(dbClient *db.Client, id interface{}, ops ...mysqlxorm.SessionOption) error {
				return nil
			})
			defer patch3.Unpatch()
			s.dbClient = &dbClient

			var bdl bundle.Bundle
			patch4 := monkey.PatchInstanceMethod(reflect.TypeOf(&bdl), "DeleteCron", func(bdl *bundle.Bundle, cronID uint64) error {
				assert.EqualValues(t, cronID, tt.args.req.CronID)
				return nil
			})
			defer patch4.Unpatch()

			var daemonInterface daemonInterface
			s.Daemon = daemonInterface
			s.EdgePipelineRegister = &edgepipeline_register.MockEdgeRegister{}

			if err := s.delete(tt.args.req, tt.args.option); (err != nil) != tt.wantErr {
				t.Errorf("cronDelete() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_provider_update(t *testing.T) {
	type args struct {
		req    *pb.CronUpdateRequest
		cron   db.PipelineCron
		fields []string
		option mysqlxorm.SessionOption
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test to edge",
			args: args{
				req: &pb.CronUpdateRequest{
					CronID: 2,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}

			var dbClient db.Client
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "UpdatePipelineCronWillUseDefault", func(dbClient *db.Client, id interface{}, cron *db.PipelineCron, columns []string, ops ...mysqlxorm.SessionOption) error {
				enable := false
				cron.Enable = &enable
				return nil
			})
			defer patch2.Unpatch()
			s.dbClient = &dbClient

			var daemonInterface daemonInterface
			s.Daemon = daemonInterface

			var bdl bundle.Bundle
			patch3 := monkey.PatchInstanceMethod(reflect.TypeOf(&bdl), "CronUpdate", func(bdl *bundle.Bundle, req *pb.CronUpdateRequest) error {
				assert.EqualValues(t, req, tt.args.req)
				return nil
			})
			defer patch3.Unpatch()

			s.EdgePipelineRegister = &edgepipeline_register.MockEdgeRegister{}

			if err := s.update(tt.args.req, tt.args.cron, tt.args.fields, tt.args.option); (err != nil) != tt.wantErr {
				t.Errorf("update() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

type MockDaemon struct {
	daemonInterface
}

func (m MockDaemon) AddIntoPipelineCrond(cron *db.PipelineCron) error {
	if cron.ID == 1 {
		return nil
	}
	return fmt.Errorf("failed to add")
}

func Test_provider_addIntoPipelineCrond(t *testing.T) {
	type fields struct {
		Daemon daemon.Interface
	}
	type args struct {
		cron *db.PipelineCron
	}
	enable := true
	mockDaemon := &MockDaemon{}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test with enable",
			fields: fields{
				Daemon: mockDaemon,
			},
			args: args{
				cron: &db.PipelineCron{
					ID:       1,
					CronExpr: "*/1 * * * *",
					Enable:   &enable,
				},
			},
			wantErr: false,
		},
		{
			name: "test with disable",
			fields: fields{
				Daemon: mockDaemon,
			},
			args: args{
				cron: &db.PipelineCron{
					ID:       1,
					CronExpr: "*/1 * * * *",
					Enable:   new(bool),
				},
			},
			wantErr: false,
		},
		{
			name: "test with error",
			fields: fields{
				Daemon: mockDaemon,
			},
			args: args{
				cron: &db.PipelineCron{
					ID:       2,
					CronExpr: "*/1 * * * *",
					Enable:   &enable,
				},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{
				Daemon: tt.fields.Daemon,
			}
			if err := s.addIntoPipelineCrond(tt.args.cron); (err != nil) != tt.wantErr {
				t.Errorf("addIntoPipelineCrond() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
