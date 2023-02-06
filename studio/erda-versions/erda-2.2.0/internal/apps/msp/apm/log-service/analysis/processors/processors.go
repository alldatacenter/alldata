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

package processors

import (
	"fmt"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
)

// Processor .
type Processor interface {
	Process(content string) (string, map[string]interface{}, map[string]string, map[string]string, error)
	Keys() []*pb.FieldDefine
}

type processor struct {
	Processor
	tags map[string]string
}

var processors = make(map[string]func(metric string, cfg []byte) (Processor, error))

// RegisterProcessor .
func RegisterProcessor(typ string, creator func(metric string, cfg []byte) (Processor, error)) {
	processors[typ] = creator
}

// NewProcessor .
func NewProcessor(name, typ string, cfg []byte) (Processor, error) {
	creator, ok := processors[typ]
	if !ok {
		return nil, fmt.Errorf("processor %s not exist", typ)
	}
	return creator(name, cfg)
}

// Processors .
type Processors struct {
	ps map[string][]*processor
}

// New .
func New() *Processors {
	return &Processors{
		ps: make(map[string][]*processor),
	}
}

// Add .
func (ps *Processors) Add(key string, tags map[string]string, name, typ string, config []byte) error {
	creator, ok := processors[typ]
	if !ok {
		return fmt.Errorf("processor %s not exist", typ)
	}
	p, err := creator(name, config)
	if err != nil {
		return err
	}
	ps.ps[key] = append(ps.ps[key], &processor{
		tags:      tags,
		Processor: p,
	})
	return nil
}

// Find .
func (ps *Processors) Find(name, key string, tags map[string]string) []Processor {
	procs := ps.ps[key]
	var list []Processor
loop:
	for _, p := range procs {
		if len(p.tags) > 0 {
			for k, v := range p.tags {
				if tags[k] != v {
					continue loop
				}
			}
		}
		list = append(list, p)
	}
	return list
}
