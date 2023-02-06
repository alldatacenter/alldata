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

package metric

import (
	"sort"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
)

// TagsKeys .
func TagsKeys(m *pb.MetricMeta) []string {
	var keys []string
	for k := range m.Tags {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	return keys
}

// FieldsKeys .
func FieldsKeys(m *pb.MetricMeta) []string {
	var keys []string
	for k := range m.Fields {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	return keys
}

// NewMeta .
func NewMeta() *pb.MetricMeta {
	return &pb.MetricMeta{
		Name:   &pb.NameDefine{},
		Tags:   make(map[string]*pb.TagDefine),
		Fields: make(map[string]*pb.FieldDefine),
	}
}

// CopyNameDefine .
func CopyNameDefine(name *pb.NameDefine) *pb.NameDefine {
	return &pb.NameDefine{
		Key:  name.Key,
		Name: name.Name,
	}
}

// CopyTagDefine .
func CopyTagDefine(tag *pb.TagDefine) *pb.TagDefine {
	clone := &pb.TagDefine{
		Key:  tag.Key,
		Name: tag.Name,
	}
	var values []*pb.ValueDefine
	for _, v := range tag.Values {
		values = append(values, CopyValue(v))
	}
	clone.Values = values
	return clone
}

// CopyTagDefine .
func CopyFieldDefine(field *pb.FieldDefine) *pb.FieldDefine {
	clone := &pb.FieldDefine{
		Key:  field.Key,
		Name: field.Name,
		Type: field.Type,
		Unit: field.Unit,
	}
	var values []*pb.ValueDefine
	for _, v := range field.Values {
		values = append(values, CopyValue(v))
	}
	clone.Values = values
	return clone
}

// CopyValue .
func CopyValue(v *pb.ValueDefine) *pb.ValueDefine {
	val := &pb.ValueDefine{
		Name: v.Name,
	}
	if v.Value != nil {
		value, _ := structpb.NewValue(v.Value.AsInterface())
		val.Value = value
	}
	return val
}

// CopyMeta
func CopyMeta(m *pb.MetricMeta) *pb.MetricMeta {
	n := &pb.MetricMeta{
		Tags:   make(map[string]*pb.TagDefine),
		Fields: make(map[string]*pb.FieldDefine),
	}
	n.Name = &pb.NameDefine{
		Key:  m.Name.Key,
		Name: m.Name.Name,
	}
	for k, t := range m.Tags {
		n.Tags[k] = CopyTagDefine(t)
	}
	for k, f := range m.Fields {
		n.Fields[k] = CopyFieldDefine(f)
	}
	return n
}
