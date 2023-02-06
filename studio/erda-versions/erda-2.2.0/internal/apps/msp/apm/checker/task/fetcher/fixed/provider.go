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

package fixed

import (
	"context"
	"reflect"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/task/fetcher"
)

type config struct {
	Checkers []*pb.Checker `file:"checkers"`
}

// +provider
type provider struct {
	Cfg      *config
	Log      logs.Logger
	watchers []chan *fetcher.Event
}

func (p *provider) Run(ctx context.Context) error {
	if len(p.watchers) <= 0 {
		return nil
	}
	for _, c := range p.Cfg.Checkers {
		for _, w := range p.watchers {
			select {
			case w <- &fetcher.Event{
				Action: fetcher.ActionAdd,
				Data:   c,
			}:
			case <-ctx.Done():
				return nil
			}
		}
	}
	return nil
}

func (p *provider) Watch() <-chan *fetcher.Event {
	ch := make(chan *fetcher.Event, 16)
	p.watchers = append(p.watchers, ch)
	return ch
}

func init() {
	servicehub.Register("erda.msp.apm.checker.task.fetcher.fixed", &servicehub.Spec{
		Services:   []string{"erda.msp.apm.checker.task.fetcher"},
		Types:      []reflect.Type{reflect.TypeOf((*fetcher.Interface)(nil)).Elem()},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
