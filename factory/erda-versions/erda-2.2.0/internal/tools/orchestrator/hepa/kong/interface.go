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

package kong

import (
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
)

type KongAdapter interface {
	KongExist() bool
	GetVersion() (string, error)
	CheckPluginEnabled(pluginName string) (bool, error)
	CreateConsumer(req *KongConsumerReqDto) (*KongConsumerRespDto, error)
	DeleteConsumer(string) error
	CreateOrUpdateRoute(req *KongRouteReqDto) (*KongRouteRespDto, error)
	DeleteRoute(string) error
	UpdateRoute(req *KongRouteReqDto) (*KongRouteRespDto, error)
	CreateOrUpdateService(req *KongServiceReqDto) (*KongServiceRespDto, error)
	DeleteService(string) error
	DeletePluginIfExist(req *KongPluginReqDto) error
	CreateOrUpdatePlugin(req *KongPluginReqDto) (*KongPluginRespDto, error)
	CreateOrUpdatePluginById(req *KongPluginReqDto) (*KongPluginRespDto, error)
	GetPlugin(req *KongPluginReqDto) (*KongPluginRespDto, error)
	AddPlugin(req *KongPluginReqDto) (*KongPluginRespDto, error)
	UpdatePlugin(req *KongPluginReqDto) (*KongPluginRespDto, error)
	PutPlugin(req *KongPluginReqDto) (*KongPluginRespDto, error)
	RemovePlugin(string) error
	CreateCredential(req *KongCredentialReqDto) (*KongCredentialDto, error)
	DeleteCredential(string, string, string) error
	GetCredentialList(string, string) (*KongCredentialListDto, error)
	CreateAclGroup(string, string) error
	CreateUpstream(req *KongUpstreamDto) (*KongUpstreamDto, error)
	GetUpstreamStatus(string) (*KongUpstreamStatusRespDto, error)
	AddUpstreamTarget(string, *KongTargetDto) (*KongTargetDto, error)
	DeleteUpstreamTarget(string, string) error
	TouchRouteOAuthMethod(string) error
	GetRoutes() ([]KongRouteRespDto, error)
	GetRoutesWithTag(tag string) ([]KongRouteRespDto, error)
}
