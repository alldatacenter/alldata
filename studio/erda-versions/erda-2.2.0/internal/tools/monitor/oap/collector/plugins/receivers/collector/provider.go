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
	"strings"

	"github.com/labstack/echo"
	"github.com/labstack/echo/middleware"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/authentication"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins"
)

var providerName = plugins.WithPrefixReceiver("collector")

type config struct {
	MetadataKeyOfTopic string `file:"metadata_key_of_topic"`
	Auth               struct {
		Skip bool `file:"skip"`
	} `file:"auth"`
}

// +provider
type provider struct {
	Cfg       *config
	Log       logs.Logger
	Router    httpserver.Router        `autowired:"http-router"`
	Validator authentication.Validator `autowired:"erda.oap.collector.authentication.Validator"`

	consumer model.ObservableDataConsumerFunc
}

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func (p *provider) RegisterConsumer(consumer model.ObservableDataConsumerFunc) {
	p.consumer = consumer
}

func (p *provider) tokenAuth() interface{} {
	return middleware.KeyAuthWithConfig(middleware.KeyAuthConfig{
		Validator: func(s string, context echo.Context) (bool, error) {
			clusterName := context.Request().Header.Get(apistructs.AuthClusterKeyHeader)
			if clusterName == "" {
				return false, nil
			}

			if p.Validator.Validate(strings.ToLower(tokenpb.ScopeEnum_CMP_CLUSTER.String()), clusterName, s) {
				return true, nil
			}

			return false, nil
		},
		Skipper: func(context echo.Context) bool {
			if p.Cfg.Auth.Skip {
				return true
			}
			return false
		},
	})
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	// old
	p.Router.POST("/api/v1/collect/logs/:source", p.collectLogs, p.tokenAuth())
	p.Router.POST("/api/v1/collect/:metric", p.collectMetric, p.tokenAuth())

	return nil
}

func init() {
	servicehub.Register(providerName, &servicehub.Spec{
		Services:    []string{providerName},
		Description: "here is description of erda.oap.collector.receiver.collector",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
