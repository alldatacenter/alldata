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
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/hepa/openapi_consumer/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
)

type KongCredentialListDto struct {
	Total int64               `json:"total"`
	Data  []KongCredentialDto `json:"data"`
}

func (dto KongCredentialDto) ToCredential() *pb.Credential {
	res := &pb.Credential{
		ConsumerId:   dto.ConsumerId,
		CreatedAt:    dto.CreatedAt,
		Id:           dto.Id,
		Key:          dto.Key,
		RedirectUrls: dto.RedirectUrls,
		Name:         dto.Name,
		ClientId:     dto.ClientId,
		ClientSecret: dto.ClientSecret,
		Secret:       dto.Secret,
		Username:     dto.Username,
	}
	v, _ := structpb.NewValue(util.GetPureInterface(dto.RedirectUrl))
	res.RedirectUrl = v
	return res
}

func FromCredential(cred *pb.Credential) KongCredentialDto {
	res := KongCredentialDto{
		ConsumerId:   cred.ConsumerId,
		CreatedAt:    cred.CreatedAt,
		Id:           cred.Id,
		Key:          cred.Key,
		RedirectUrls: cred.RedirectUrls,
		Name:         cred.Name,
		ClientId:     cred.ClientId,
		ClientSecret: cred.ClientSecret,
		Secret:       cred.Secret,
		Username:     cred.Username,
	}
	res.RedirectUrl = cred.RedirectUrl.AsInterface()
	return res
}

type KongCredentialDto struct {
	ConsumerId string `json:"consumer_id,omitempty"`
	CreatedAt  int64  `json:"created_at,omitempty"`
	Id         string `json:"id,omitempty"`
	// key-auth, sign-auth
	Key string `json:"key,omitempty"`
	// oauth2
	RedirectUrl interface{} `json:"redirect_uri,omitempty"`
	// v2
	RedirectUrls []string `json:"redirect_uris,omitempty"`
	Name         string   `json:"name,omitempty"`
	ClientId     string   `json:"client_id,omitempty"`
	ClientSecret string   `json:"client_secret,omitempty"`
	// sign-auth, hmac-auth
	Secret string `json:"secret,omitempty"`
	// hmac-auth
	Username string `json:"username,omitempty"`
}

func (dto *KongCredentialDto) ToHmacReq() {
	if dto == nil {
		return
	}
	if dto.Username == "" {
		dto.Username = dto.Key
		dto.Key = ""
	}
}

func (dto *KongCredentialDto) ToHmacResp() {
	if dto == nil {
		return
	}
	if dto.Key == "" {
		dto.Key = dto.Username
		dto.Username = ""
	}
}

func (dto *KongCredentialDto) ToV2() {
	if dto == nil {
		return
	}
	if url, ok := dto.RedirectUrl.([]string); ok && len(url) > 0 {
		dto.RedirectUrls = url
		dto.RedirectUrl = nil
	}
}

func (dto *KongCredentialDto) Compatiable() {
	if dto == nil {
		return
	}
	dto.RedirectUrl = dto.RedirectUrls
}

func (dto *KongCredentialDto) AdjustCreatedAt() {
	dto.CreatedAt *= 1000
}
