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

package task

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/plugins"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/task/fetcher"
	"github.com/erda-project/erda/internal/pkg/metrics/report"
)

type config struct {
	DefaultPeriodicWorkerInterval time.Duration `file:"default_periodic_worker_interval" default:"30s"`
}

// +provider
type provider struct {
	Cfg     *config
	Log     logs.Logger
	Report  report.MetricReport `autowired:"metric-report-client" optional:"true"`
	Fetcher fetcher.Interface   `autowired:"erda.msp.apm.checker.task.fetcher"`
	events  <-chan *fetcher.Event
	plugins map[string]plugins.Interface
	workers WorkerManager
}

func (p *provider) Init(ctx servicehub.Context) error {
	if p.Cfg.DefaultPeriodicWorkerInterval > 0 {
		defaultPeriodicInterval = p.Cfg.DefaultPeriodicWorkerInterval
	}
	p.workers = newSimpleWorkerManager(p.Log)
	p.plugins = make(map[string]plugins.Interface)
	ctx.Hub().ForeachServices(func(service string) bool {
		if strings.HasPrefix(service, "erda.msp.apm.checker.task.plugins.") {
			plugin, ok := ctx.Service(service).(plugins.Interface)
			if !ok {
				panic(fmt.Errorf("service %s is not checker plugins", service))
			}
			name := service[len("erda.msp.apm.checker.task.plugins."):]
			p.Log.Debugf("load checker plugin %q", name)
			p.plugins[name] = plugin
		}
		return true
	})
	p.events = p.Fetcher.Watch()
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	taskCtx := newTaskContext(ctx, p.Report)
	for {
		select {
		case event, ok := <-p.events:
			if !ok {
				return nil
			}
			switch event.Action {
			case fetcher.ActionAdd, fetcher.ActionUpdate:
				plugin, ok := p.plugins[event.Data.Type]
				if !ok {
					p.Log.Debugf("invalid checker plugin type %q", event.Data.Type)
					continue
				}
				err := plugin.Validate(event.Data)
				if err != nil {
					p.Log.Debugf("invalid checker checker: %v", err)
					continue
				}

				// setup tags
				if event.Data.Tags == nil {
					event.Data.Tags = make(map[string]string)
				}
				event.Data.Tags["_meta"] = "true"
				event.Data.Tags["type"] = event.Data.Type

				// new checker handler
				checker, err := plugin.New(event.Data)
				if err != nil {
					p.Log.Debugf("fail to create %s checker: %v", event.Data.Type, err)
					continue
				}
				w, err := NewWorker(p.Log, event.Data, taskCtx, checker)
				if err != nil {
					p.Log.Debugf("fail to create %s checker worker: %v", event.Data.Type, err)
					continue
				}
				p.Log.Infof("start worker for checker(id=%d type=%s)", event.Data.Id, event.Data.Type)
				go w.Run(ctx)
				p.workers.Put(event.Data.Id, w)
			case fetcher.ActionDelete:
				p.workers.Remove(event.Data.Id)
			}
		case <-ctx.Done():
			return nil
		}
	}
}

func init() {
	servicehub.Register("erda.msp.apm.checker.task", &servicehub.Spec{
		DependenciesFunc: func(hub *servicehub.Hub) (list []string) {
			hub.ForeachServices(func(service string) bool {
				if strings.HasPrefix(service, "erda.msp.apm.checker.task.plugins.") {
					list = append(list, service)
				}
				return true
			})
			return list
		},
		ConfigFunc: func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
