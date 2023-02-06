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

package hepa

import (
	"net/http"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/base/version"
	_ "github.com/erda-project/erda-infra/providers/health"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	hepaI18n "github.com/erda-project/erda/internal/tools/orchestrator/hepa/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/pkg/discover"
)

type myCfg struct {
	Log    config.LogConfig
	Server config.ServerConfig
}

type provider struct {
	Cfg        *myCfg            // auto inject this field
	Log        logs.Logger       // auto inject this field
	HttpServer httpserver.Router `autowired:"http-server"`
	LogTrans   i18n.Translator   `translator:"log-trans"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	config.ServerConf = &p.Cfg.Server
	config.LogConf = &p.Cfg.Log
	common.InitLogger()
	orm.Init()
	logrus.Info(version.String())
	logrus.Infof("server conf: %+v", config.ServerConf)
	logrus.Infof("log conf: %+v", config.LogConf)
	p.HttpServer.GET("/api/gateway/openapi/metrics/*", func(resp http.ResponseWriter, req *http.Request) {
		path := strings.Replace(req.URL.Path, "/api/gateway/openapi/metrics/charts", "/api/metrics", 1)
		path += "?" + req.URL.RawQuery
		logrus.Infof("monitor proxy url:%s", path)
		headers := make(map[string]string)
		for key, values := range req.Header {
			headers[key] = values[0]
		}
		code, body, err := util.CommonRequest("GET", discover.Monitor()+path, nil, headers)
		if err != nil {
			logrus.Error(err)
			code = http.StatusInternalServerError
			body = []byte("")
		}
		resp.Header().Set("Content-Type", "application/json; charset=utf-8")
		resp.WriteHeader(code)
		resp.Write(body)
	}, permission.Intercepter(
		permission.ScopeOrg, permission.OrgIDFromHeader(),
		"org", permission.ActionGet,
	))
	hepaI18n.SetSingle(ctx.Service("i18n").(i18n.I18n).Translator("log-trans"))
	return nil
}

func init() {
	servicehub.Register("hepa", &servicehub.Spec{
		Services:    []string{"hepa"},
		Description: "hepa",
		ConfigFunc:  func() interface{} { return &myCfg{} },
		Creator:     func() servicehub.Provider { return &provider{} },
	})
}
