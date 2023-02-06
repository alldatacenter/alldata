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

package clusterinfo

import (
	"context"
	"reflect"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
)

var pd *provider

type config struct {
	ClusterName              string        `env:"DICE_CLUSTER_NAME"`
	ErdaNamespace            string        `env:"DICE_NAMESPACE"`
	IsEdge                   bool          `env:"DICE_IS_EDGE" default:"false"`
	RetryClusterHookInterval time.Duration `file:"retry_cluster_hook_interval" default:"5s"`
	RefreshClustersInterval  time.Duration `file:"refresh_clusters_interval" env:"REFRESH_CLUSTERS_INTERVAL"`
}

type provider struct {
	Log logs.Logger
	Cfg *config

	bdl      *bundle.Bundle
	cache    cache
	notifier Notifier

	EdgeRegister edgepipeline_register.Interface
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithClusterManager(), bundle.WithCoreServices())
	p.cache = NewClusterInfoCache()
	p.notifier = NewClusterInfoNotifier()
	pd = p
	return nil
}

func (p *provider) registerClusterHookUntilSuccess(ctx context.Context) {
	for {
		select {
		case <-ctx.Done():
			return
		default:
			err := p.registerClusterHook()
			if err == nil {
				return
			}
			p.Log.Errorf("failed to register cluster hook(auto retry), err: %v", err)
			time.Sleep(p.Cfg.RetryClusterHookInterval)
		}
	}
}

func (p *provider) Run(ctx context.Context) error {
	go p.continueUpdateAndRefresh()
	p.EdgeRegister.OnCenter(p.registerClusterHookUntilSuccess)
	return nil
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("clusterinfo", &servicehub.Spec{
		Services:     []string{"clusterinfo"},
		Types:        []reflect.Type{interfaceType},
		Dependencies: nil,
		Description:  "pipeline clusterinfo",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
