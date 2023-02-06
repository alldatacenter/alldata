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

package pipelinesvc

import (
	"context"
	"testing"
	"time"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type CronServiceServerTestImpl struct {
	create *cronpb.CronCreateResponse
}

func (c CronServiceServerTestImpl) CronCreate(ctx context.Context, request *cronpb.CronCreateRequest) (*cronpb.CronCreateResponse, error) {
	return c.create, nil
}

func (c CronServiceServerTestImpl) CronPaging(ctx context.Context, request *cronpb.CronPagingRequest) (*cronpb.CronPagingResponse, error) {
	panic("implement me")
}

func (c CronServiceServerTestImpl) CronStart(ctx context.Context, request *cronpb.CronStartRequest) (*cronpb.CronStartResponse, error) {
	panic("implement me")
}

func (c CronServiceServerTestImpl) CronStop(ctx context.Context, request *cronpb.CronStopRequest) (*cronpb.CronStopResponse, error) {
	panic("implement me")
}

func (c CronServiceServerTestImpl) CronDelete(ctx context.Context, request *cronpb.CronDeleteRequest) (*cronpb.CronDeleteResponse, error) {
	panic("implement me")
}

func (c CronServiceServerTestImpl) CronGet(ctx context.Context, request *cronpb.CronGetRequest) (*cronpb.CronGetResponse, error) {
	panic("implement me")
}

func (c CronServiceServerTestImpl) CronUpdate(ctx context.Context, request *cronpb.CronUpdateRequest) (*cronpb.CronUpdateResponse, error) {
	panic("implement me")
}

func TestPipelineSvc_UpdatePipelineCron(t *testing.T) {
	type args struct {
		p                      *spec.Pipeline
		cronStartFrom          *time.Time
		configManageNamespaces []string
		cronCompensator        *pipelineyml.CronCompensator
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test id > 0",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						PipelineSource:  "test",
						PipelineYmlName: "test",
						ClusterName:     "test",
					},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							CronExpr: "test",
						},
						PipelineYml: "test",
						Snapshot: spec.Snapshot{
							Envs: map[string]string{
								"test": "test",
							},
						},
					},
					Labels: map[string]string{
						"test": "test",
					},
				},
				cronStartFrom:          nil,
				configManageNamespaces: nil,
				cronCompensator:        nil,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &PipelineSvc{}
			impl := CronServiceServerTestImpl{
				create: &cronpb.CronCreateResponse{
					Data: &pb.Cron{
						ID: 1,
					},
				},
			}
			s.pipelineCronSvc = impl

			if err := s.UpdatePipelineCron(tt.args.p, tt.args.cronStartFrom, tt.args.configManageNamespaces, tt.args.cronCompensator); (err != nil) != tt.wantErr {
				t.Errorf("UpdatePipelineCron() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
