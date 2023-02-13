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

package sls

import (
	"context"
	"fmt"
	"time"

	sls "github.com/aliyun/aliyun-log-go-sdk"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/pkg/metrics/report"
)

type (
	// Account .
	Account struct {
		Group           string            `file:"comsume_group"`
		AccessKey       string            `file:"ali_access_key"`
		AccessSecretKey string            `file:"ali_access_secret_key"`
		Endpoints       []string          `file:"endpoints"`
		Tags            map[string]string `file:"tags"`
	}
	// LogStore .
	LogStore struct {
		Project string `file:"project"`
		Store   string `file:"store"`
		Type    string `file:"type"`
	}
	// ConsumeFunc .
	ConsumeFunc func(shardID int, groups *sls.LogGroupList) string
	// Processor .
	Processor interface {
		MatchProject(project string) bool
		MatchStore(project, store string) *LogStore
		GetHandler(project, store, typ string, account *Account) ConsumeFunc
	}
)

// Endpoints sls endpoints
var Endpoints = []string{
	"cn-hangzhou.log.aliyuncs.com",           // 华东1（杭州）
	"cn-hangzhou-finance.log.aliyuncs.com",   // 华东1（杭州-金融云）
	"cn-shanghai.log.aliyuncs.com",           // 华东2（上海）
	"cn-shanghai-finance-1.log.aliyuncs.com", // 华东2（上海-金融云）
	"cn-qingdao.log.aliyuncs.com",            // 华北1（青岛）
	"cn-beijing.log.aliyuncs.com",            // 华北2（北京）
	"cn-zhangjiakou.log.aliyuncs.com",        // 华北3（张家口）
	"cn-huhehaote.log.aliyuncs.com",          // 华北5（呼和浩特）
	"cn-shenzhen.log.aliyuncs.com",           // 华南1（深圳）
	"cn-shenzhen-finance.log.aliyuncs.com",   // 华南1（深圳-金融云）
	"cn-chengdu.log.aliyuncs.com",            // 西南1（成都）
	"cn-hongkong.log.aliyuncs.com",           // 中国（香港）
	"ap-northeast-1.log.aliyuncs.com",        // 日本（东京）
	"ap-southeast-1.log.aliyuncs.com",        // 新加坡
	"ap-southeast-2.log.aliyuncs.com",        // 澳大利亚（悉尼）
	"ap-southeast-3.log.aliyuncs.com",        // 马来西亚（吉隆坡）
	"ap-southeast-5.log.aliyuncs.com",        // 印度尼西亚（雅加达）
	"me-east-1.log.aliyuncs.com",             // 阿联酋（迪拜）
	"us-west-1.log.aliyuncs.com",             // 美国（硅谷）
	"eu-central-1.log.aliyuncs.com",          // 德国（法兰克福）
	"us-east-1.log.aliyuncs.com",             // 美国（弗吉尼亚）
	"ap-south-1.log.aliyuncs.com",            // 印度（孟买）
	"eu-west-1.log.aliyuncs.com",             // 英国（伦敦）
}

type (
	config struct {
		ProjectsReloadInterval time.Duration `file:"projects_reload_interval" default:"1m"`
		Account                Account       `file:"account"`
		LogStores              []LogStore    `file:"log_stores"`
	}
	provider struct {
		Cfg      *config
		Log      logs.Logger
		Reporter report.MetricReport `autowired:"metric-report-client"`

		manager  *ConsumerManager
		projects map[string]map[string]*LogStore
	}
)

func (p *provider) Init(ctx servicehub.Context) error {
	p.projects = make(map[string]map[string]*LogStore)
	for _, item := range p.Cfg.LogStores {
		if len(item.Project) <= 0 || len(item.Store) <= 0 || len(item.Type) <= 0 {
			return fmt.Errorf("project、store、type is required")
		}
		logStores := p.projects[item.Store]
		if logStores == nil {
			logStores = make(map[string]*LogStore)
			p.projects[item.Store] = logStores
		}
		logStore := item
		logStores[item.Store] = &logStore
	}
	if len(p.Cfg.Account.AccessKey) <= 0 || len(p.Cfg.Account.AccessSecretKey) <= 0 {
		return fmt.Errorf("ali_access_key and ali_access_secret_key is required")
	}
	if len(p.Cfg.Account.Endpoints) <= 0 {
		p.Cfg.Account.Endpoints = Endpoints
	}
	p.manager = newConsumerManager(p.Log, &p.Cfg.Account, p.Cfg.ProjectsReloadInterval, p)
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	return p.manager.Start(ctx)
}

func (p *provider) MatchProject(project string) bool {
	return len(p.projects[project]) > 0
}

func (p *provider) MatchStore(project, store string) *LogStore {
	return p.projects[project][store]
}

func init() {
	servicehub.Register("sls-log-to-metric", &servicehub.Spec{
		Services: []string{"sls log to metric"},
		// Dependencies: []string{"kafka", "elasticsearch"},
		Description: "import logs from aliyun sls",
		ConfigFunc:  func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
