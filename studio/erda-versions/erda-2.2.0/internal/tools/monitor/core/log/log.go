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

package log

import (
	"time"
)

// Log .
type Log struct {
	UniqId    string            `json:"-" ch:"_id"`
	OrgName   string            `json:"-" ch:"org_name"`
	TenantId  string            `json:"-" ch:"tenant_id"`
	GroupId   string            `json:"-" ch:"group_id"`
	Source    string            `json:"source" ch:"source"`
	ID        string            `json:"id" ch:"id"`
	Stream    string            `json:"stream" ch:"stream"`
	Content   string            `json:"content" ch:"content"`
	Offset    int64             `json:"offset" ch:"offset"`
	Time      *time.Time        `json:"time,omitempty"` // the time key in fluent-bit is RFC3339Nano
	Timestamp int64             `json:"timestamp" ch:"timestamp"`
	Tags      map[string]string `json:"tags" ch:"tags"`
}

func (l *Log) Hash() uint64 {
	return 0
}

func (l *Log) GetTags() map[string]string {
	if l.Tags == nil {
		l.Tags = map[string]string{}
	}
	return l.Tags
}

// LabeledLog .
type LabeledLog struct {
	Log
	Labels map[string]string `json:"labels,omitempty"`
}

// Meta Log Meta
type Meta struct {
	Source string            `json:"source"`
	ID     string            `json:"id"`
	Tags   map[string]string `json:"tags"`
}
