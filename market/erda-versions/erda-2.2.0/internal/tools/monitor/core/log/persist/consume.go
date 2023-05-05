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

package persist

import (
	"encoding/json"
	"strings"
	"time"

	"github.com/erda-project/erda/internal/tools/monitor/core/log"
)

func (p *provider) decodeLog(key, value []byte, topic *string, timestamp time.Time) (interface{}, error) {
	data := &log.LabeledLog{}
	if err := json.Unmarshal(value, data); err != nil {
		p.stats.DecodeError(value, err)
		if p.Cfg.PrintInvalidLog {
			p.Log.Warnf("unknown format log data: %s", string(value))
		} else {
			p.Log.Warnf("failed to decode log: %v", err)
		}
		return nil, err
	}
	p.normalize(&data.Log)
	if err := p.validator.Validate(data); err != nil {
		p.stats.ValidateError(data)
		if p.Cfg.PrintInvalidLog {
			p.Log.Warnf("invalid log data: %s, %s", string(value), err)
		} else {
			p.Log.Warnf("invalid log: %v", err)
		}
		return nil, err
	}
	if err := p.metadata.Process(data); err != nil {
		p.stats.MetadataError(data, err)
		p.Log.Errorf("failed to process log metadata: %v", err)
	}
	return data, nil
}

func (p *provider) handleReadError(err error) error {
	p.Log.Errorf("failed to read logs from kafka: %s", err)
	return nil // return nil to continue read
}

func (p *provider) handleWriteError(list []interface{}, err error) error {
	p.Log.Errorf("failed to write into storage: %s", err)
	return nil // return nil to continue consume
}

func (p *provider) confirmErrorHandler(err error) error {
	p.Log.Errorf("failed to confirm logs from kafka: %s", err)
	return err // return error to exit
}

func (p *provider) normalize(data *log.Log) {
	if data.Tags == nil {
		data.Tags = make(map[string]string)
	}

	if data.Time != nil {
		data.Timestamp = data.Time.UnixNano()
		data.Time = nil
	}

	// setup level
	if level, ok := data.Tags["level"]; ok {
		data.Tags["level"] = strings.ToUpper(level)
	} else {
		data.Tags["level"] = "INFO"
	}

	// setup request id
	if reqID, ok := data.Tags["request_id"]; ok {
		data.Tags["trace_id"] = reqID
	} else if reqID, ok = data.Tags["trace_id"]; ok {
		data.Tags["request_id"] = reqID
	} else if reqID, ok = data.Tags["request-id"]; ok {
		data.Tags["request_id"] = reqID
		data.Tags["trace_id"] = reqID
		delete(data.Tags, "request-id")
	}

	// setup org name
	if _, ok := data.Tags["dice_org_name"]; !ok {
		if org, ok := data.Tags["org_name"]; ok {
			data.Tags["dice_org_name"] = org
		}
	}

	// setup log key for compatibility
	key, ok := data.Tags["monitor_log_key"]
	if !ok {
		key, ok = data.Tags["terminus_log_key"]
		if ok {
			data.Tags["monitor_log_key"] = key
		}
	}

	// setup log id
	for _, key := range p.Cfg.IDKeys {
		if val, ok := data.Tags[key]; ok {
			data.ID = val
			break
		}
	}

	// setup default stream
	if data.Stream == "" {
		data.Stream = "stdout"
	}
}
