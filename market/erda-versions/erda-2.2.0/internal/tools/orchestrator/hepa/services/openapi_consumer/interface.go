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

package openapi_consumer

import (
	"context"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

var Service GatewayOpenapiConsumerService

type GatewayOpenapiConsumerService interface {
	Clone(context.Context) GatewayOpenapiConsumerService
	GrantPackageToConsumer(consumerId, packageId string) error
	RevokePackageFromConsumer(consumerId, packageId string) error
	CreateClientConsumer(clientName, clientId, clientSecret, clusterName string) (*orm.GatewayConsumer, error)
	CreateConsumer(*dto.DiceArgsDto, *dto.OpenConsumerDto) (string, bool, error)
	GetConsumers(*dto.GetOpenConsumersDto) (common.NewPageQuery, error)
	GetConsumersName(*dto.GetOpenConsumersDto) ([]dto.OpenConsumerInfoDto, error)
	UpdateConsumer(string, *dto.OpenConsumerDto) (*dto.OpenConsumerInfoDto, error)
	DeleteConsumer(string) (bool, error)
	GetConsumerCredentials(string) (dto.ConsumerCredentialsDto, error)
	UpdateConsumerCredentials(string, *dto.ConsumerCredentialsDto) (dto.ConsumerCredentialsDto, string, error)
	GetConsumerAcls(string) ([]dto.ConsumerAclInfoDto, error)
	UpdateConsumerAcls(string, *dto.ConsumerAclsDto) (bool, error)
	GetConsumersOfPackage(string) ([]orm.GatewayConsumer, error)
	GetKongConsumerName(consumer *orm.GatewayConsumer) string
	GetPackageAcls(string) ([]dto.PackageAclInfoDto, error)
	UpdatePackageAcls(string, *dto.PackageAclsDto) (bool, error)
	GetPackageApiAcls(string, string) ([]dto.PackageAclInfoDto, error)
	UpdatePackageApiAcls(string, string, *dto.PackageAclsDto) (bool, error)
}
