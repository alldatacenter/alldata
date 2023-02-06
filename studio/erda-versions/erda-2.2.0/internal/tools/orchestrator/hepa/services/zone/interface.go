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

package zone

import (
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/k8s"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

type RouteConfig struct {
	Hosts []string
	Path  string
	k8s.RouteOptions
}

type ZoneRoute struct {
	Route RouteConfig
}

type ZoneConfig struct {
	*ZoneRoute
	Name      string
	ProjectId string
	Env       string
	Az        string
	Type      string
}

var Service GatewayZoneService

type GatewayZoneService interface {
	CreateZoneWithoutIngress(ZoneConfig, ...*service.SessionHelper) (*orm.GatewayZone, error)
	CreateZone(ZoneConfig, ...*service.SessionHelper) (*orm.GatewayZone, error)
	DeleteZoneRoute(string, ...*service.SessionHelper) error
	UpdateZoneRoute(string, ZoneRoute, ...*service.SessionHelper) (bool, error)
	// trigger domain-policy update
	SetZoneKongPolicies(string, dto.ZoneKongPolicies, *service.SessionHelper) error
	SetZoneKongPoliciesWithoutDomainPolicy(zoneId string, policies *dto.ZoneKongPolicies, helper *service.SessionHelper) error
	UpdateKongDomainPolicy(az, projectId, env string, helper *service.SessionHelper) error
	DeleteZone(string) error
	UpdateBuiltinPolicies(string) error
	GetZone(string, ...*service.SessionHelper) (*orm.GatewayZone, error)
}
