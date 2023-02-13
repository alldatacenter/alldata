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

package actionmgr

import (
	"context"
	"reflect"
	"sync"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/goroutinepool"
)

type config struct {
	RefreshInterval    time.Duration `file:"refresh_interval" default:"1m"`
	PoolSize           int           `file:"pool_size" default:"20"`
	ActionInitFilePath string        `file:"action_init_file_path" default:"common-conf/extensions-init"`
}

// +provider
type provider struct {
	Cfg          *config
	Log          logs.Logger
	Register     transport.Register
	MySQL        mysqlxorm.Interface
	EdgeRegister edgepipeline_register.Interface
	ClusterInfo  clusterinfo.Interface

	sync.Mutex
	bdl *bundle.Bundle
	*actionService

	actionsCache        map[string]apistructs.ExtensionVersion // key: type@version, see getActionNameVersion
	defaultActionsCache map[string]apistructs.ExtensionVersion // key: type (only type, no version)
	pools               *goroutinepool.GoroutinePool
}

func (s *provider) Init(ctx servicehub.Context) error {
	s.actionService = &actionService{s, &db.Client{Interface: s.MySQL}, s.EdgeRegister, s.ClusterInfo}
	if s.Register != nil {
		pb.RegisterActionServiceImp(s.Register, s.actionService, apis.Options())
	}
	s.actionsCache = make(map[string]apistructs.ExtensionVersion)
	s.defaultActionsCache = make(map[string]apistructs.ExtensionVersion)
	s.pools = goroutinepool.New(s.Cfg.PoolSize)
	s.bdl = bundle.New(bundle.WithAllAvailableClients())
	s.dbClient = &db.Client{Interface: s.MySQL}
	go func() {
		if s.EdgeRegister.IsEdge() {
			return
		}
		s.actionService.InitAction(s.Cfg.ActionInitFilePath)
	}()
	return nil
}

func (s *provider) Run(ctx context.Context) error {
	s.edgeRegister.OnCenter(s.continuousRefreshAction)
	return nil
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("actionmgr", &servicehub.Spec{
		Types:                []reflect.Type{interfaceType},
		OptionalDependencies: []string{"service-register"},
		Description:          "pipeline action mgr",
		ConfigFunc:           func() interface{} { return &config{} },
		Creator:              func() servicehub.Provider { return &provider{} },
	})
}
