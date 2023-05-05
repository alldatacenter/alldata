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

package proxy

import (
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
)

const (
	ANNOTATION_REQ_BUFFER         = "nginx.ingress.kubernetes.io/proxy-request-buffering"
	ANNOTATION_RESP_BUFFER        = "nginx.ingress.kubernetes.io/proxy-buffering"
	ANNOTATION_REQ_LIMIT          = "nginx.ingress.kubernetes.io/proxy-body-size"
	ANNOTATION_PROXY_REQ_TIMEOUT  = "nginx.ingress.kubernetes.io/proxy-send-timeout"
	ANNOTATION_PROXY_RESP_TIMEOUT = "nginx.ingress.kubernetes.io/proxy-read-timeout"
	ANNOTATION_SERVER_SNIPPET     = "nginx.ingress.kubernetes.io/server-snippet"
	ANNOTATION_SSL_REDIRECT       = "nginx.ingress.kubernetes.io/ssl-redirect"
)

type PolicyDto struct {
	apipolicy.BaseDto
	ReqBuffer         bool  `json:"reqBuffer"`
	RespBuffer        bool  `json:"respBuffer"`
	ClientReqLimit    int64 `json:"clientReqLimit"`
	ClientReqTimeout  int64 `json:"clientReqTimeout"`
	ClientRespTimeout int64 `json:"clientRespTimeout"`
	ProxyReqTimeout   int64 `json:"proxyReqTimeout"`
	ProxyRespTimeout  int64 `json:"proxyRespTimeout"`
	HostPassthrough   bool  `json:"hostPassthrough"`
	SSLRedirect       bool  `json:"sslRedirect"`
}

func (dto PolicyDto) IsValidDto() (bool, string) {
	if !dto.Switch {
		return true, ""
	}
	if dto.ClientReqLimit <= 0 {
		return false, "客户端请求限制需要大于0"
	}
	if dto.ClientReqTimeout <= 0 {
		return false, "客户端请求超时需要大于0"
	}
	if dto.ClientRespTimeout <= 0 {
		return false, "客户端应答超时需要大于0"
	}
	if dto.ProxyReqTimeout <= 0 {
		return false, "后端请求超时需要大于0"
	}
	if dto.ProxyRespTimeout <= 0 {
		return false, "后端应答超时需要大于0"
	}
	return true, ""
}
