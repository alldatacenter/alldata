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

package metric

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/bundle"
	monitordb "github.com/erda-project/erda/internal/apps/msp/instance/db/monitor"
	mperm "github.com/erda-project/erda/internal/apps/msp/instance/permission"
)

type config struct {
	MonitorURL string `file:"monitor_url"`
}

// +provider
type provider struct {
	Cfg    *config
	Log    logs.Logger
	DB     *gorm.DB                     `autowired:"mysql-client"`
	Router httpserver.Router            `autowired:"http-router"`
	MPerm  mperm.Interface              `autowired:"msp.permission"`
	Tenant tenantpb.TenantServiceServer `autowired:"erda.msp.tenant.TenantService"`
	bdl    *bundle.Bundle

	db            *monitordb.MonitorDB
	compatibleTKs map[string][]string
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())
	p.db = &monitordb.MonitorDB{DB: p.DB}
	err = p.loadCompatibleTKs()
	if err != nil {
		return err
	}
	return p.initRoutes(p.Router)
}

func init() {
	servicehub.Register("erda.msp.apm.metric", &servicehub.Spec{
		Services:   []string{"erda.msp.apm.metric"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
