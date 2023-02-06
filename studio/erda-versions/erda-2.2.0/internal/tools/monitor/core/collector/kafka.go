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

package collector

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/providers/kafka"
)

var (
	topics = map[string]string{
		"metrics":       topicPrefix + "metrics",
		"trace":         topicPrefix + "trace",
		"container_log": topicPrefix + "container-log",
		"job_log":       topicPrefix + "job-log",
		"analytics":     topicPrefix + "analytics",
		// white list
		"alert":                topicPrefix + "alert",
		"alert-event":          topicPrefix + "alert-event",
		"error":                topicPrefix + "error",
		"metaserver_container": topicPrefix + "metaserver_container",
		"metaserver_host":      topicPrefix + "metaserver_host",
		"metrics-temp":         topicPrefix + "metrics-temp",
		"monitor-log":          topicPrefix + "monitor-log",
	}
)

const (
	topicPrefix = "spot-"
)

func (p *provider) send(name string, data []byte) error {
	topic, err := p.getTopic(name)
	if err != nil {
		return err
	}
	return p.writer.Write(&kafka.Message{
		Topic: &topic,
		Data:  data,
	})
}

func (p *provider) getTopic(typ string) (string, error) {
	if topic, ok := topics[typ]; ok {
		return topic, nil
	}
	return "", errors.Errorf("not support type")
}
