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

package legacy_consumer

import (
	"context"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

var Service GatewayConsumerService

type GatewayConsumerService interface {
	Clone(context.Context) GatewayConsumerService
	CreateDefaultConsumer(string, string, string, string) (*orm.GatewayConsumer, *orm.ConsumerAuthConfig, vars.StandardErrorCode, error)
	CreateConsumer(*dto.ConsumerCreateDto) *common.StandardResult
	GetProjectConsumerInfo(string, string, string) (*dto.ConsumerDto, error)
	GetConsumerInfo(string) *common.StandardResult
	UpdateConsumerInfo(string, *dto.ConsumerDto) *common.StandardResult
	GetConsumerList(string, string, string) *common.StandardResult
	DeleteConsumer(string) *common.StandardResult
}
