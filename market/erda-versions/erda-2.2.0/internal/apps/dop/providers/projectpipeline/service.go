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

package projectpipeline

import (
	"context"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/i18n"
	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	dpb "github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	sourcepb "github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	guidepb "github.com/erda-project/erda-proto-go/dop/guide/pb"
	"github.com/erda-project/erda-proto-go/dop/projectpipeline/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline/deftype"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
	"github.com/erda-project/erda/internal/apps/dop/services/permission"
	"github.com/erda-project/erda/internal/apps/dop/services/pipeline"
)

type ProjectPipelineService struct {
	logger logs.Logger
	db     *dao.DBClient
	bundle *bundle.Bundle

	pipelineSvc        *pipeline.Pipeline
	PipelineSource     sourcepb.SourceServiceServer
	PipelineDefinition dpb.DefinitionServiceServer
	Permission         *permission.Permission
	PipelineCms        cmspb.CmsServiceServer
	trans              i18n.Translator
	GuideSvc           guidepb.GuideServiceServer
	PipelineCron       cronpb.CronServiceServer
	tokenService       tokenpb.TokenServiceServer
	branchRuleSve      *branchrule.BranchRule
}

func (p *ProjectPipelineService) WithPipelineSvc(svc *pipeline.Pipeline) {
	p.pipelineSvc = svc
}

func (p *ProjectPipelineService) WithPermissionSvc(permission *permission.Permission) {
	p.Permission = permission
}

func (p *ProjectPipelineService) WithBranchRuleSve(svc *branchrule.BranchRule) {
	p.branchRuleSve = svc
}

type Service interface {
	Create(ctx context.Context, params *pb.CreateProjectPipelineRequest) (*pb.CreateProjectPipelineResponse, error)
	BatchCreateByGittarPushHook(ctx context.Context, params *pb.GittarPushPayloadEvent) (*pb.BatchCreateProjectPipelineResponse, error)
	List(ctx context.Context, params deftype.ProjectPipelineList) ([]*dpb.PipelineDefinition, int64, error)
	ListUsedRefs(ctx context.Context, params deftype.ProjectPipelineUsedRefList) ([]string, error)
	Delete(ctx context.Context, params deftype.ProjectPipelineDelete) (*deftype.ProjectPipelineDeleteResult, error)
	Update(ctx context.Context, params *pb.UpdateProjectPipelineRequest) (*pb.UpdateProjectPipelineResponse, error)
	SetPrimary(ctx context.Context, params deftype.ProjectPipelineCategory) (*dpb.PipelineDefinitionUpdateResponse, error)
	UnSetPrimary(ctx context.Context, params deftype.ProjectPipelineCategory) (*dpb.PipelineDefinitionUpdateResponse, error)
	ListApp(ctx context.Context, params *pb.ListAppRequest) (*pb.ListAppResponse, error)
	ListPipelineYml(ctx context.Context, req *pb.ListAppPipelineYmlRequest) (*pb.ListAppPipelineYmlResponse, error)
	CreateNamePreCheck(ctx context.Context, req *pb.CreateProjectPipelineNamePreCheckRequest) (*pb.CreateProjectPipelineNamePreCheckResponse, error)
	CreateSourcePreCheck(ctx context.Context, req *pb.CreateProjectPipelineSourcePreCheckRequest) (*pb.CreateProjectPipelineSourcePreCheckResponse, error)
	OneClickCreate(ctx context.Context, params *pb.OneClickCreateProjectPipelineRequest) (*pb.OneClickCreateProjectPipelineResponse, error)

	Run(ctx context.Context, params *pb.RunProjectPipelineRequest) (*pb.RunProjectPipelineResponse, error)
	Cancel(ctx context.Context, params *pb.CancelProjectPipelineRequest) (*pb.CancelProjectPipelineResponse, error)
	Rerun(ctx context.Context, params *pb.RerunProjectPipelineRequest) (*pb.RerunProjectPipelineResponse, error)
	RerunFailed(ctx context.Context, params *pb.RerunFailedProjectPipelineRequest) (*pb.RerunFailedProjectPipelineResponse, error)
	BatchRun(ctx context.Context, params deftype.ProjectPipelineBatchRun) (*deftype.ProjectPipelineBatchRunResult, error)
	StartCron(ctx context.Context, params deftype.ProjectPipelineStartCron) (*deftype.ProjectPipelineStartCronResult, error)
	EndCron(ctx context.Context, params deftype.ProjectPipelineEndCron) (*deftype.ProjectPipelineEndCronResult, error)
	ListExecHistory(ctx context.Context, params *pb.ListPipelineExecHistoryRequest) (*pb.ListPipelineExecHistoryResponse, error)
}
