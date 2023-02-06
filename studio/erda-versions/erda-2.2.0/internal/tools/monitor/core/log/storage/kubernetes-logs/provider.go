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

package kuberneteslogs

import (
	"fmt"
	"io"
	"time"

	"github.com/go-redis/redis"
	"github.com/recallsong/go-utils/encoding/jsonx"
	v1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	k8sclient "github.com/erda-project/erda/pkg/k8s-client-manager"
)

type (
	config struct {
		BufferLines int           `file:"buffer_lines" default:"1024"`
		TimeSpan    time.Duration `file:"time_span" default:"3m"`
	}
	provider struct {
		Cfg     *config
		Log     logs.Logger
		Redis   *redis.Client       `autowired:"redis-client"`
		Clients k8sclient.Interface `autowired:"k8s-client-manager"`

		ctx servicehub.Context
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.ctx = ctx
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return &cStorage{
		log: p.Log,
		getQueryFunc: func(clusterName string) (func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error), error) {
			client, _, err := p.Clients.GetClient(clusterName)
			if err != nil {
				return nil, err
			}
			if client == nil {
				return nil, fmt.Errorf("not found clientset")
			}
			return func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error) {
				if it.debug {
					fmt.Printf("[%v] namespace: %v,podname: %v, opts: %s \n", opts.SinceTime.UnixNano(), it.podNamespace, it.podName, jsonx.MarshalAndIndent(opts))
				}
				return client.CoreV1().Pods(it.podNamespace).GetLogs(it.podName, opts).Stream(it.ctx)
			}, nil
		},
		bufferLines: int64(p.Cfg.BufferLines),
		timeSpan:    int64(p.Cfg.TimeSpan),
	}
}

func init() {
	servicehub.Register("kubernetes-logs-storage", &servicehub.Spec{
		Services:   []string{"log-storage-kubernetes-reader"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
