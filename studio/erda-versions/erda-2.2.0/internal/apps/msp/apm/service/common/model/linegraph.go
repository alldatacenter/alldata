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

package model

import (
	"strings"

	"github.com/ahmetb/go-linq/v3"
	"google.golang.org/protobuf/types/known/structpb"

	structure "github.com/erda-project/erda-infra/providers/component-protocol/components/commodel/data-structure"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/linegraph"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/pkg/math"
)

type LineGraphMetaData struct {
	Time      string  `json:"time,omitempty"`
	Value     float64 `json:"value,omitempty"`
	Dimension string  `json:"dimension,omitempty"`
}

func ToQueryParams(tenantId string, serviceId string, instanceId string) map[string]*structpb.Value {
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
		"service_id":   structpb.NewStringValue(serviceId),
		"instance_id":  structpb.NewStringValue(instanceId),
	}
	return queryParams
}

func HandleLineGraphMetaData(lang i18n.LanguageCodes, i18n i18n.Translator, title string, dataType structure.Type, dataPrecision structure.Precision, graph []*LineGraphMetaData) *linegraph.Data {
	line := linegraph.New(i18n.Text(lang, title))
	var yAxis []linq.Group
	linq.From(graph).GroupBy(
		func(group interface{}) interface{} { return group.(*LineGraphMetaData).Dimension },
		func(value interface{}) interface{} {
			return math.DecimalPlacesWithDigitsNumber(value.(*LineGraphMetaData).Value, 2)
		},
	).ToSlice(&yAxis)
	for _, group := range yAxis {
		line.SetYAxis(group.Key.(string), group.Group...)
	}

	var xAxis []interface{}
	linq.From(graph).Where(func(i interface{}) bool {
		return i.(*LineGraphMetaData).Dimension == line.Dimensions[0]
	}).Select(func(i interface{}) interface{} {
		t := i.(*LineGraphMetaData).Time
		t = strings.ReplaceAll(t, "T", " ")
		t = strings.ReplaceAll(t, "Z", "")
		return t
	}).ToSlice(&xAxis)
	line.SetXAxis(xAxis...)
	line.SetYOptions(&linegraph.Options{Structure: &structure.DataStructure{
		Type:      dataType,
		Precision: structure.Precision(i18n.Text(lang, string(dataPrecision))),
		Enable:    true,
	}})

	return line
}
