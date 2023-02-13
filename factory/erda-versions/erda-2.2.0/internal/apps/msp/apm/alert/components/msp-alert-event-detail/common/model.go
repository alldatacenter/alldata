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

package common

type Sort struct {
	FieldKey  string
	Ascending bool
}

type AlertExpression struct {
	Filters   []*AlertExpressionFilter   `json:"filters"`
	Functions []*AlertExpressionFunction `json:"functions"`
	Group     []string                   `json:"group"`
	Metric    string                     `json:"metric"`
	Metrics   []string                   `json:"metrics"`
	Outputs   []string                   `json:"outputs"`
	Select    map[string]string          `json:"select"`
	Window    *int64                     `json:"window"`
}

type AlertExpressionFilter struct {
	DataType string      `json:"dataType"`
	Operator string      `json:"operator"`
	Tag      string      `json:"tag"`
	Value    interface{} `json:"value"`
}

type AlertExpressionFunction struct {
	Alias       *string     `json:"alias"`
	Aggregator  string      `json:"aggregator"`
	Field       string      `json:"field"`
	FieldScript *string     `json:"fieldScript"`
	Operator    *string     `json:"operator"`
	Value       interface{} `json:"value"`
	Trigger     string      `json:"trigger"`
	Condition   string      `json:"condition"`
}
