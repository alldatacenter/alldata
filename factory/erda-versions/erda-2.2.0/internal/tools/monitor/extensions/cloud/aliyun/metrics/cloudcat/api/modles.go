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

package api

// Metric .
type Metric struct {
	Name      string                 `json:"name"`
	Timestamp uint64                 `json:"timestamp"`
	Tags      map[string]string      `json:"tags"`
	Fields    map[string]interface{} `json:"fields"`
}

// OrgInfo .
type OrgInfo struct {
	OrgId        string
	OrgName      string
	AccessKey    string
	AccessSecret string
}

// ProjectMeta .
type ProjectMeta struct {
	Description string
	Labels      map[string]interface{}
	Namespace   string
}

type MetricMeta struct {
}

type DataPoints string

// type DataPoint struct {
// 	Timestamp  uint64      `json:"timestamp"`
// 	UserId     string      `json:"userId"`
// 	InstanceId string      `json:"instanceId"`
// 	Average    interface{} `json:"Average,omitempty"`
// 	Maximum    interface{} `json:"Maximum,omitempty"`
// 	Minimum    interface{} `json:"Minimum,omitempty"`
// 	Value      interface{} `json:"Value,omitempty"`
// }
