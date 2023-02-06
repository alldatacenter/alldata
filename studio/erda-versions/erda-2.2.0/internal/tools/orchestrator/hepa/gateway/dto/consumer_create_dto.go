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
	CT_PRO = "project"
	CT_ORG = "org"
)

type OpenConsumerDto struct {
	Name        string `json:"name"`
	Description string `json:"description"`
	Type        string `json:"-"`
}

type ConsumerCreateDto struct {
	OrgId        int    `json:"orgId"`
	ProjectId    string `json:"projectId"`
	Env          string `json:"env"`
	ConsumerId   string `json:"consumerId"`
	ConsumerName string `json:"consumerName"`
	Description  string `json:"description"`
}

func NewConsumerCreateDto() *ConsumerCreateDto {
	return &ConsumerCreateDto{ConsumerName: "default"}
}

func (dto ConsumerCreateDto) IsEmpty() bool {
	return len(dto.ProjectId) == 0 || len(dto.Env) == 0 || len(dto.ConsumerName) == 0
}
