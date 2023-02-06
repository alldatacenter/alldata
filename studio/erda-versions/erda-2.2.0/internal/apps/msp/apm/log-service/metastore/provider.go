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

package metastore

import (
	"encoding/json"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
)

type config struct {
	InstanceID       string `file:"instance_id" env:"LOG_SERVICE_INSTANCE_ID"`
	EsUrl            string `file:"es_url" env:"LOGS_ES_URL"`
	EsSecurityEnable bool   `file:"es_security_enable" env:"LOGS_ES_SECURITY_ENABLE"`
	EsUsername       string `file:"es_username" env:"LOGS_ES_SECURITY_USERNAME"`
	EsPassword       string `file:"es_password" env:"LOGS_ES_SECURITY_PASSWORD"`
}

type provider struct {
	Cfg                  *config
	Log                  logs.Logger
	DB                   *gorm.DB `autowired:"mysql-client"`
	LogServiceInstanceDB *db.LogServiceInstanceDB
}

func (p *provider) Init(ctx servicehub.Context) error {

	p.LogServiceInstanceDB = &db.LogServiceInstanceDB{DB: p.DB}

	var esConfig = struct {
		Security bool   `json:"securityEnable"`
		Username string `json:"securityUsername"`
		Password string `json:"securityPassword"`
	}{
		Security: p.Cfg.EsSecurityEnable,
		Username: p.Cfg.EsUsername,
		Password: p.Cfg.EsPassword,
	}

	conf, _ := json.Marshal(&esConfig)
	err := p.LogServiceInstanceDB.AddOrUpdateEsUrls(p.Cfg.InstanceID, p.Cfg.EsUrl, string(conf))
	if err != nil {
		p.Log.Errorf("fail store log service instanceId-esUrl map: %s", err.Error())
	}

	return err
}

func init() {
	servicehub.Register("erda.msp.apm.log-service.metastore", &servicehub.Spec{
		Services:     []string{"erda.msp.apm.log-service.metastore"},
		Description:  "erda.msp.apm.log-service.metastore",
		Dependencies: []string{"mysql"},
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
