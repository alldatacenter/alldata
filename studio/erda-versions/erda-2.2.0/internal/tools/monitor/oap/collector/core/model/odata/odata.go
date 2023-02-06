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

package odata

import (
	"hash/fnv"
	"strings"

	"github.com/erda-project/erda-proto-go/oap/common/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

type DataType string

const (
	MetricType DataType = "METRIC"
	SpanType   DataType = "SPAN"
	LogType    DataType = "LOG"
	RawType    DataType = "RAW"
)

const (
	TagsPrefix   = "tags."
	FieldsPrefix = "fields."
)

// The slice representation of Attributes
type Tag struct {
	Key   string
	Value string
}

func GetKeyValue(data ObservableData, key string) (interface{}, bool) {
	if strings.HasPrefix(key, TagsPrefix) {
		val, ok := data.GetTags()[trimTags(key)]
		return val, ok
	}

	switch item := data.(type) {
	case *metric.Metric:
		if strings.HasPrefix(key, FieldsPrefix) {
			val, ok := item.Fields[trimFields(key)]
			return val, ok
		} else if key == "name" {
			return item.Name, true
		}
	case *log.Log:
	case *trace.Span:
	}
	return "", false
}

func SetKeyValue(data ObservableData, key string, value interface{}) {
	if strings.HasPrefix(key, TagsPrefix) {
		data.GetTags()[trimTags(key)] = value.(string)
	}

	switch item := data.(type) {
	case *metric.Metric:
		if strings.HasPrefix(key, FieldsPrefix) {
			item.Fields[trimFields(key)] = value
		} else if key == "name" {
			item.Name = value.(string)
		}
	case *log.Log:
	case *trace.Span:
	}
}

func DeleteKeyValue(data ObservableData, key string) {
	if strings.HasPrefix(key, TagsPrefix) {
		delete(data.GetTags(), trimTags(key))
	}

	switch item := data.(type) {
	case *metric.Metric:
		if strings.HasPrefix(key, FieldsPrefix) {
			delete(item.Fields, trimFields(key))
		}
	case *log.Log:
	case *trace.Span:
	}
}

func trimTags(key string) string {
	return strings.TrimPrefix(key, TagsPrefix)
}

func trimFields(key string) string {
	return strings.TrimPrefix(key, FieldsPrefix)
}

var (
	_ ObservableData = &metric.Metric{}
	_ ObservableData = &log.Log{}
	_ ObservableData = &trace.Span{}
	_ ObservableData = &Raw{}
)

type ObservableData interface {
	GetTags() map[string]string
	Hash() uint64
}

type SourceItem interface {
	GetName() string
	GetAttributes() map[string]string
	GetRelations() *pb.Relation
}

func HashTagList(tagsList []Tag) uint64 {
	h := fnv.New64a()
	for _, item := range tagsList {
		h.Write([]byte(item.Key))
		h.Write([]byte("\n"))
		h.Write([]byte(item.Value))
		h.Write([]byte("\n"))
	}
	return h.Sum64()
}
