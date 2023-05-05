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

package orm

import (
	"github.com/erda-project/erda-proto-go/core/hepa/openapi_consumer/pb"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
)

const (
	OAUTH2       = "oauth2"
	KEYAUTH      = "key-auth"
	SIGNAUTH     = "sign-auth"
	HMACAUTH     = "hmac-auth"
	KeyAuthTips  = "请将appKey带在名为appKey的url参数或者名为X-App-Key的请求头上"
	SignAuthTips = "请将appKey带在名为appKey的url参数上，将参数签名串带在名为sign的url参数上"
)

type AuthItem struct {
	AuthType string                        `json:"authType"`
	AuthData kongDto.KongCredentialListDto `json:"authData"`
	AuthTips string                        `json:"authTips"`
}

type ConsumerAuthConfig struct {
	Auths []AuthItem `json:"auths"`
}

func (item AuthItem) ToAuth() *pb.ConsumerAuthItem {
	res := &pb.ConsumerAuthItem{
		AuthType: item.AuthType,
		AuthTips: item.AuthTips,
	}
	authData := &pb.CredentialList{
		Total: item.AuthData.Total,
	}
	data := []*pb.Credential{}
	for _, auth := range item.AuthData.Data {
		data = append(data, auth.ToCredential())
	}
	authData.Data = data
	res.AuthData = authData
	return res
}

func FromAuth(item *pb.ConsumerAuthItem) AuthItem {
	res := AuthItem{
		AuthType: item.AuthType,
		AuthTips: item.AuthTips,
	}
	if item.AuthData == nil {
		return res
	}
	authData := kongDto.KongCredentialListDto{
		Total: item.AuthData.Total,
	}
	data := []kongDto.KongCredentialDto{}
	for _, auth := range item.AuthData.Data {
		data = append(data, kongDto.FromCredential(auth))
	}
	authData.Data = data
	res.AuthData = authData
	return res
}
