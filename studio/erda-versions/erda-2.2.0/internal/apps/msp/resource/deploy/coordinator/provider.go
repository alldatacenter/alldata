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

package coordinator

import (
	"fmt"
	"strings"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
)

type config struct {
}

// +provider
type provider struct {
	Cfg            *config
	Log            logs.Logger
	DB             *gorm.DB `autowired:"mysql-client"`
	defaultHandler *handlers.DefaultDeployHandler
	handlers       map[string]handlers.ResourceDeployHandler
}

// Init .
func (p *provider) Init(ctx servicehub.Context) error {
	// build default handler instance for some common use
	p.defaultHandler = handlers.NewDefaultHandler(p.DB, p.Log)

	// load all other registered handlers
	p.handlers = map[string]handlers.ResourceDeployHandler{}
	ctx.Hub().ForeachServices(func(service string) bool {
		if strings.HasPrefix(service, "erda.msp.resource.deploy.handlers.") {
			handler, ok := ctx.Service(service).(handlers.ResourceDeployHandler)
			if !ok {
				panic(fmt.Errorf("service %s is not resource deploy handler", service))
			}
			name := service[len("erda.msp.resource.deploy.handlers."):]
			p.Log.Debugf("load resource deploy handler %q", name)
			p.handlers[name] = handler
		}
		return true
	})

	return nil
}

func init() {
	servicehub.Register("erda.msp.resource.deploy.coordinator", &servicehub.Spec{
		Services: []string{
			"erda.msp.resource.deploy.coordinator",
		},
		DependenciesFunc: func(hub *servicehub.Hub) (list []string) {
			hub.ForeachServices(func(service string) bool {
				if strings.HasPrefix(service, "erda.msp.resource.deploy.handlers.") {
					list = append(list, service)
				}
				return true
			})
			return list
		},
		Description: "erda.msp.resource.deploy.coordinator",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
