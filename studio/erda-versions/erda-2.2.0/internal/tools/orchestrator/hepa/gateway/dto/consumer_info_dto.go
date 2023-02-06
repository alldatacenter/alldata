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

import "github.com/erda-project/erda-proto-go/core/hepa/openapi_consumer/pb"

type ConsumerInfoDto struct {
	ConsumerId       string               `json:"consumerId"`
	ConsumerName     string               `json:"consumerName"`
	ConsumerApiInfos []ConsumerApiInfoDto `json:"consumerApiInfos"`
}

type OpenConsumerInfoDto struct {
	Id       string `json:"id"`
	CreateAt string `json:"createAt"`
	OpenConsumerDto
}

func (dto OpenConsumerInfoDto) ToConsumer() *pb.Consumer {
	return &pb.Consumer{
		Id:          dto.Id,
		CreateAt:    dto.CreateAt,
		Name:        dto.Name,
		Description: dto.Description,
	}
}
