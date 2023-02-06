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

package notifygroup

import (
	"context"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/messenger/notifygroup/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/services/notify"
	"github.com/erda-project/erda/internal/core/legacy/services/permission"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/i18n"
)

type config struct{}

type provider struct {
	Cfg                *config
	Log                logs.Logger
	Register           transport.Register `autowired:"service-register" optional:"true"`
	DB                 *gorm.DB           `autowired:"mysql-client"`
	notifyGroupService *notifyGroupService
	audit              audit.Auditor
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.audit = audit.GetAuditor(ctx)
	p.notifyGroupService = &notifyGroupService{}
	pm := permission.New(permission.WithDBClient(&dao.DBClient{
		DB: p.DB,
	}))
	p.notifyGroupService.Permission = pm
	p.notifyGroupService.NotifyGroup = notify.New(notify.WithDBClient(&dao.DBClient{
		p.DB,
	}))
	p.notifyGroupService.bdl = bundle.New(bundle.WithI18nLoader(&i18n.LocaleResourceLoader{}))
	if p.Register != nil {
		type NotifyGroupService = pb.NotifyGroupServiceServer
		pb.RegisterNotifyGroupServiceImp(p.Register, p.notifyGroupService, apis.Options(),
			p.audit.Audit(
				audit.Method(NotifyGroupService.CreateNotifyGroup, audit.OrgScope, string(apistructs.CreateOrgNotifyGroupTemplate),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(NotifyGroupService.UpdateNotifyGroup, audit.OrgScope, string(apistructs.UpdateOrgNotifyGroupTemplate),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(NotifyGroupService.DeleteNotifyGroup, audit.OrgScope, string(apistructs.DeleteOrgNotifyGroupTemplate),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
			),
		)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.messenger.notifygroup.NotifyGroupService" || ctx.Type() == pb.NotifyGroupServiceServerType() || ctx.Type() == pb.NotifyGroupServiceHandlerType():
		return p.notifyGroupService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.messenger.notifygroup", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
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
