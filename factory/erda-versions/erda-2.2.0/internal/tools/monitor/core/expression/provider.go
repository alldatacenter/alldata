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
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/expression/pb"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	alertdb "github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
	AlertRules  string `file:"alert_rules"`
	MetricRules string `file:"metric_rules"`
}

type provider struct {
	Cfg               *config
	Log               logs.Logger
	Register          transport.Register `autowired:"service-register" optional:"true"`
	t                 i18n.Translator
	DB                *gorm.DB `autowired:"mysql-client"`
	expressionService *expressionService
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.expressionService = &expressionService{
		alertDB:                        &alertdb.AlertExpressionDB{DB: p.DB},
		metricDB:                       &alertdb.MetricExpressionDB{DB: p.DB},
		customizeAlertNotifyTemplateDB: &alertdb.CustomizeAlertNotifyTemplateDB{DB: p.DB},
		alertNotifyDB:                  &alertdb.AlertNotifyDB{DB: p.DB},
		clientDB:                       &dao.DBClient{DB: p.DB},
	}
	err := p.expressionService.init(p.Cfg.AlertRules, p.Cfg.MetricRules)
	if err != nil {
		return err
	}
	if p.Register != nil {
		pb.RegisterExpressionServiceImp(p.Register, p.expressionService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.monitor.expression.ExpressionService" || ctx.Type() == pb.ExpressionServiceServerType() || ctx.Type() == pb.ExpressionServiceHandlerType():
		return p.expressionService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.monitor.expression", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		OptionalDependencies: []string{"service-register"},
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Types: pb.Types(),
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
