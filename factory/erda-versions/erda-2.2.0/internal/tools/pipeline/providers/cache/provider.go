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

package cache

import (
	"reflect"
	"sync"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
)

type config struct {
}

type provider struct {
	Log       logs.Logger
	Cfg       *config
	MySQL     mysqlxorm.Interface
	ActionMgr actionmgr.Interface

	dbClient *dbclient.Client
	bdl      *bundle.Bundle

	cacheMap sync.Map
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.dbClient = &dbclient.Client{Engine: p.MySQL.DB()}
	p.bdl = bundle.New(bundle.WithAllAvailableClients())
	p.cacheMap = sync.Map{}
	return nil
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("cache", &servicehub.Spec{
		Services:    []string{"cache"},
		Description: "cache",
		Types:       []reflect.Type{interfaceType},
		ConfigFunc:  func() interface{} { return &config{} },
		Creator:     func() servicehub.Provider { return &provider{} },
	})
}
