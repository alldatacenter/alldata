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

package report

import (
	"net/http"

	"github.com/recallsong/go-utils/logs"

	"github.com/erda-project/erda-infra/base/servicehub"
)

type ReportMode string

type config struct {
	ReportConfig ReportConfig `file:"report_config"`
}

type CollectorConfig struct {
	Addr     string `file:"addr" env:"COLLECTOR_ADDR" default:"collector:7076"`
	UserName string `file:"username" env:"COLLECTOR_AUTH_USERNAME"`
	Password string `file:"password" env:"COLLECTOR_AUTH_PASSWORD"`
	Retry    int    `file:"retry" env:"TELEMETRY_REPORT_STRICT_RETRY" default:"3"`
}

type ReportConfig struct {
	Mode    string `file:"mode" default:"performance"`
	UdpHost string `file:"udp_host" env:"HOST_IP" default:"localhost"`
	UdpPort string `file:"upd_port" env:"HOST_PORT" default:"7082"`

	Collector CollectorConfig `file:"collector"`

	BufferSize int `file:"buffer_size" env:"REPORT_BUFFER_SIZE" default:"200"`
}

type provider struct {
	Cfg        *config
	Log        logs.Logger
	httpClient *ReportClient
}

func (p *provider) Init(ctx servicehub.Context) error {
	client := &ReportClient{
		CFG: &config{
			ReportConfig: p.Cfg.ReportConfig,
		},
		HttpClient: &http.Client{},
	}
	p.httpClient = client
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p.httpClient
}

func init() {
	servicehub.Register("metric-report-client", &servicehub.Spec{
		Services: []string{"metric-report-client"},
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
