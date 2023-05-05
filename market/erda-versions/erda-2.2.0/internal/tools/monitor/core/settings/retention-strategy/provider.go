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

package retention

import (
	"context"
	"fmt"
	"sync/atomic"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
)

// Interface .
type Interface interface {
	GetTTL(key string) time.Duration
	DefaultTTL() time.Duration
	GetConfigKey(name string, tags map[string]string) string
	GetTTLByTags(name string, tags map[string]string) time.Duration
	Loading(ctx context.Context)
}

var _ Interface = (*provider)(nil)

type (
	config struct {
		DefaultTTL       time.Duration `file:"default_ttl" default:"168h"`
		LoadFromDatabase bool          `file:"load_from_database"`
		ReloadInterval   time.Duration `file:"ttl_reload_interval" default:"3m"`
		PrintDetails     bool          `file:"print_details"`
	}
	provider struct {
		Cfg   *config
		Log   logs.Logger
		DB    *gorm.DB `autowired:"mysql-client"`
		typ   string
		value atomic.Value
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	if len(ctx.Label()) < 0 {
		return fmt.Errorf("provider label is required")
	}
	p.typ = ctx.Label()

	err = p.loadConfig()
	if err != nil {
		return err
	}

	routes := ctx.Service("http-router", interceptors.CORS()).(httpserver.Router)
	err = p.intRoutes(routes)
	if err != nil {
		return fmt.Errorf("failed to init routes: %s", err)
	}
	return nil
}

func (p *provider) Loading(ctx context.Context) {
	if p.Cfg.LoadFromDatabase {
		timer := time.NewTimer(0)
		defer timer.Stop()
		defer p.value.Store((*retentionConfig)(nil))
		for {
			p.loadConfig()
			select {
			case <-timer.C:
			case <-ctx.Done():
				return
			}
			timer.Reset(p.Cfg.ReloadInterval)
		}
	}
}

func init() {
	servicehub.Register("storage-retention-strategy", &servicehub.Spec{
		Services:     []string{"storage-retention-strategy"},
		Dependencies: []string{"http-router"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
