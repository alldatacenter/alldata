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

package exporter

import (
	"encoding/json"
	"time"

	"github.com/recallsong/go-utils/encoding"
	"github.com/recallsong/go-utils/reflectx"

	"github.com/erda-project/erda-infra/base/logs"
)

// consumer .
type consumer struct {
	log     logs.Logger
	filters map[string]string
	output  Output
}

type content struct {
	// encoding.RawBytes 不去解析具体内容
	ID        encoding.RawBytes            `json:"id"`
	Timestamp int64                        `json:"timestamp,omitempty"`
	Time      *time.Time                   `json:"time,omitempty"`
	Source    encoding.RawBytes            `json:"source"`
	Content   encoding.RawBytes            `json:"content"`
	Offset    encoding.RawBytes            `json:"offset"`
	Stream    encoding.RawBytes            `json:"stream"`
	Tags      map[string]encoding.RawBytes `json:"tags"`
}
type labelscontent struct {
	content
	Labels map[string]string `json:"labels,omitempy"`
}

// Invoke .
func (c *consumer) Invoke(key []byte, value []byte, topic *string, timestamp time.Time) error {
	var data labelscontent
	err := json.Unmarshal(value, &data)
	if err != nil || data.Tags == nil {
		c.log.Warnf("invalid log message: %s", err)
		return nil
	}

	// 兼容
	key, ok := data.Tags["monitor_log_key"]
	if !ok {
		key, ok = data.Tags["terminus_log_key"]
		if ok {
			data.Tags["monitor_log_key"] = key
		}
	}
	if !ok {
		key, ok = data.Tags["msp_env_id"]
	}

	if len(key) <= 2 {
		return nil
	}
	if data.Timestamp == 0 && data.Time != nil {
		data.Timestamp = data.Time.UnixNano()
	}
	// do filter
	// allow no filter
	// todo: support filter by es index existence
	if c.filters != nil {
		for k, v := range c.filters {
			val, ok := data.Tags[k]
			if !ok {
				return nil
			}
			if len(v) > 0 && v != reflectx.BytesToString(val[1:len(val)-1]) {
				return nil
			}
		}
	}

	if reqId, ok := data.Tags["trace_id"]; ok {
		data.Tags["request-id"] = reqId
	} else if reqId, ok = data.Tags["request-id"]; ok {
		data.Tags["trace_id"] = data.Tags["request-id"]
	}

	delete(data.Labels, "monitor_log_output")
	delete(data.Labels, "monitor_log_output_config")
	value, err = json.Marshal(&data)
	if err != nil {
		return err
	}
	return c.output.Write(reflectx.BytesToString(key[1:len(key)-1]), value)
}
