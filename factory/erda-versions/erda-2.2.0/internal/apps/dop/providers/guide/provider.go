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

package guide

import (
	"context"
	"reflect"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/dop/guide/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/guide/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/cron"
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

	GuideService *GuideService
}

func (p *provider) Run(ctx context.Context) error {
	cron := cron.New()
	err := cron.AddFunc(conf.UpdateGuideExpiryStatusCron(), func() {
		p.Log.Infof("begin update guide...")
		if err := p.GuideService.BatchUpdateGuideExpiryStatus(); err != nil {
			p.Log.Errorf("failed to BatchUpdateGuideExpiryStatus, err: %v", err)
		}
	})
	if err != nil {
		panic(err)
	}
	cron.Start()
	return nil
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bundle = bundle.New(bundle.WithGittar())
	p.GuideService = &GuideService{
		bdl: p.bundle,
		db: &db.GuideDB{
			DBClient: &dao.DBClient{
				DBEngine: &dbengine.DBEngine{
					DB: p.DB,
				},
			},
		},
	}
	if p.Register != nil {
		pb.RegisterGuideServiceImp(p.Register, p.GuideService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.dop.guide.GuideServiceMethod" || ctx.Type() == reflect.TypeOf(reflect.TypeOf((*Service)(nil)).Elem()):
		return p.GuideService
	case ctx.Service() == "erda.dop.guide.GuideService" || ctx.Type() == pb.GuideServiceServerType() || ctx.Type() == pb.GuideServiceHandlerType():
		return p.GuideService
	}
	return p
}

func init() {
	servicehub.Register("erda.dop.guide", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                append(pb.Types()),
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
