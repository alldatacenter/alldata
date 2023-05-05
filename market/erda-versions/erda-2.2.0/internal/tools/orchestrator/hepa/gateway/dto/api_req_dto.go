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

import "github.com/erda-project/erda-proto-go/core/hepa/api/pb"

var INNER_HOSTS = []string{
	"dev-api-gateway.kube-system.svc.cluster.local",
	"test-api-gateway.kube-system.svc.cluster.local",
	"staging-api-gateway.kube-system.svc.cluster.local",
	"api-gateway.kube-system.svc.cluster.local",
}

type ApiDto struct {
	// 路径
	Path string `json:"path"`
	// 方法
	Method string `json:"method"`
	// 转发方式
	RedirectType string `json:"redirectType"`
	// 转发地址
	RedirectAddr string `json:"redirectAddr"`
	// 转发地址
	RedirectPath string `json:"redirectPath"`
	// 域名
	Hosts []string `json:"hosts"`
	//项目名称
	ProjectId string `json:"projectId"`
	// 描述
	Description string `json:"description"`
	// 应用名
	DiceApp string `json:"diceApp"`
	// 服务名
	DiceService string `json:"diceService"`
	// 是否开启公网访问
	OuterNetEnable bool `json:"outerNetEnable"`
	// 分支id
	RuntimeId string `json:"runtimeId"`
	//环境
	Env              string      `json:"env"`
	RegisterType     string      `json:"-"`
	NeedAuth         int         `json:"-"`
	UpstreamApiId    string      `json:"-"`
	RuntimeServiceId string      `json:"-"`
	IsInner          int         `json:"-"`
	Swagger          interface{} `json:"-"`
	DaoId            string      `json:"-"`
}

type ApiReqOptionDto struct {
	// 插件信息
	Policies []string `json:"policies"`
	// consumerId
	ConsumerId string `json:"consumerId"`
}

type ApiReqDto struct {
	*ApiDto
	*ApiReqOptionDto
}

func (dto *ApiReqDto) IsEmpty() bool {
	if (len(dto.Path) == 0 && len(dto.Method) == 0) || len(dto.Env) == 0 || len(dto.ProjectId) == 0 {
		return true
	}
	if dto.RedirectPath == "" {
		dto.RedirectPath = dto.Path
	}
	switch dto.RedirectType {
	case RT_URL:
		return dto.RedirectAddr == ""
	case RT_SERVICE:
		return false
	default:
		return true
	}
}

func (dto *ApiReqDto) AddHost(host string) {
	dto.Hosts = append(dto.Hosts, host)
}

func MakeApiDto(req *pb.ApiRequest) *ApiDto {
	return &ApiDto{
		Path:           req.Path,
		Method:         req.Method,
		RedirectType:   req.RedirectType,
		RedirectAddr:   req.RedirectAddr,
		RedirectPath:   req.RedirectPath,
		Hosts:          req.Hosts,
		ProjectId:      req.ProjectId,
		Description:    req.Description,
		DiceApp:        req.DiceApp,
		DiceService:    req.DiceService,
		OuterNetEnable: req.OuterNetEnable,
		RuntimeId:      req.RuntimeId,
		Env:            req.Env,
	}
}
