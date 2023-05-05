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

package dto

const (
	BASE_PRIORITY                 = 0
	WILDCARD_DOMAIN_BASE_PRIORITY = 1000
)

type ZoneKongPolicies struct {
	Id          string `json:"id"`
	Regex       string `json:"regex"`
	Enables     string `json:"enable"`
	Disables    string `json:"disable"`
	Priority    int    `json:"priority"`
	PackageName string `json:"packageName"`
	ProjectId   string `json:"-"`
	Env         string `json:"-"`
}

type SortByRegexList []ZoneKongPolicies

func (list SortByRegexList) Len() int { return len(list) }

func (list SortByRegexList) Swap(i, j int) { list[i], list[j] = list[j], list[i] }

func (list SortByRegexList) Less(i, j int) bool {
	return list[i].Priority > list[j].Priority
}
