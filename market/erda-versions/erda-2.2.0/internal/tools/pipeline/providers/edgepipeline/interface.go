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

package edgepipeline

import (
	"context"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/pipelinesvc"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type Interface interface {
	CreateInterface

	InjectLegacyFields(pSvc *pipelinesvc.PipelineSvc)
}

type CreateInterface interface {
	CreatePipeline(ctx context.Context, req *apistructs.PipelineCreateRequestV2) (*apistructs.PipelineDTO, error)
	CreateCron(ctx context.Context, req *cronpb.CronCreateRequest) (*pb.Cron, error)
	RunPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineRunRequest) error
	CancelPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineCancelRequest) error
	RerunFailedPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineRerunFailedRequest) (*apistructs.PipelineDTO, error)
	RerunPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineRerunRequest) (*apistructs.PipelineDTO, error)
}

func (s *provider) InjectLegacyFields(pipelineSvc *pipelinesvc.PipelineSvc) {
	s.pipelineSvc = pipelineSvc
}
