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

package collector

import (
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/authentication"
)

type config struct {
	Auth struct {
		Username string `file:"username"`
		Password string `file:"password"`
		Force    bool   `file:"force"`
		Skip     bool   `file:"skip"`
	}
	Output         kafka.ProducerConfig `file:"output"`
	TaSamplingRate float64              `file:"ta_sampling_rate" default:"100"`
}

type define struct{}

func (m *define) Services() []string     { return []string{"metrics-collector"} }
func (m *define) Dependencies() []string { return []string{"http-server", "kafka-producer"} }
func (m *define) Summary() string        { return "log and metrics collector" }
func (m *define) Description() string    { return m.Summary() }
func (m *define) Config() interface{}    { return &config{} }
func (m *define) Creator() servicehub.Creator {
	return func() servicehub.Provider { return &provider{} }
}

// provider .
type provider struct {
	Cfg    *config
	Logger logs.Logger
	writer writer.Writer
	Kafka  kafka.Interface `autowired:"kafka"`

	auth      *Authenticator
	Validator authentication.Validator `autowired:"erda.oap.collector.authentication.Validator"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	w, err := p.Kafka.NewProducer(&p.Cfg.Output)
	if err != nil {
		return fmt.Errorf("fail to create kafka producer: %s", err)
	}
	p.writer = w

	p.auth = NewAuthenticator(
		WithLogger(p.Logger),
		WithValidator(p.Validator),
		WithConfig(p.Cfg),
	)

	r := ctx.Service("http-server",
		// telemetry.HttpMetric(),
		interceptors.CORS(),
		interceptors.Recover(p.Logger),
	).(httpserver.Router)
	if err := p.intRoute(r); err != nil {
		return fmt.Errorf("fail to init route: %s", err)
	}
	return nil
}

func init() {
	servicehub.RegisterProvider("monitor-collector", &define{})
}
