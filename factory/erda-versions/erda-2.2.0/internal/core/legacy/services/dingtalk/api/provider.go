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

package api

import (
	"github.com/go-redis/redis"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/caches"
	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/manager"
)

type config struct {
}

type provider struct {
	Cfg     *config
	Log     logs.Logger
	Redis   *redis.Client `autowired:"redis-client" optional:"true"`
	manager *manager.Manager
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.manager = manager.NewManager(p.Log, caches.NewRedis(p.Redis))
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p.manager
}

func init() {
	servicehub.Register("dingtalk.api", &servicehub.Spec{
		Services: []string{"dingtalk.api"},
		Creator:  func() servicehub.Provider { return &provider{} },
	})
}
