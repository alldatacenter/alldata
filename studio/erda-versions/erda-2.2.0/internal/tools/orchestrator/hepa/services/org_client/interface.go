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

package org_client

import (
	"context"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/exdto"
)

var Service GatewayOrgClientService

type GatewayOrgClientService interface {
	Clone(context.Context) GatewayOrgClientService
	Create(orgId, name string) (dto.ClientInfoDto, bool, error)
	Delete(id string) (bool, error)
	GetCredentials(id string) (dto.ClientInfoDto, error)
	UpdateCredentials(id string, secret ...string) (dto.ClientInfoDto, error)
	GrantPackage(id, packageId string) (bool, error)
	RevokePackage(id, packageId string) (bool, error)
	CreateOrUpdateLimit(id, packageId string, limits exdto.ChangeLimitsReq) (bool, error)
}
