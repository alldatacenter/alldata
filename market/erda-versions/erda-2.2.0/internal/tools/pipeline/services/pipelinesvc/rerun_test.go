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
	"reflect"
	"testing"

	"bou.ke/monkey"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	commonpb "github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type TestPipelineCron struct {
	getResp *cronpb.CronGetResponse
}

func (t TestPipelineCron) CronCreate(ctx context.Context, request *cronpb.CronCreateRequest) (*cronpb.CronCreateResponse, error) {
	return &cronpb.CronCreateResponse{
		Data: &commonpb.Cron{
			ID: 123,
		},
	}, nil
}

func (t TestPipelineCron) CronPaging(ctx context.Context, request *cronpb.CronPagingRequest) (*cronpb.CronPagingResponse, error) {
	panic("implement me")
}

func (t TestPipelineCron) CronStart(ctx context.Context, request *cronpb.CronStartRequest) (*cronpb.CronStartResponse, error) {
	panic("implement me")
}

func (t TestPipelineCron) CronStop(ctx context.Context, request *cronpb.CronStopRequest) (*cronpb.CronStopResponse, error) {
	panic("implement me")
}

func (t TestPipelineCron) CronDelete(ctx context.Context, request *cronpb.CronDeleteRequest) (*cronpb.CronDeleteResponse, error) {
	panic("implement me")
}

func (t TestPipelineCron) CronGet(ctx context.Context, request *cronpb.CronGetRequest) (*cronpb.CronGetResponse, error) {
	return t.getResp, nil
}

func (t TestPipelineCron) CronUpdate(ctx context.Context, request *cronpb.CronUpdateRequest) (*cronpb.CronUpdateResponse, error) {
	panic("implement me")
}

func TestPipelineSvc_Rerun(t *testing.T) {
	type args struct {
		ctx context.Context
		req *apistructs.PipelineRerunRequest
	}
	tests := []struct {
		name    string
		args    args
		want    *spec.Pipeline
		wantErr bool
	}{
		{
			name: "test",
			args: args{
				ctx: context.Background(),
				req: &apistructs.PipelineRerunRequest{
					PipelineID: 1,
				},
			},
			wantErr: true,
			want:    nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &PipelineSvc{}

			var dbClient dbclient.Client
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(&dbClient), "GetPipeline", func(dbClient *dbclient.Client, id interface{}, ops ...dbclient.SessionOption) (spec.Pipeline, error) {
				return spec.Pipeline{}, nil
			})
			defer patch.Unpatch()

			var testPipelineCron TestPipelineCron
			testPipelineCron.getResp = &cronpb.CronGetResponse{
				Data: nil,
			}
			s.pipelineCronSvc = testPipelineCron

			got, err := s.Rerun(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("Rerun() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Rerun() got = %v, want %v", got, tt.want)
			}
		})
	}
}
