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

package controller

import (
	"fmt"
	"strings"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-proto-go/core/monitor/diagnotor/pb"
	"github.com/erda-project/erda/pkg/common/apis"
	k8sclient "github.com/erda-project/erda/pkg/k8s-client-manager"
)

type config struct {
	AgentImage string `file:"agent_image"`
}

// +provider
type provider struct {
	Cfg      *config
	Log      logs.Logger
	Register transport.Register  `autowired:"service-register" optional:"true"`
	Router   httpserver.Router   `autowired:"http-router"`
	Clients  k8sclient.Interface `autowired:"k8s-client-manager"`

	diagnotorService *diagnotorService
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.Cfg.AgentImage = p.getAgentImage()
	if len(p.Cfg.AgentImage) <= 0 {
		return fmt.Errorf("agent_image is required")
	}
	p.Log.Infof("agent_image = %q", p.Cfg.AgentImage)

	p.diagnotorService = &diagnotorService{p: p}
	if p.Register != nil {
		pb.RegisterDiagnotorServiceImp(p.Register, p.diagnotorService, apis.Options())
	}
	return nil
}

func (p *provider) getAgentImage() string {
	if len(p.Cfg.AgentImage) > 0 {
		return p.Cfg.AgentImage
	}
	if len(version.DockerImage) > 0 {
		idx := strings.LastIndex(version.DockerImage, ":")
		if idx > 0 {
			tag := version.DockerImage[idx+1:]
			base := version.DockerImage[:idx]
			idx = strings.LastIndex(base, "/")
			if idx > 0 {
				return base[:idx+1] + "diagnotor-agent" + ":" + tag
			}
		}
	}
	return ""
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.monitor.diagnotor.DiagnotorService" || ctx.Type() == pb.DiagnotorServiceServerType() || ctx.Type() == pb.DiagnotorServiceHandlerType():
		return p.diagnotorService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.monitor.diagnotor", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
