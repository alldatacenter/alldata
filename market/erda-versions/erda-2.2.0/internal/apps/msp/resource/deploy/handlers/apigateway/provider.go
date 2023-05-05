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

package apigateway

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
)

type config struct {
	MainClusterInfo struct {
		Name       string `file:"name"`
		RootDomain string `file:"root_domain"`
		Protocol   string `file:"protocol"`
		HttpPort   string `file:"http_port"`
		HttpsPort  string `file:"https_port"`
	} `file:"main_cluster_info"`
}

// +provider
type provider struct {
	*handlers.DefaultDeployHandler
	Cfg *config
	Log logs.Logger
	DB  *gorm.DB `autowired:"mysql-client"`
}

// Init this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	p.DefaultDeployHandler = handlers.NewDefaultHandler(p.DB, p.Log)
	return nil
}

func init() {
	servicehub.Register("erda.msp.resource.deploy.handlers.apigateway", &servicehub.Spec{
		Services: []string{
			"erda.msp.resource.deploy.handlers.apigateway",
		},
		Description: "erda.msp.resource.deploy.handlers.apigateway",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
