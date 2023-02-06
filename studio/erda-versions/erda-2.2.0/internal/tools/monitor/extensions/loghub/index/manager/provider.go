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

package manager

import (
	"sync/atomic"
	"time"

	"github.com/olivere/elastic"
	"github.com/robfig/cron"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	"github.com/erda-project/erda-infra/providers/httpserver"
)

type config struct {
	IndexPrefix       string `file:"index_prefix" default:"rlogs-"`
	IndexTemplateName string `file:"index_template_name"`
	IndexTemplateFile string `file:"index_template_file"`

	// reload
	RequestTimeout time.Duration `file:"request_timeout" default:"120s"`
	ReloadInterval time.Duration `file:"reload_interval" default:"2m"`

	// clean
	IndexTTL           time.Duration `file:"index_ttl" default:"168h"`
	EnableIndexClean   bool          `file:"enable_index_clean"`
	IndexCheckInterval string        `file:"index_check_interval"`

	// rollover
	EnableIndexRollover bool   `file:"enable_index_rollover"`
	RolloverBodyFile    string `file:"rollover_body_file"`
	RolloverInterval    string `file:"rollover_interval"`
}

type provider struct {
	C      *config
	L      logs.Logger
	client *elastic.Client
	cron   *cron.Cron

	rolloverBody string

	reload     chan struct{}
	indices    atomic.Value
	timeRanges map[string]*timeRange
}

func (p *provider) Init(ctx servicehub.Context) error {
	es := ctx.Service("elasticsearch@logs").(elasticsearch.Interface)
	p.client = es.Client()
	err := p.setupIndexTemplate(p.client)
	if err != nil {
		return err
	}
	err = p.loadRolloverBody()
	if err != nil {
		return err
	}
	p.cron = cron.New()
	routes := ctx.Service("http-server").(httpserver.Router)
	return p.intRoutes(routes)
}

// Start .
func (p *provider) Start() error {
	// load
	go func() {
		tick := time.Tick(p.C.ReloadInterval)
		for {
			p.reloadAllIndices()
			select {
			case <-tick:
			case _, ok := <-p.reload:
				if !ok {
					return
				}
			}
		}
	}()

	// clean
	if p.C.EnableIndexClean && len(p.C.IndexCheckInterval) > 0 {
		p.cleanIndices()
		p.cron.AddFunc(p.C.IndexCheckInterval, p.cleanIndices)
	}

	// rollover
	if p.C.EnableIndexRollover && len(p.C.RolloverInterval) > 0 && len(p.rolloverBody) > 0 {
		p.doRolloverAlias()
		p.cron.AddFunc(p.C.RolloverInterval, p.doRolloverAlias)
	}
	p.cron.Run()
	return nil
}

func (p *provider) Close() error {
	p.cron.Stop()
	close(p.reload)
	return nil
}

func init() {
	servicehub.Register("logs-index-manager", &servicehub.Spec{
		Services:     []string{"logs-index-manager"},
		Dependencies: []string{"elasticsearch@logs", "http-server"},
		Description:  "logs index manager",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{
				timeRanges: make(map[string]*timeRange),
				reload:     make(chan struct{}),
			}
		},
	})
}
