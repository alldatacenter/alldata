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

package initializer

import (
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
)

type (
	template struct {
		Name string `file:"name"`
		Path string `file:"path"`
	}
	createIndex struct {
		Index string `file:"index"`
	}
	config struct {
		RequestTimeout time.Duration `file:"request_timeout" default:"2m"`
		Templates      []template    `file:"templates"`
		Creates        []createIndex `file:"creates"`
	}
	provider struct {
		Cfg *config
		Log logs.Logger
		es  elasticsearch.Interface `autowired:"elasticsearch"`
	}
)

func (p *provider) Init(ctx servicehub.Context) error {
	if es, err := index.FindElasticSearch(ctx, true); err != nil {
		return err
	} else {
		p.es = es
	}

	err := p.initTemplates(ctx, p.es.Client(), p.Cfg.Templates)
	if err != nil {
		return err
	}
	return p.createIndices(ctx, p.es.Client(), p.Cfg.Creates)
}

func init() {
	servicehub.Register("elasticsearch.index.initializer", &servicehub.Spec{
		Services:     []string{"elasticsearch.index.initializer"},
		Dependencies: []string{"elasticsearch"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
