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

package block

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/mysql"
)

type pconfig struct {
	Tables struct {
		SystemBlock string `file:"system_block" default:"sp_dashboard_block_system"`
		UserBlock   string `file:"user_block" default:"sp_dashboard_block"`
	} `file:"tables"`
}

type provider struct {
	Cfg *pconfig
	Log logs.Logger
	db  *DB
}

// Init .
func (p *provider) Init(ctx servicehub.Context) error {
	if len(p.Cfg.Tables.SystemBlock) > 0 {
		tableSystemBlock = p.Cfg.Tables.SystemBlock
	}
	if len(p.Cfg.Tables.UserBlock) > 0 {
		tableBlock = p.Cfg.Tables.UserBlock
	}
	p.db = newDB(ctx.Service("mysql").(mysql.Interface).DB())
	return nil
}

func init() {
	servicehub.Register("dataview-v1", &servicehub.Spec{
		Services:     []string{"chart-block"},
		Dependencies: []string{"mysql"},
		Description:  "chart block",
		ConfigFunc:   func() interface{} { return &pconfig{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
