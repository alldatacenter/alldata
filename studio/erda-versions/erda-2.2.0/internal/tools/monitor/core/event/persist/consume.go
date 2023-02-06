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
	"time"

	"github.com/erda-project/erda-proto-go/oap/event/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/event"
)

func (p *provider) decode(key, value []byte, topic *string, timestamp time.Time) (interface{}, error) {
	input := &pb.Event{}
	if err := json.Unmarshal(value, input); err != nil {
		p.stats.DecodeError(value, err)
		if p.Cfg.PrintInvalidEvent {
			p.Log.Warnf("unknown format event data: %s", string(value))
		} else {
			p.Log.Warnf("failed to decode event: %v", err)
		}
		return nil, err
	}
	data := p.normalize(input)
	if err := p.validator.Validate(data); err != nil {
		p.stats.ValidateError(data)
		if p.Cfg.PrintInvalidEvent {
			p.Log.Warnf("invalid event data: %s, %s", string(value), err)
		} else {
			p.Log.Warnf("invalid event: %v", err)
		}
		return nil, err
	}
	if err := p.metadata.Process(data); err != nil {
		p.stats.MetadataError(data, err)
		p.Log.Errorf("failed to process event metadata: %v", err)
	}
	return data, nil
}

func (p *provider) handleReadError(err error) error {
	p.Log.Errorf("failed to read events from kafka: %s", err)
	return nil // return nil to continue read
}

func (p *provider) handleWriteError(list []interface{}, err error) error {
	p.Log.Errorf("failed to write events into storage: %s", err)
	return nil // return nil to continue consume
}

func (p *provider) confirmErrorHandler(err error) error {
	p.Log.Errorf("failed to confirm events from kafka: %s", err)
	return err // return error to exit
}

func (p *provider) normalize(input *pb.Event) *event.Event {
	return &event.Event{
		EventID:   input.EventID,
		Name:      input.Name,
		Kind:      input.Kind.String(),
		Content:   input.Message,
		Timestamp: int64(input.TimeUnixNano),
		Tags:      input.Attributes,
		Relations: &event.Relation{
			ResID:   input.Relations.ResID,
			ResType: input.Relations.ResType,
			TraceID: input.Relations.TraceID,
		},
	}
}
