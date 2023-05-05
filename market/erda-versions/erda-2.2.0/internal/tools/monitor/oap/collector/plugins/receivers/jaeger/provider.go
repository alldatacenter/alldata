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

package jaeger

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-proto-go/oap/collector/receiver/jaeger/pb"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/interceptor"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins"
)

var providerName = plugins.WithPrefixReceiver("jaeger")

type config struct {
	// some fields of config for this provider
	// Kafka struct {
	// 	Producer kafka.ProducerConfig `file:"producer"  desc:"kafka Producer Config"`
	// } `file:"kafka"`
}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger

	jaegerService pb.JaegerServiceServer
	Register      transport.Register       `autowired:"service-register" optional:"true"`
	Interceptors  interceptor.Interceptors `autowired:"erda.oap.collector.interceptor.Interceptor"`

	consumer model.ObservableDataConsumerFunc
}

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func (p *provider) ComponentID() model.ComponentID {
	return model.ComponentID(providerName)
}

func (p *provider) RegisterConsumer(consumer model.ObservableDataConsumerFunc) {
	p.consumer = consumer
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	if p.Register != nil {
		p.jaegerService = &jaegerServiceImpl{Log: p.Log, p: p}
		pb.RegisterJaegerServiceImp(p.Register, p.jaegerService,
			transport.WithHTTPOptions(transhttp.WithDecoder(ThriftDecoder), transhttp.WithInterceptor(p.Interceptors.ExtractHttpHeaders)),
			transport.WithInterceptors(p.Interceptors.Authentication, p.Interceptors.SpanTagOverwrite),
		)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.oap.collector.receiver.jaeger.JaegerService" || ctx.Type() == pb.JaegerServiceServerType() || ctx.Type() == pb.JaegerServiceHandlerType():
		return p.jaegerService
	}
	return p
}

func init() {
	servicehub.Register(providerName, &servicehub.Spec{
		Services:    pb.ServiceNames(providerName),
		Description: "here is description of erda.oap.collector.receiver.jaeger",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
