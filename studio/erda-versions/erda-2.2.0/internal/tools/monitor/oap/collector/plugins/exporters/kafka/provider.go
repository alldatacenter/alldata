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

package kafka

import (
	"encoding/json"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins"
)

var providerName = plugins.WithPrefixExporter("kafka")

type config struct {
	MetadataKeyOfTopic string               `file:"metadata_key_of_topic"`
	Producer           kafka.ProducerConfig `file:"producer"`
	// capability of old data format
	Compatibility bool `file:"compatibility" default:"true"`
}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger

	Kafka  kafka.Interface `autowired:"kafka"`
	writer writer.Writer
}

func (p *provider) ExportMetric(items ...*metric.Metric) error {
	for _, item := range items {
		data, err := json.Marshal(item) // TODO. Parallelism
		if err != nil {
			p.Log.Errorf("serialize err: %s", err)
			continue
		}
		err = p.writer.Write(&kafka.Message{
			Data: data,
		})
		if err != nil {
			p.Log.Errorf("write data to %s err: %s", p.Cfg.Producer.Topic, err)
			continue
		}
	}
	return nil
}

func (p *provider) ExportLog(items ...*log.Log) error     { return nil }
func (p *provider) ExportSpan(items ...*trace.Span) error { return nil }
func (p *provider) ExportRaw(items ...*odata.Raw) error {
	for _, item := range items {
		if p.Cfg.MetadataKeyOfTopic != "" {
			tmp, ok := item.Meta[p.Cfg.MetadataKeyOfTopic]
			if !ok {
				p.Log.Errorf("unable to find topic with key %s", p.Cfg.MetadataKeyOfTopic)
				continue
			}

			if err := p.writer.Write(&kafka.Message{
				Topic: &tmp,
				Data:  item.Data,
			}); err != nil {
				p.Log.Errorf("write data to %s err: %s", tmp, err)
			}
		} else {
			if err := p.writer.Write(item.Data); err != nil {
				p.Log.Errorf("write data to %s err: %s", p.Cfg.Producer.Topic, err)
			}
		}
	}
	return nil
}

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func (p *provider) Connect() error {
	return nil
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	producer, err := p.Kafka.NewProducer(&p.Cfg.Producer)
	if err != nil {
		return err
	}
	p.writer = producer

	return nil
}

func init() {
	servicehub.Register(providerName, &servicehub.Spec{
		Services: []string{
			providerName,
		},
		Description: "here is description of erda.oap.collector.exporter.kafka",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
