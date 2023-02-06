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

package cors

import (
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
)

const (
	ANNOTATION_CORS_ENABLE      = "nginx.ingress.kubernetes.io/enable-cors"
	ANNOTATION_CORS_METHODS     = "nginx.ingress.kubernetes.io/cors-allow-methods"
	ANNOTATION_CORS_HEADERS     = "nginx.ingress.kubernetes.io/cors-allow-headers"
	ANNOTATION_CORS_ORIGIN      = "nginx.ingress.kubernetes.io/cors-allow-origin"
	ANNOTATION_CORS_CREDENTIALS = "nginx.ingress.kubernetes.io/cors-allow-credentials"
	ANNOTATION_CORS_MAXAGE      = "nginx.ingress.kubernetes.io/cors-max-age"
)

type PolicyDto struct {
	apipolicy.BaseDto
	Methods     string `json:"methods"`
	Headers     string `json:"headers"`
	Origin      string `json:"origin"`
	Credentials bool   `json:"credentials"`
	MaxAge      int64  `json:"maxAge"`
}

func (dto PolicyDto) IsValidDto() (bool, string) {
	if !dto.Switch {
		return true, ""
	}
	if dto.Methods == "" {
		return false, "HTTP方法字段不能为空"
	}
	if dto.Methods == "*" {
		return false, "HTTP方法不允许配置通配符"
	}
	if dto.Headers == "" {
		return false, "HTTP请求头字段不能为空"
	}
	if dto.Headers == "*" {
		return false, "HTTP请求头不允许配置通配符"
	}
	if dto.Origin == "" {
		return false, "跨域地址字段不能为空"
	}
	if dto.MaxAge <= 0 {
		return false, "预检请求缓存时间需要大于0"
	}
	return true, ""
}
