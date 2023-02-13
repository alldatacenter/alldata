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

package agentinjector

import (
	"context"
	"crypto/tls"
	"fmt"
	"net/http"
	"strings"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda-infra/providers/httpserver"
)

type config struct {
	WebHookAddr        string `file:"webhook_addr"`
	UseInsecure        bool   `file:"use_insecure"` // for test
	CertFile           string `file:"tls_cert_file" default:"/etc/server/certs/monitor-injector.pem"`
	KeyFile            string `file:"tls_key_file" default:"/etc/server/certs/monitor-injector-key.pem"`
	InitContainerImage string `file:"init_container_image" env:"INIT_CONTAINER_IMAGE"`
}

type provider struct {
	Cfg    *config
	Log    logs.Logger
	Router httpserver.Router `autowired:"http-router"`

	server                *http.Server
	initContainerImageTag string
}

func (p *provider) Init(ctx servicehub.Context) error {
	if len(p.Cfg.InitContainerImage) <= 0 {
		p.Cfg.InitContainerImage = version.DockerImage
	}
	idx := strings.LastIndex(p.Cfg.InitContainerImage, ":")
	if idx <= 0 {
		return fmt.Errorf("invalid init_container_image %q", p.Cfg.InitContainerImage)
	}

	mux := p.newServeMux()
	if p.Cfg.UseInsecure {
		p.Router.Any("/**", mux.ServeHTTP)
	} else {
		svr, err := p.newHTTPSServer()
		if err != nil {
			return err
		}
		svr.Handler = mux
		p.server = svr
	}
	return nil
}

func (p *provider) newHTTPSServer() (*http.Server, error) {
	pair, err := tls.LoadX509KeyPair(p.Cfg.CertFile, p.Cfg.KeyFile)
	if err != nil {
		return nil, err
	}
	return &http.Server{
		Addr:      p.Cfg.WebHookAddr,
		TLSConfig: &tls.Config{Certificates: []tls.Certificate{pair}},
	}, nil
}

func (p *provider) Start() error {
	if p.server != nil {
		p.Log.Infof("starting https server at %s", p.Cfg.WebHookAddr)
		return p.server.ListenAndServeTLS("", "")
	}
	return nil
}

func (p *provider) Close() error {
	if p.server != nil {
		return p.server.Shutdown(context.Background())
	}
	return nil
}

func init() {
	servicehub.Register("erda.core.monitor.agent-injector", &servicehub.Spec{
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
