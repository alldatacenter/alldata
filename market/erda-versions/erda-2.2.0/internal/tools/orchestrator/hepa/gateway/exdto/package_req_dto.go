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

package exdto

import "github.com/pkg/errors"

// AuthType
const (
	AT_KEY_AUTH   = "key-auth"
	AT_OAUTH2     = "oauth2"
	AT_SIGN_AUTH  = "sign-auth"
	AT_ALIYUN_APP = "aliyun-app"
)

// AclType
const (
	ACL      = "acl"
	ACL_NONE = ""
	ACL_ON   = "on"
	ACL_OFF  = "off"
)

// Scene
const (
	OPENAPI_SCENE = "openapi"
	WEBAPI_SCENE  = "webapi"
	UNITY_SCENE   = "unity"
)

type PackageDto struct {
	Name             string   `json:"name"`
	BindDomain       []string `json:"bindDomain"`
	AuthType         string   `json:"authType"`
	AclType          string   `json:"aclType"`
	Scene            string   `json:"scene"`
	Description      string   `json:"description"`
	NeedBindCloudapi bool     `json:"needBindCloudapi"`
}

func (dto PackageDto) CheckValid() error {
	if dto.Name == "" || len(dto.BindDomain) == 0 || dto.Scene == "" {
		goto failed
	}
	if !dto.NeedBindCloudapi && dto.AuthType == AT_ALIYUN_APP {
		goto failed
	}
	return nil
failed:
	return errors.Errorf("invalid dto: %+v", dto)
}
