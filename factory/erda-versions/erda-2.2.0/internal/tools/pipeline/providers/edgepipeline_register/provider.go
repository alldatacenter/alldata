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
	"reflect"
	"sync"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/gorilla/schema"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher"
	httpinput "github.com/erda-project/erda/internal/core/messenger/eventbox/input/http"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/webhook"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker"
)

type Config struct {
	IsEdge                       bool          `env:"DICE_IS_EDGE" default:"false"`
	ErdaNamespace                string        `env:"DICE_NAMESPACE"`
	ClusterName                  string        `env:"DICE_CLUSTER_NAME"`
	AllowedSources               []string      `file:"allowed_sources" env:"EDGE_ALLOWED_SOURCES"` // env support comma-seperated string
	PipelineAddr                 string        `env:"PIPELINE_ADDR"`
	PipelineHost                 string        `env:"PIPELINE_HOST"`
	ClusterManagerEndpoint       string        `file:"cluster_manager_endpoint" desc:"cluster manager endpoint"`
	ClusterAccessKey             string        `file:"cluster_access_key" desc:"cluster access key, if specified will doesn't start watcher"`
	RetryConnectDialerInterval   time.Duration `file:"retry_cluster_hook_interval" default:"1s"`
	EtcdPrefixOfClusterAccessKey string        `file:"cluster_access_key_etcd_prefix" env:"EDGE_PIPELINE_CLUSTER_ACCESS_KEY_ETCD_PREFIX"`
	UpdateClientInterval         time.Duration `file:"update_client_interval" env:"UPDATE_CLIENT_INTERVAL" default:"60s"`
}

type provider struct {
	sync.Mutex

	Log      logs.Logger
	Cfg      *Config
	LW       leaderworker.Interface
	Register transport.Register

	bdl                *bundle.Bundle
	EtcdClient         *clientv3.Client
	started            bool
	forCenterUse       forCenterUse
	forEdgeUse         forEdgeUse
	queryStringDecoder *schema.Decoder
	eventHandlers      []EventHandler
	edgeClients        map[string]apistructs.ClusterManagerClientDetail

	webHookHTTP     *webhook.WebHookHTTP
	httpI           *httpinput.HttpInput
	eventDispatcher dispatcher.Dispatcher
}

func (p *provider) Init(ctx servicehub.Context) error {
	for _, s := range p.Cfg.AllowedSources {
		p.Log.Infof("allowed source: %s", s)
	}
	if err := p.checkEtcdPrefixKey(p.Cfg.EtcdPrefixOfClusterAccessKey); err != nil {
		return err
	}
	p.bdl = bundle.New(bundle.WithClusterManager(), bundle.WithCoreServices())
	p.bdl = bundle.New(bundle.WithClusterManager())
	p.queryStringDecoder = schema.NewDecoder()
	webHookHTTP, err := webhook.NewWebHookHTTP()
	if err != nil {
		return err
	}
	p.webHookHTTP = webHookHTTP
	httpI, err := httpinput.New()
	if err != nil {
		return err
	}
	p.httpI = httpI
	eventDispatcher, err := p.newEventDispatcher()
	if err != nil {
		return err
	}
	p.eventDispatcher = eventDispatcher
	p.forEdgeUse.handlersOnEdge = make(chan func(context.Context), 0)
	p.forCenterUse.handlersOnCenter = make(chan func(context.Context), 0)
	p.eventHandlers = make([]EventHandler, 0)
	p.edgeClients = make(map[string]apistructs.ClusterManagerClientDetail)
	p.startEdgeCenterUse(ctx)
	p.OnEdge(p.watchClusterCredential)
	p.OnEdge(p.initWebHookEndpoints)
	p.OnEdge(p.startEventDispatcher)
	p.waitingEdgeReady(ctx)
	p.OnCenter(p.registerClientHookUntilSuccess)
	p.OnCenter(p.continuousUpdateClient)
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	p.LW.OnLeader(p.RegisterEdgeToDialer)
	return nil
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("edgepipeline_register", &servicehub.Spec{
		Services:     []string{"edgepipeline_register"},
		Types:        []reflect.Type{interfaceType},
		Dependencies: nil,
		Description:  "pipeline edgepipeline register agent",
		ConfigFunc:   func() interface{} { return &Config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
