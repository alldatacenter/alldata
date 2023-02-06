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

package run

import (
	"context"
	"reflect"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cache"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cancel"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/engine"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/secret"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/user"
)

type config struct {
}

type provider struct {
	Log logs.Logger
	Cfg *config

	dbClient *dbclient.Client

	MySQL       mysqlxorm.Interface
	User        user.Interface
	Cancel      cancel.Interface
	Cache       cache.Interface
	ClusterInfo clusterinfo.Interface
	Secret      secret.Interface
	Engine      engine.Interface
}

func (s *provider) Init(ctx servicehub.Context) error {
	s.dbClient = &dbclient.Client{Engine: s.MySQL.DB()}
	return nil
}

func (s *provider) Run(ctx context.Context) error {
	return nil
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("run", &servicehub.Spec{
		Services:     []string{"run"},
		Types:        []reflect.Type{interfaceType},
		Dependencies: nil,
		Description:  "pipeline run",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
