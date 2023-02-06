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
	"sort"
	"strings"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	metricpkg "github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

// GroupMetricMap .
type GroupMetricMap struct {
	Name            string          `json:"name"`
	Filters         []*pb.TagFilter `json:"filters"`
	Fields          []string        `json:"fields"`
	Tags            []string        `json:"tags"`
	AddTagsToFields bool            `json:"add_tags_to_fields"`
}

// GroupProvider .
type GroupProvider interface {
	MappingsByID(id, scope, scopeID string, metric []string, ms map[string]*pb.MetricMeta) ([]*GroupMetricMap, error)
	Groups(langCodes i18n.LanguageCodes, t i18n.Translator, scope, scopeID string, ms map[string]*pb.MetricMeta) ([]*pb.Group, error)
}

func (m *Manager) getGroupProviers() (list []GroupProvider) {
	for _, fn := range m.groupProviders {
		list = append(list, fn())
	}
	return list
}

func (m *Manager) getGroupAndMetric(id string) (string, []string) {
	idx := strings.Index(id, "@")
	if idx > 0 {
		if len(id[idx+1:]) > 0 {
			return id[:idx], []string{id[idx+1:]}
		}
		return id[:idx], nil
	}
	return id, nil
}

func (m *Manager) MetricGroups(langs i18n.LanguageCodes, scope, scopeID, mode string) ([]*pb.Group, error) {
	ms, err := m.getMetricMeta(langs, scope, scopeID)
	if err != nil {
		return nil, err
	}
	gp := m.getGroupProviers()
	var gs []*pb.Group
	t := m.i18n.Translator("_group")
	for _, p := range gp {
		g, err := p.Groups(langs, t, scope, scopeID, ms)
		if err != nil {
			return nil, err
		}
		gs = appendGroups(gs, g)
	}
	sortGroups(gs)
	return gs, nil
}

func appendGroups(group1, group2 []*pb.Group) []*pb.Group {
	for _, g1 := range group1 {
		for _, g2 := range group2 {
			if g1.Id == g2.Id {
				if len(g2.Name) > 0 {
					g1.Name = g2.Name
				}
				g1.Children = appendGroups(g1.Children, g2.Children)
				break
			}
		}
	}
	for _, g2 := range group2 {
		var find bool
		for _, g1 := range group1 {
			if g2.Id == g1.Id {
				find = true
				break
			}
		}
		if !find {
			group1 = append(group1, g2)
		}
	}
	return group1
}

func sortGroups(groups []*pb.Group) {
	sort.Slice(groups, func(i, j int) bool {
		return groups[i].Order < groups[j].Order
	})
	for _, g := range groups {
		if len(g.Children) <= 0 {
			continue
		}
		sortGroups(g.Children)
	}
}

// formats .
const (
	InfluxFormat = "influx"
	DotFormat    = "dot"
)

func (m *Manager) MetricGroup(langCodes i18n.LanguageCodes, scope, scopeID, id, mode, format string, appendTags bool) (*pb.MetricGroup, error) {
	groupID, metric := m.getGroupAndMetric(id)
	ms, err := m.getMetricMeta(langCodes, scope, scopeID, metric...)
	if err != nil {
		return nil, err
	}
	gp := m.getGroupProviers()
	var mappings []*GroupMetricMap
	for _, p := range gp {
		m, err := p.MappingsByID(groupID, scope, scopeID, metric, ms)
		if err != nil {
			return nil, err
		}
		mappings = append(mappings, m...)
	}

	gd := &pb.MetricGroup{
		Id: id,
	}
	if format == InfluxFormat {
		gd.Meta, err = m.getTypeAggDefineInflux(langCodes, mode)
		if err != nil {
			return nil, err
		}
	} else {
		gd.Meta, err = m.getTypeAggDefine(langCodes, mode)
		if err != nil {
			return nil, err
		}
	}

	for _, gm := range mappings {
		mm := ms[gm.Name]
		if mm == nil {
			continue
		}
		gmm := &pb.GroupMetricMeta{
			Metric:  mm.Name.Key,
			Name:    mm.Name.Name,
			Filters: gm.Filters,
		}

		if len(gm.Fields) <= 0 {
			for _, f := range metricpkg.FieldsKeys(mm) {
				fd := mm.Fields[f]
				if fd != nil {
					fd.Key = getFieldKeyWithFormat(format, fd.Key, "field")
					gmm.Fields = append(gmm.Fields, fd)
				}
			}
		} else {
			for _, f := range gm.Fields {
				fd := mm.Fields[f]
				if fd != nil {
					fd.Key = getFieldKeyWithFormat(format, fd.Key, "field")
					gmm.Fields = append(gmm.Fields, fd)
				}
			}
		}
		if len(gm.Tags) > 0 {
			for _, t := range gm.Tags {
				td := mm.Tags[t]
				if td != nil {
					fd := &pb.FieldDefine{
						Key:    getFieldKeyWithFormat(format, t, "tag"),
						Type:   StringType,
						Name:   td.Name,
						Values: td.Values,
					}
					gmm.Fields = append(gmm.Fields, fd)
				}
			}
		} else if gm.AddTagsToFields || appendTags {
			for _, t := range metricpkg.TagsKeys(mm) {
				td := mm.Tags[t]
				if td != nil {
					fd := &pb.FieldDefine{
						Key:    getFieldKeyWithFormat(format, t, "tag"),
						Type:   StringType,
						Name:   td.Name,
						Values: td.Values,
					}
					gmm.Fields = append(gmm.Fields, fd)
				}
			}
		}
		for _, t := range metricpkg.TagsKeys(mm) {
			td := mm.Tags[t]
			if td != nil {
				gmm.Tags = append(gmm.Tags, mm.Tags[t])
			}
		}
		gd.Metrics = append(gd.Metrics, gmm)
	}
	return gd, nil
}

func getFieldKeyWithFormat(format, key, typ string) string {
	if format == InfluxFormat {
		return key + "::" + typ
	} else if format == DotFormat || typ == "tag" {
		return typ + "s." + key
	}
	return key
}

func appendMetricToGroup(groups []*pb.Group, sep string, metricmeta map[string]*pb.MetricMeta, mapping map[string][]*GroupMetricMap, forceAppend bool) []*pb.Group {
	for _, g := range groups {
		g.Children = appendMetricToGroup(g.Children, sep, metricmeta, mapping, forceAppend)
		if len(mapping[g.Id]) > 1 || forceAppend {
			for _, m := range mapping[g.Id] {
				c := &pb.Group{
					Id:   g.Id + sep + m.Name,
					Name: m.Name,
				}
				meta := metricmeta[m.Name]
				if meta != nil {
					c.Name = meta.Name.Name
				}
				g.Children = append(g.Children, c)
			}
		}
	}
	return groups
}
