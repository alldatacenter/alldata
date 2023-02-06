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

package permission

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	monitordb "github.com/erda-project/erda/internal/apps/msp/instance/db/monitor"
	tenantdb "github.com/erda-project/erda/internal/apps/msp/tenant/db"
)

// +provider
type provider struct {
	Log              logs.Logger
	DB               *gorm.DB `autowired:"mysql-client"`
	instanceTenantDB *instancedb.InstanceTenantDB
	tmcDB            *instancedb.TmcDB
	monitorDB        *monitordb.MonitorDB
	MSPTenantDB      *tenantdb.MSPTenantDB
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.instanceTenantDB = &instancedb.InstanceTenantDB{DB: p.DB}
	p.tmcDB = &instancedb.TmcDB{DB: p.DB}
	p.monitorDB = &monitordb.MonitorDB{DB: p.DB}
	p.MSPTenantDB = &tenantdb.MSPTenantDB{DB: p.DB}
	return nil
}

func init() {
	servicehub.Register("msp.permission", &servicehub.Spec{
		Services: []string{"msp.permission"},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
