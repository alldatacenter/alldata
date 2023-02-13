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

import (
	"github.com/gin-gonic/gin"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type GetOpenLimitRulesDto struct {
	DiceArgsDto
	ConsumerId string
	PackageId  string
}

func NewGetOpenLimitRulesDto(c *gin.Context) GetOpenLimitRulesDto {
	return GetOpenLimitRulesDto{
		DiceArgsDto: NewDiceArgsDto(c),
		ConsumerId:  c.Query("consumerId"),
		PackageId:   c.Query("packageId"),
	}
}

func (impl GetOpenLimitRulesDto) GenSelectOptions() []orm.SelectOption {
	options := impl.DiceArgsDto.GenSelectOptions()
	if impl.ConsumerId != "" {
		options = append(options, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "consumer_id",
			Value:  impl.ConsumerId,
		})
	}
	if impl.PackageId != "" {
		options = append(options, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "package_id",
			Value:  impl.PackageId,
		})
	}
	return options
}
