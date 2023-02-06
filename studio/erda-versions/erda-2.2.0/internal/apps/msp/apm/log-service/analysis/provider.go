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

package analysis

import (
	"fmt"
	"sync/atomic"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda-infra/providers/mysql"
	"github.com/erda-project/erda/internal/apps/msp/apm/log-service/rules/db"
)

type config struct {
	Filters    map[string]string `file:"filters"`
	Processors struct {
		Scope          string        `file:"scope"`
		ScopeID        string        `file:"scope_id"`
		ScopeIDKey     string        `file:"scope_id_key"`
		ReloadInterval time.Duration `file:"reload_interval" default:"3m"`
	} `file:"processors"`
	Input  kafka.ConsumerConfig `file:"input"`
	Output struct {
		Type      string               `file:"type"`
		Kafka     kafka.ProducerConfig `file:"kafka"`
		Collector struct {
			URL      string `file:"url"`
			UserName string `file:"username"`
			Password string `file:"password"`
		} `file:"collector"`
	} `file:"output"`
}

type provider struct {
	C          *config
	L          logs.Logger
	mysql      *gorm.DB
	kafka      kafka.Interface
	output     writer.Writer
	processors atomic.Value
	db         *db.DB
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.mysql = ctx.Service("mysql").(mysql.Interface).DB()
	p.db = db.New(p.mysql)
	p.kafka = ctx.Service("kafka").(kafka.Interface)
	w, err := p.kafka.NewProducer(&p.C.Output.Kafka)
	if err != nil {
		return fmt.Errorf("fail to create kafka producer: %s", err)
	}
	p.output = w
	return nil
}

// Start .
func (p *provider) Start() error {
	err := p.kafka.NewConsumer(&p.C.Input, p.invoke)
	if err != nil {
		return err
	}
	go func() {
		for {
			err := p.loadProcessors()
			if err != nil {
				p.L.Errorf("fail to load processors: %s", err)
			}
			time.Sleep(p.C.Processors.ReloadInterval)
		}
	}()
	return nil
}

func (p *provider) Close() error { return nil }

func init() {
	servicehub.Register("logs-metrics-analysis", &servicehub.Spec{
		Services:     []string{"logs-metrics-analysis"},
		Dependencies: []string{"kafka", "mysql"},
		Description:  "parse logs to metrics",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
