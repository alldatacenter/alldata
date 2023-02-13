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

package query

import (
	"net/http"
	"reflect"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
)

var clientType = reflect.TypeOf((*MetricQuery)(nil)).Elem()

type config struct {
	Endpoint string        `file:"endpoint" default:"http://localhost:7076" desc:"collector host"`
	Timeout  time.Duration `file:"timeout" default:"10s"`
}

type define struct{}

func (d *define) Services() []string { return []string{"metricq-client"} }
func (d *define) Types() []reflect.Type {
	return []reflect.Type{
		clientType,
	}
}
func (d *define) Summary() string { return "metricq-client" }

func (d *define) Description() string { return d.Summary() }

func (d *define) Config() interface{} { return &config{} }

func (d *define) Creator() servicehub.Creator {
	return func() servicehub.Provider {
		return &provider{}
	}
}

type provider struct {
	Cfg    *config
	Log    logs.Logger
	Client *queryClient
}

// Init .
func (p *provider) Init(ctx servicehub.Context) error {
	client := &queryClient{
		endpoint:   p.Cfg.Endpoint,
		timeout:    p.Cfg.Timeout,
		httpClient: &http.Client{},
	}
	p.Client = client
	return nil
}

// Provide .
func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p.Client
}

func init() {
	servicehub.RegisterProvider("metricq-client", &define{})
}
