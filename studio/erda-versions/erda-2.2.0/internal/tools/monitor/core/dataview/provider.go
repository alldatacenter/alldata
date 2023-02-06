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

package dataview

import (
	"context"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/dataview/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/internal/tools/monitor/core/dataview/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

func (p *provider) initRoutes(routes httpserver.Router) error {
	routes.POST("/api/dashboard/blocks/parse", p.ParseDashboardTemplate)
	routes.POST("/api/dashboard/blocks/export", p.ExportDashboardFile)
	routes.POST("/api/dashboard/blocks/import", p.ImportDashboardFile)
	return nil
}

type config struct {
	Tables struct {
		SystemBlock string `file:"system_block" default:"sp_dashboard_block_system"`
		UserBlock   string `file:"user_block" default:"sp_dashboard_block"`
	} `file:"tables"`
}

// +provider
type provider struct {
	Cfg             *config
	Log             logs.Logger
	Register        transport.Register `autowired:"service-register" optional:"true"`
	DB              *gorm.DB           `autowired:"mysql-client"`
	dataViewService *dataViewService
	Tran            i18n.Translator `translator:"charts"`
	bdl             *bundle.Bundle
	audit           audit.Auditor
	sys             *db.SystemViewDB
	custom          *db.CustomViewDB
	history         *db.ErdaDashboardHistoryDB

	ExportChannel chan string
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.audit = audit.GetAuditor(ctx)
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())
	if len(p.Cfg.Tables.SystemBlock) > 0 {
		db.TableSystemView = p.Cfg.Tables.SystemBlock
	}
	if len(p.Cfg.Tables.UserBlock) > 0 {
		db.TableCustomView = p.Cfg.Tables.UserBlock
	}
	p.sys = &db.SystemViewDB{DB: p.DB}
	p.custom = &db.CustomViewDB{DB: p.DB}
	p.history = &db.ErdaDashboardHistoryDB{DB: p.DB}

	p.dataViewService = &dataViewService{
		p:       p,
		sys:     p.sys,
		custom:  p.custom,
		history: p.history,
	}
	if p.Register != nil {
		type DataViewService = pb.DataViewServiceServer
		pb.RegisterDataViewServiceImp(p.Register, p.dataViewService, apis.Options(),
			p.audit.Audit(
				audit.Method(DataViewService.CreateCustomView, audit.OrgScope, string(apistructs.AddDashboard),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(DataViewService.UpdateCustomView, audit.OrgScope, string(apistructs.UpdateDashboard),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(DataViewService.DeleteCustomView, audit.OrgScope, string(apistructs.DeleteDashboard),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
			),
		)
	}

	p.ExportChannel = make(chan string, 1)
	p.ExportTaskExecutor(time.Second * time.Duration(20))
	routes := ctx.Service("http-server", interceptors.Recover(p.Log), interceptors.CORS()).(httpserver.Router)
	return p.initRoutes(routes)
}

func (p *provider) ExportTaskExecutor(interval time.Duration) {
	// Scheduled polling export task
	go func() {
		ticker := time.NewTicker(interval)
		for {
			select {
			case <-ticker.C:
				ticker.Reset(interval)
			case id := <-p.ExportChannel:
				p.ExportTask(id)
			}
		}
	}()
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.monitor.dataview.DataViewService" || ctx.Type() == pb.DataViewServiceServerType() || ctx.Type() == pb.DataViewServiceHandlerType():
		return p.dataViewService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.monitor.dataview", &servicehub.Spec{
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
