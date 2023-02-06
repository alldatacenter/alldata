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

package configcenter

import (
	"net/http"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/http/encoding"
	"github.com/erda-project/erda-proto-go/msp/configcenter/pb"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	mperm "github.com/erda-project/erda/internal/apps/msp/instance/permission"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
}

// +provider
type provider struct {
	Cfg                 *config
	Log                 logs.Logger
	Register            transport.Register `autowired:"service-register" optional:"true"`
	DB                  *gorm.DB           `autowired:"mysql-client"`
	Perm                perm.Interface     `autowired:"permission"`
	MPerm               mperm.Interface    `autowired:"msp.permission"`
	configCenterService *configCenterService
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.configCenterService = &configCenterService{
		p:                p,
		instanceTenantDB: &instancedb.InstanceTenantDB{DB: p.DB},
		instanceDB:       &instancedb.InstanceDB{DB: p.DB},
	}
	if p.Register != nil {
		type ConfigCenterService = pb.ConfigCenterServiceServer
		pb.RegisterConfigCenterServiceImp(p.Register, p.configCenterService, apis.Options(),
			transport.WithHTTPOptions(transhttp.WithEncoder(func(rw http.ResponseWriter, r *http.Request, data interface{}) error {
				// compatibility with api "/api/tmc/config/tenants/{tenantID}/groups/{groupID}" response
				if resp, ok := data.(*apis.Response); ok && resp != nil {
					if data, ok := resp.Data.([]*pb.GroupProperties); ok {
						m := make(map[string]interface{})
						for _, item := range data {
							m[item.Group] = item.Properties
						}
						resp.Data = m
					}
				}
				return encoding.EncodeResponse(rw, r, data)
			})),
			p.Perm.Check(
				perm.Method(ConfigCenterService.GetGroups, perm.ScopeProject, "config-center_group", perm.ActionGet, p.MPerm.TenantToProjectID("", "TenantID")),
				perm.Method(ConfigCenterService.GetGroupProperties, perm.ScopeProject, "config-center_properties", perm.ActionGet, p.MPerm.TenantToProjectID("", "TenantID")),
				perm.Method(ConfigCenterService.SaveGroupProperties, perm.ScopeProject, "config-center_properties", perm.ActionUpdate, p.MPerm.TenantToProjectID("", "TenantID")),
			),
		)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.configcenter.ConfigCenterService" || ctx.Type() == pb.ConfigCenterServiceServerType() || ctx.Type() == pb.ConfigCenterServiceHandlerType():
		return p.configCenterService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.configcenter", &servicehub.Spec{
		Services:   pb.ServiceNames(),
		Types:      pb.Types(),
		ConfigFunc: func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
