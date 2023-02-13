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
	"reflect"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
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
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type config struct {
}

type provider struct {
	Cfg      *config
	Log      logs.Logger
	bundle   *bundle.Bundle
	DB       *gorm.DB           `autowired:"mysql-client"`
	Register transport.Register `autowired:"service-register" required:"true"`
	Trans    i18n.Translator    `translator:"project-pipeline" required:"true"`

	projectPipelineSvc *ProjectPipelineService
	PipelineSource     sourcepb.SourceServiceServer `autowired:"erda.core.pipeline.source.SourceService" required:"true"`
	PipelineDefinition dpb.DefinitionServiceServer  `autowired:"erda.core.pipeline.definition.DefinitionService" required:"true"`
	PipelineCms        cmspb.CmsServiceServer       `autowired:"erda.core.pipeline.cms.CmsService"`
	GuideSvc           guidepb.GuideServiceServer   `autowired:"erda.dop.guide.GuideService" required:"true"`
	PipelineCron       cronpb.CronServiceServer     `autowired:"erda.core.pipeline.cron.CronService" required:"true"`
	TokenService       tokenpb.TokenServiceServer   `autowired:"erda.core.token.TokenService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bundle = bundle.New(bundle.WithAllAvailableClients())
	p.projectPipelineSvc = &ProjectPipelineService{
		logger: p.Log,
		db: &dao.DBClient{
			DBEngine: &dbengine.DBEngine{
				DB: p.DB,
			},
		},
		bundle:             p.bundle,
		PipelineSource:     p.PipelineSource,
		PipelineDefinition: p.PipelineDefinition,
		PipelineCms:        p.PipelineCms,
		trans:              p.Trans,
		GuideSvc:           p.GuideSvc,
		PipelineCron:       p.PipelineCron,
		tokenService:       p.TokenService,
	}
	if p.Register != nil {
		pb.RegisterProjectPipelineServiceImp(p.Register, p.projectPipelineSvc, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.dop.projectpipeline.ProjectPipelineServiceMethod" || ctx.Type() == reflect.TypeOf(reflect.TypeOf((*Service)(nil)).Elem()):
		return p.projectPipelineSvc
	case ctx.Service() == "erda.dop.projectpipeline.ProjectPipelineService" || ctx.Type() == pb.ProjectPipelineServiceServerType() || ctx.Type() == pb.ProjectPipelineServiceHandlerType():
		return p.projectPipelineSvc
	}
	return p
}

func init() {
	servicehub.Register("erda.dop.projectpipeline", &servicehub.Spec{
		Services:             append(pb.ServiceNames(), "erda.dop.projectpipeline.ProjectPipelineServiceMethod"),
		Types:                append(pb.Types(), reflect.TypeOf(reflect.TypeOf((*Service)(nil)).Elem())),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
