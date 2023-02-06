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
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/compressor"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/exporters/collector/auth"
)

var providerName = plugins.WithPrefixExporter("collector")

type config struct {
	URL             string        `file:"url"`
	Timeout         time.Duration `file:"timeout" default:"3s"`
	Serializer      string        `file:"serializer" default:"json"`
	ContentEncoding string        `file:"content_encoding" default:"gzip"`
	Authentication  *struct {
		Type    string                 `file:"type" default:"token"`
		Options map[string]interface{} `file:"options"`
	} `file:"authentication"`
	Headers map[string]string `file:"headers"`
	// capability of old data format
	Compatibility bool `file:"compatibility" default:"true"`
}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger

	client *http.Client
	au     auth.Authenticator
	cp     compressor.Compressor
}

func (p *provider) ExportMetric(items ...*metric.Metric) error {
	buf, err := json.Marshal(items)
	if err != nil {
		return fmt.Errorf("serialize err: %w", err)
	}
	buf, err = p.cp.Compress(buf)
	if err != nil {
		return fmt.Errorf("compress err: %w", err)
	}
	req, err := http.NewRequest(http.MethodPost, p.Cfg.URL, bytes.NewReader(buf))
	if err != nil {
		return fmt.Errorf("create request err: %w", err)
	}
	setHeaders(req, p.Cfg.Headers)
	p.au.Secure(req)
	code, err := doRequest(p.client, req)
	if err != nil {
		return fmt.Errorf("do request err: %w", err)
	}
	if code < 200 || code >= 300 {
		return fmt.Errorf("response status code %d is not success", code)
	}
	return nil
}

func (p *provider) ExportLog(items ...*log.Log) error     { return nil }
func (p *provider) ExportSpan(items ...*trace.Span) error { return nil }
func (p *provider) ExportRaw(items ...*odata.Raw) error   { return nil }

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func (p *provider) Connect() error {
	client := &http.Client{
		Timeout: p.Cfg.Timeout,
	}
	p.client = client

	headReq, err := http.NewRequest(http.MethodHead, p.Cfg.URL, nil)
	if err != nil {
		return err
	}
	code, err := doRequest(p.client, headReq)
	if err != nil {
		return err
	}
	if code >= 500 {
		return fmt.Errorf("invalid response code: %d", code)
	}
	return nil
}

func (p *provider) Init(ctx servicehub.Context) error {
	if err := p.Connect(); err != nil {
		return fmt.Errorf("try connect to remote err: %w", err)
	}
	if err := p.createAuthenticator(); err != nil {
		return fmt.Errorf("createAuthenticator err: %w", err)
	}
	if err := p.createCompressor(); err != nil {
		return fmt.Errorf("createCompressor err: %w", err)
	}
	return nil
}

func (p *provider) createAuthenticator() error {
	cfg := p.Cfg.Authentication.Options
	switch auth.AuthenticationType(p.Cfg.Authentication.Type) {
	case auth.Basic:
		au, err := auth.NewBasicAuth(cfg)
		if err != nil {
			return err
		}
		p.au = au
	case auth.Token:
		au, err := auth.NewTokenAuth(cfg)
		if err != nil {
			return err
		}
		p.au = au
	default:
		return fmt.Errorf("invalid authentication type: %q", p.Cfg.Authentication.Type)
	}
	return nil
}

func (p *provider) createCompressor() error {
	switch p.Cfg.ContentEncoding {
	case GZIPEncoding:
		p.cp = compressor.NewGzipEncoder(3)
	default:
		return fmt.Errorf("invalid ContentEncoding: %q", p.Cfg.ContentEncoding)
	}
	return nil
}

func init() {
	servicehub.Register(providerName, &servicehub.Spec{
		Services: []string{
			providerName,
		},
		Description: "here is description of erda.oap.collector.exporter.collector",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
