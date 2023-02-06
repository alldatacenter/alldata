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

package k8sclient

import (
	"fmt"
	"reflect"
	"time"

	"github.com/bluele/gcache"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/pkg/k8sclient"
	"github.com/erda-project/erda/pkg/k8sclient/scheme"
)

// Interface .
type Interface interface {
	GetClient(clusterName string) (kubernetes.Interface, *rest.Config, error)
	GetCRClient(clusterName string) (client.Client, *rest.Config, error)
}

type (
	config struct {
		CacheTTL  time.Duration `file:"cache_ttl" default:"10m"`
		CacheSize int           `file:"cache_size" default:"5000"`
	}
	provider struct {
		Cfg   *config
		Log   logs.Logger
		cache gcache.Cache
	}
	cacheItem struct {
		Config    *rest.Config
		Clientset kubernetes.Interface
		CRClient  client.Client
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.cache = gcache.New(p.Cfg.CacheSize).LRU().Expiration(p.Cfg.CacheTTL).LoaderFunc(func(key interface{}) (interface{}, error) {
		rc, err := k8sclient.GetRestConfig(key.(string))
		if err != nil {
			return nil, err
		}
		client, err := k8sclient.NewForRestConfig(rc, k8sclient.WithSchemes(scheme.LocalSchemeBuilder...))
		if err != nil {
			return nil, err
		}
		return &cacheItem{
			Config:    rc,
			Clientset: client.ClientSet,
			CRClient:  client.CRClient,
		}, nil
	}).Build()
	return nil
}

func (p *provider) GetClient(clusterName string) (kubernetes.Interface, *rest.Config, error) {
	val, err := p.cache.Get(clusterName)
	if err != nil {
		return nil, nil, err
	}
	if val == nil {
		return nil, nil, fmt.Errorf("not found client for %q", clusterName)
	}
	item, _ := val.(*cacheItem)
	return item.Clientset, item.Config, nil
}

func (p *provider) GetCRClient(clusterName string) (client.Client, *rest.Config, error) {
	val, err := p.cache.Get(clusterName)
	if err != nil {
		return nil, nil, err
	}
	if val == nil {
		return nil, nil, fmt.Errorf("not found client for %q", clusterName)
	}
	item, _ := val.(*cacheItem)
	return item.CRClient, item.Config, nil
}

func init() {
	servicehub.Register("k8s-client-manager", &servicehub.Spec{
		Services: []string{"k8s-client-manager"},
		Types: []reflect.Type{
			reflect.TypeOf((*Interface)(nil)).Elem(),
		},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
