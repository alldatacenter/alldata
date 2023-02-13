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

package template

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda-infra/providers/mysql"
	"github.com/erda-project/erda/internal/tools/monitor/common/db"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
)

type define struct{}

func (d *define) Services() []string { return []string{"template"} }
func (d *define) Dependencies() []string {
	return []string{"http-server", "mysql"}
}
func (d *define) Summary() string     { return "template" }
func (d *define) Description() string { return d.Summary() }
func (d *define) Config() interface{} { return &pconfig{} }
func (d *define) Creator() servicehub.Creator {
	return func() servicehub.Provider { return &provider{} }
}

type pconfig struct {
	PresetDashboards string `file:"preset_dashboards"`
	Tables           struct {
		Template string `file:"template" default:"sp_dashboard_template"`
	} `file:"tables"`
}

type provider struct {
	C         *pconfig
	L         logs.Logger
	metricq   metricq.Queryer
	db        *DB
	authDb    *db.DB
	presetMap map[string][]string
}

// Init .
func (p *provider) Init(ctx servicehub.Context) error {
	if len(p.C.Tables.Template) > 0 {
		tableTemplate = p.C.Tables.Template
	}

	p.authDb = db.New(ctx.Service("mysql").(mysql.Interface).DB())

	p.db = newDB(ctx.Service("mysql").(mysql.Interface).DB())

	routes := ctx.Service("http-server", interceptors.Recover(p.L)).(httpserver.Router)

	return p.intRoutes(routes)
}

func init() {
	servicehub.RegisterProvider("template", &define{})
}
