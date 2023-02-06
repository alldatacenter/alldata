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
	"fmt"

	"github.com/go-errors/errors"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
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

func (p *provider) getTopic(typ string) (string, error) {
	if topic, ok := topics[typ]; ok {
		return topic, nil
	}
	return "", errors.Errorf("not support type")
}

func (p *provider) sendRaw(name string, value []byte) error {
	od := odata.NewRaw(value)

	if p.Cfg.MetadataKeyOfTopic != "" {
		topic, err := p.getTopic(name)
		if err != nil {
			return fmt.Errorf("getTopic with name: %s, err: %w", name, err)
		}
		od.Meta[p.Cfg.MetadataKeyOfTopic] = topic
	}

	p.consumer(od)
	return nil
}
