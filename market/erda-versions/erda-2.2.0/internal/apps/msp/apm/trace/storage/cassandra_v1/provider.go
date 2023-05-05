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

package cassandra_v1

import (
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/cassandra"
	"github.com/erda-project/erda-infra/providers/kafka"
)

type config struct {
	SpotInput kafka.ConsumerConfig `file:"spot_input"`
	OapInput  kafka.ConsumerConfig `file:"oap_input"`
	Output    struct {
		Cassandra struct {
			cassandra.WriterConfig  `file:"writer_config"`
			cassandra.SessionConfig `file:"session_config"`
			GCGraceSeconds          int           `file:"gc_grace_seconds" default:"86400"`
			TTL                     time.Duration `file:"ttl" default:"168h"`
		} `file:"cassandra"`
		//Kafka kafka.ProducerConfig `file:"kafka"`
	} `file:"output"`
}

type provider struct {
	Cfg    *config
	Log    logs.Logger
	ttlSec int
	kafka  kafka.Interface
	output struct {
		cassandra writer.Writer
		kafka     writer.Writer
	}
	cassandraSession *cassandra.Session
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.ttlSec = int(p.Cfg.Output.Cassandra.TTL.Seconds())
	cassandra := ctx.Service("cassandra").(cassandra.Interface)
	session, err := cassandra.NewSession(&p.Cfg.Output.Cassandra.SessionConfig)
	p.cassandraSession = session
	if err != nil {
		return fmt.Errorf("fail to create cassandra session: %s", err)
	}
	err = p.initCassandra(session)
	if err != nil {
		return err
	}
	p.output.cassandra = cassandra.NewBatchWriter(session, &p.Cfg.Output.Cassandra.WriterConfig, p.createTraceStatement)

	p.kafka = ctx.Service("kafka").(kafka.Interface)
	//w, err := p.kafka.NewProducer(&p.Cfg.Output.Kafka)
	//if err != nil {
	//	return fmt.Errorf("fail to create kafka producer: %s", err)
	//}
	//p.output.kafka = w
	return nil
}

// Start .
func (p *provider) Start() error {
	if err := p.kafka.NewConsumer(&p.Cfg.SpotInput, p.spotSpanConsumer); err != nil {
		return err
	}
	return p.kafka.NewConsumer(&p.Cfg.OapInput, p.oapSpanConsumer)
}

func (p *provider) Close() error {
	p.Log.Debug("not support close kafka consumer")
	return nil
}

func init() {
	servicehub.Register("trace-storage", &servicehub.Spec{
		Services:     []string{"trace-storage"},
		Dependencies: []string{"kafka", "cassandra", "kafka.topic.initializer"},
		Description:  "trace storage",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
