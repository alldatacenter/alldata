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

// Author: recallsong
// Email: ruiguo.srg@alibaba-inc.com

package elasticsearch

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/exporter"
)

type config struct {
	WriterConfig elasticsearch.WriterConfig `file:"writer_config"`
	Index        struct {
		Prefix string `file:"prefix"`
		Index  string `file:"index"`
	} `file:"index"`
	Disabled bool `file:"disabled"`
}

type provider struct {
	C      *config
	L      logs.Logger
	exp    exporter.Interface
	es     elasticsearch.Interface
	output writer.Writer
}

func (p *provider) Init(ctx servicehub.Context) error {
	if p.C.Disabled {
		p.L.Warnf("provider disabled")
		return nil
	}
	p.exp = ctx.Service("logs-exporter-base").(exporter.Interface)
	p.es = ctx.Service("elasticsearch").(elasticsearch.Interface)
	p.output = p.es.NewBatchWriter(&p.C.WriterConfig)
	return nil
}

func (p *provider) Start() error {
	if p.exp == nil {
		return nil
	}
	return p.exp.NewConsumer(p.newOutput)
}

func (p *provider) Close() error { return nil }

func (p *provider) newOutput(i int) (exporter.Output, error) {
	return &esOutput{
		prefix: p.C.Index.Prefix,
		index:  p.C.Index.Index,
		writer: p.output,
	}, nil
}

type esOutput struct {
	prefix string
	index  string
	writer writer.Writer
}

func (o *esOutput) Write(key string, data []byte) error {
	index := o.index
	if len(index) <= 0 {
		index = o.prefix + key
	}
	return o.writer.Write(&elasticsearch.Document{
		Index: index,
		Data:  string(data),
	})
}

func init() {
	servicehub.Register("logs-exporter-elasticsearch", &servicehub.Spec{
		Services:     []string{"logs-exporter-elasticsearch"},
		Dependencies: []string{"logs-exporter-base", "elasticsearch@logs"},
		Description:  "logs export to elasticsearch",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
