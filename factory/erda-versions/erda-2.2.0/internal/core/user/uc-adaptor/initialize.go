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

package uc_adaptor

import (
	"github.com/gorilla/schema"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/conf"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/dao"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/endpoints"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/service/adaptor"
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/ucclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	// "terminus.io/dice/telemetry/promxp"
)

// Initialize 初始化应用启动服务.
func Initialize() error {
	// 装载配置信息
	conf.Load()
	if conf.Debug() {
		logrus.SetLevel(logrus.DebugLevel)
		logrus.Infof("set log level: %s", logrus.DebugLevel)
	}

	ep, err := initEndpoints()
	if err != nil {
		return err
	}

	server := httpserver.New(conf.ListenAddr())
	// server.Router().Path("/metrics").Methods(http.MethodGet).Handler(promxp.Handler("uc-adaptor"))
	server.Router().UseEncodedPath()
	server.RegisterEndpoint(ep.Routes())
	logrus.Infof("start the service and listen on address: \"%s\"", conf.ListenAddr())

	return server.ListenAndServe()
}

// 初始化 Endpoints
func initEndpoints() (*endpoints.Endpoints, error) {
	// init bundle
	bdl := bundle.New(
		bundle.WithCoreServices(),
	)

	// init uc client
	uc := ucclient.NewUCClient()

	// init db
	db, err := dao.Open()
	if err != nil {
		return nil, err
	}

	// queryStringDecoder
	queryStringDecoder := schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)

	// ucAdaptor
	ucAdaptor := adaptor.New(
		adaptor.WithDBClient(db),
		adaptor.WithBundle(bdl),
		adaptor.WithUCClient(uc),
	)

	// compose endpoints
	ep := endpoints.New(
		endpoints.WithBundle(bdl),
		endpoints.WithQueryStringDecoder(queryStringDecoder),
		endpoints.WithUcAdaptor(ucAdaptor),
	)

	return ep, nil
}
