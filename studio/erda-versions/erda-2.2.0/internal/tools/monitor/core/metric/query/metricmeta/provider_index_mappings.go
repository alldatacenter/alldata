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

package metricmeta

import (
	"context"
	"fmt"
	"strings"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	indexloader "github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

// IndexMappingsGroupProvider .
type IndexMappingsGroupProvider struct {
	index indexloader.Interface
}

// NewIndexMappingsGroupProvider .
func NewIndexMappingsGroupProvider(index indexloader.Interface) (*IndexMappingsGroupProvider, error) {
	return &IndexMappingsGroupProvider{index}, nil
}

// MappingsByID .
func (p *IndexMappingsGroupProvider) MappingsByID(id, scope, scopeID string, names []string, ms map[string]*pb.MetricMeta) (gmm []*GroupMetricMap, err error) {
	for _, name := range names {
		if mm, ok := ms[name]; ok {
			gmm = append(gmm, &GroupMetricMap{
				Name:            mm.Name.Key,
				AddTagsToFields: true,
			})
		}
	}
	return gmm, nil
}

// Groups .
func (p *IndexMappingsGroupProvider) Groups(langCodes i18n.LanguageCodes, t i18n.Translator, scope, scopeID string, ms map[string]*pb.MetricMeta) (groups []*pb.Group, err error) {
	names := p.index.Keys()
	for _, name := range names {
		if strings.HasPrefix(name, "_") {
			continue
		}
		groups = append(groups, &pb.Group{
			Id:   name + "@" + name,
			Name: t.Text(langCodes, name),
		})
	}
	return groups, nil
}

// IndexMappingsMetricMetaProvider .
type IndexMappingsMetricMetaProvider struct {
	index indexloader.Interface
}

// NewIndexMappingsMetricMetaProvider .
func NewIndexMappingsMetricMetaProvider(index indexloader.Interface) (*IndexMappingsMetricMetaProvider, error) {
	return &IndexMappingsMetricMetaProvider{index}, nil
}

// MetricMeta .
func (p *IndexMappingsMetricMetaProvider) MetricMeta(langCodes i18n.LanguageCodes, i i18n.I18n, scope, scopeID string, names ...string) (map[string]*pb.MetricMeta, error) {
	var indices []string
	// if len(names) > 0 {
	// 	for _, n := range names {
	// 		indices = append(indices, p.index.IndexPrefix()+"-"+n+"-*")
	// 	}
	// } else {
	// 	indices = append(indices, p.index.IndexPrefix()+"-*")
	// }

	for _, prefix := range p.index.Prefixes() {
		indices = append(indices, prefix+"*")
	}
	resp, err := p.getMappings(indices)
	if err != nil {
		return nil, err
	}
	metas := make(map[string]*pb.MetricMeta)
	for key, m := range resp {
		match := p.index.Match(key)
		if match == nil {
			continue
		}
		if len(match.Keys) <= 1 || match.Keys[0] == "empty" {
			continue
		}
		key = match.Keys[0]
		mp, ok := m.(map[string]interface{})
		if !ok {
			continue
		}
		mp, ok = mp["mappings"].(map[string]interface{})
		if !ok {
			continue
		}
		if len(mp) <= 0 {
			continue
		}
		for _, m := range mp {
			m, ok := m.(map[string]interface{})
			if ok {
				mp = m
				break
			}
		}
		if len(mp) <= 0 {
			continue
		}
		mp, ok = mp["properties"].(map[string]interface{})
		if !ok {
			continue
		}
		tags, ok := mp["tags"].(map[string]interface{})
		if !ok {
			continue
		}
		tags, ok = tags["properties"].(map[string]interface{})
		if !ok {
			continue
		}
		fields, ok := mp["fields"].(map[string]interface{})
		if !ok {
			continue
		}
		fields, ok = fields["properties"].(map[string]interface{})
		if !ok {
			continue
		}
		meta := metric.NewMeta()
		meta.Name.Key, meta.Name.Name = key, key
		for k := range tags {
			if !strings.HasPrefix(k, "_") {
				meta.Tags[k] = &pb.TagDefine{
					Key:  k,
					Name: k,
				}
			}
		}
		for k, v := range fields {
			info, ok := v.(map[string]interface{})
			if !ok {
				continue
			}
			typ, ok := info["type"]
			if !ok {
				continue
			}
			t := fmt.Sprint(typ)
			switch t {
			case "keyword", "string":
				t = StringType
			case "double", "long":
				t = NumberType
			case "boolean":
				t = BoolType
			default:
				t = StringType
			}
			meta.Fields[k] = &pb.FieldDefine{
				Key:  k,
				Type: t,
				Name: k,
				Unit: "",
			}
		}
		metas[key] = mergeMetricMeta(meta, metas[key])
	}
	return transMetricMetas(langCodes, i, metas), nil
}

func mergeMetricMeta(m1, m2 *pb.MetricMeta) *pb.MetricMeta {
	if m2 == nil {
		return m1
	}
	if m1 == nil {
		return m2
	}
	for t, tag := range m2.Tags {
		if _, ok := m1.Tags[t]; !ok {
			m1.Tags[t] = tag
		}
	}
	for f, field := range m2.Fields {
		if _, ok := m1.Fields[f]; !ok {
			m1.Fields[f] = field
		}
	}
	return m1
}

func (p *IndexMappingsMetricMetaProvider) getMappings(indices []string) (map[string]interface{}, error) {
	context, cancel := context.WithTimeout(context.Background(), p.index.RequestTimeout()*2)
	defer cancel()
	return p.index.Client().GetMapping().Index(indices...).IgnoreUnavailable(true).AllowNoIndices(true).Do(context)
}
