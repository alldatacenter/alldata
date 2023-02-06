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

package chartmeta

import (
	"encoding/json"
	"fmt"
	"strings"
)

type Table struct {
	Name        string `gorm:"column:name"`
	Title       string `gorm:"column:title"`
	MetricsName string `gorm:"column:metricsName"`
	Fields      string `gorm:"column:fields"`
	Parameters  string `gorm:"column:parameters"`
	Type        string `gorm:"column:type"`
	Order       int    `gorm:"column:order"`
	Unit        string `gorm:"column:unit"`
}

type FieldInfo struct {
	DataMeta
	// ChartType string `yaml:"chart_type" json:"chart_type"`
}

func (m *Manager) LoadDatabase() error {
	var list []*Table
	if err := m.db.Table("chart_meta").Order("`type`,`order` ASC").Find(&list).Error; err != nil {
		return err
	}
	nameMap := make(map[string]*ChartMeta)
	typeMap := make(map[string][]*ChartMeta)
	for _, line := range list {
		if _, ok := nameMap[line.Name]; ok {
			m.log.Warnf("name %s conflicts", line.Name)
		}
		var defines map[string]*DataMeta
		var parameters map[string][]string
		// var chartType string
		line.Fields = strings.TrimSpace(line.Fields)
		if len(line.Fields) != 0 {
			var fields map[string]*FieldInfo
			err := json.Unmarshal([]byte(line.Fields), &fields)
			if err != nil {
				m.log.Warnf("%s invalid fields format: %s", line.Name, err)
				continue
			}
			defines = make(map[string]*DataMeta)
			for key, field := range fields {
				field.DataMeta.OriginalUnit = &line.Unit
				defines[key] = &field.DataMeta
				// if chartType == "" {
				// 	chartType = field.ChartType
				// } else if chartType != field.ChartType {
				// 	fmt.Println(line.Name, chartType, field.ChartType)
				// }
				if field.AxisIndex == nil || field.ChartType == nil || field.Label == nil ||
					field.Unit == nil || field.UnitType == nil {
					m.log.Warnf("database chart field unset some key")
				}
			}
		}
		line.Parameters = strings.TrimSpace(line.Parameters)
		if len(line.Parameters) != 0 {
			var ps map[string]interface{}
			err := json.Unmarshal([]byte(line.Parameters), &ps)
			if err != nil {
				m.log.Warnf("%s invalid parameters format: %s", line.Name, err)
				continue
			}
			parameters = make(map[string][]string)
			for key, vals := range ps {
				switch value := vals.(type) {
				case []interface{}:
					for _, val := range value {
						parameters[key] = append(parameters[key], fmt.Sprint(val))
					}
				case []string:
					parameters[key] = value
				case string:
					parameters[key] = []string{value}
				default:
					parameters[key] = []string{fmt.Sprint(value)}
				}
			}
		}
		typ := line.Type
		if typ == "0" || typ == "" {
			typ = "default"
		}
		cm := &ChartMeta{
			Name:        line.Name,
			Title:       line.Title,
			MetricNames: line.MetricsName,
			Defines:     defines,
			Parameters:  parameters,
			Type:        typ,
			// ChartType:   chartType,
			Order: line.Order,
		}
		cm.mergeParams()
		nameMap[line.Name] = cm
		typeMap[typ] = append(typeMap[typ], cm)
	}
	m.reloadCharts(nameMap, typeMap)
	return nil
}
