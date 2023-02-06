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
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	mutex "github.com/erda-project/erda-infra/providers/etcd-mutex"
	kafkaInf "github.com/erda-project/erda-infra/providers/kafka"
)

type (
	config struct {
		Force             bool          `file:"force" default:"true"`
		RequestTimeout    time.Duration `file:"request_timeout" default:"2m"`
		Topics            []string      `file:"topics"`
		NumPartitions     int           `file:"num_partitions" default:"15"`
		ReplicationFactor int           `file:"replication_factor" default:"1"`
	}
	provider struct {
		Cfg      *config
		Log      logs.Logger
		KafkaInf kafkaInf.Interface `autowired:"kafka"`
		Lock     mutex.Mutex        `mutex-key:"kafka-topic-initializer"`
	}
)

func (p *provider) Init(ctx servicehub.Context) error {
	if err := p.Lock.Lock(ctx); err != nil {
		return fmt.Errorf("lock failed: %w", err)
	}
	defer func() {
		err := p.Lock.Unlock(ctx)
		if err != nil {
			p.Log.Error(err)
		}
	}()

	cli, err := p.KafkaInf.NewAdminClient()
	if err != nil {
		return err
	}
	ctxTimeout, cancel := context.WithTimeout(ctx, p.Cfg.RequestTimeout)
	defer cancel()

	metadata, err := cli.GetMetadata(nil, true, int(p.Cfg.RequestTimeout.Milliseconds()))
	if err != nil {
		return fmt.Errorf("GetMetadata: %w", err)
	}

	topicList := getTopicList(p.Cfg.Topics, metadata, p.Cfg.NumPartitions, p.Cfg.ReplicationFactor)
	if len(topicList) == 0 {
		return nil
	}
	p.Log.Infof("topics: %+v need to be created", topicList)
	// 1. validate
	_, err = cli.CreateTopics(ctxTimeout, topicList, kafka.SetAdminValidateOnly(true))
	if err != nil {
		return fmt.Errorf("validate CreateTopics: %w", err)
	}
	// 2. create
	rs, err := cli.CreateTopics(ctxTimeout, topicList, kafka.SetAdminValidateOnly(false))
	if err != nil {
		return fmt.Errorf("do CreateTopics: %w", err)
	}
	// 3. check
	for idx, item := range rs {
		if item.Error.Code() != kafka.ErrNoError {
			expr := fmt.Sprintf("create topic %s failed. retry with index %d, err: %s", item.Topic, idx, item.Error.Error())
			if p.Cfg.Force {
				return errors.New(expr)
			} else {
				p.Log.Error(expr)
			}
		}
	}
	return nil
}

func getTopicList(topics []string, metadata *kafka.Metadata, numPartitions, replicationFactor int) []kafka.TopicSpecification {
	existedTopic := make(map[string]struct{})
	for _, item := range metadata.Topics {
		if item.Error.Code() == kafka.ErrNoError {
			existedTopic[item.Topic] = struct{}{}
		}
	}

	topicList := make([]kafka.TopicSpecification, 0)
	for _, name := range topics {
		if _, ok := existedTopic[name]; !ok {
			topicList = append(topicList, kafka.TopicSpecification{
				Topic:             name,
				NumPartitions:     numPartitions,
				ReplicationFactor: replicationFactor,
			})
		}
	}
	return topicList
}

func init() {
	servicehub.Register("kafka.topic.initializer", &servicehub.Spec{
		Services:     []string{"kafka.topic.initializer"},
		Dependencies: []string{"kafka"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
