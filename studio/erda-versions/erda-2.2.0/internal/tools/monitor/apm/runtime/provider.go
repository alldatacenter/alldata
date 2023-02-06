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

package runtime

import (
	"github.com/olivere/elastic"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda-infra/providers/mysql"
	"github.com/erda-project/erda/internal/tools/monitor/common/db"
)

type provider struct {
	Cfg *config
	Log logs.Logger
	db  *db.DB
	es  *elastic.Client
	ctx servicehub.Context
}

type define struct{}

func (d *define) Services() []string     { return []string{"apm-runtime"} }
func (d *define) Dependencies() []string { return []string{"http-server", "elasticsearch", "mysql"} }
func (d *define) Summary() string        { return "apm-runtime api" }
func (d *define) Description() string    { return d.Summary() }
func (d *define) Config() interface{}    { return &config{} }

func (d *define) Creator() servicehub.Creator {
	return func() servicehub.Provider {
		return &provider{}
	}
}

type config struct{}

func (runtime *provider) Init(ctx servicehub.Context) (err error) {
	runtime.ctx = ctx

	// elasticsearch
	es := ctx.Service("elasticsearch").(elasticsearch.Interface)
	runtime.es = es.Client()

	// mysql
	runtime.db = db.New(ctx.Service("mysql").(mysql.Interface).DB())

	routes := ctx.Service("http-server", interceptors.Recover(runtime.Log)).(httpserver.Router)
	return runtime.initRoutes(routes)
}

func init() {
	servicehub.RegisterProvider("apm-runtime", &define{})
}
