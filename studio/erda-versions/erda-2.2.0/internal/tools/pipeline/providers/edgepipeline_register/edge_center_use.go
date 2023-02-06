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

package edgepipeline_register

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/pkg/safe"
)

func (p *provider) mustStarted() {
	p.Lock()
	defer p.Unlock()
	if !p.started {
		panic(fmt.Errorf("cannot invoke this method before started"))
	}
}

type forCenterUse struct {
	handlersOnCenter chan func(ctx context.Context)
}

type forEdgeUse struct {
	handlersOnEdge chan func(ctx context.Context)
}

func (p *provider) OnEdge(f func(context.Context)) {
	p.mustStarted()
	p.forEdgeUse.handlersOnEdge <- f
}

func (p *provider) OnCenter(f func(context.Context)) {
	p.mustStarted()
	p.forCenterUse.handlersOnCenter <- f
}

func (p *provider) startEdgeCenterUse(ctx context.Context) {
	p.Lock()
	p.started = true
	p.Unlock()
	go func() {
		for {
			select {
			case f := <-p.forCenterUse.handlersOnCenter:
				if !p.Cfg.IsEdge {
					safe.Go(func() { f(ctx) })
				}
			case f := <-p.forEdgeUse.handlersOnEdge:
				if p.Cfg.IsEdge {
					safe.Go(func() { f(ctx) })
				}
			case <-ctx.Done():
				return
			}
		}
	}()
}
