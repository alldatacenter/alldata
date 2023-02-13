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

package secret

import (
	"context"
	"reflect"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cache"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cancel"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/user"
)

type config struct {
}

type provider struct {
	Log logs.Logger
	Cfg *config

	MySQL       mysqlxorm.Interface
	User        user.Interface
	Cancel      cancel.Interface
	Cache       cache.Interface
	ClusterInfo clusterinfo.Interface
	CmsService  cmspb.CmsServiceServer

	dbClient *dbclient.Client
	bdl      *bundle.Bundle
}

func (s *provider) Init(ctx servicehub.Context) error {
	s.dbClient = &dbclient.Client{Engine: s.MySQL.DB()}
	s.bdl = bundle.New(bundle.WithAllAvailableClients())
	return nil
}

func (s *provider) Run(ctx context.Context) error {
	return nil
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("secret", &servicehub.Spec{
		Services:     []string{"secret"},
		Types:        []reflect.Type{interfaceType},
		Dependencies: nil,
		Description:  "secret",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
