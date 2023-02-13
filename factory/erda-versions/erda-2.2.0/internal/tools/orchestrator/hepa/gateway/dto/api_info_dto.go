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

	"github.com/erda-project/erda-proto-go/core/hepa/api/pb"
)

const (
	RT_AUTO_REGISTER = "register"
	RT_AUTO          = "auto"
	RT_MANUAL        = "manual"
	NT_IN            = "inner"
	NT_OUT           = "outer"
	ST_UP            = "asc"
	ST_DOWN          = "desc"
)

type GetApisDto struct {
	From             string
	Method           string
	ConsumerId       string
	RuntimeId        string
	RuntimeServiceId string
	DiceApp          string
	DiceService      string
	ApiPath          string
	RegisterType     string
	NetType          string
	NeedAuth         int
	SortField        string
	SortType         string
	OrgId            string
	ProjectId        string
	Env              string
	Size             int64
	Page             int64
}

type ApiInfoDto struct {
	ApiId string `json:"apiId"`
	// 列表中展示时使用此字段
	Path string `json:"path"`
	// API编辑时用于展现
	DisplayPath string `json:"displayPath"`
	// 若有此字段，API编辑时展现前缀
	DisplayPathPrefix string      `json:"displayPathPrefix,omitempty"`
	OuterNetEnable    bool        `json:"outerNetEnable"`
	RegisterType      string      `json:"registerType"`
	NeedAuth          bool        `json:"needAuth"`
	Method            string      `json:"method,omitempty"`
	Description       string      `json:"description"`
	RedirectAddr      string      `json:"redirectAddr"`
	RedirectPath      string      `json:"redirectPath"`
	RedirectType      string      `json:"redirectType"`
	MonitorPath       string      `json:"monitorPath"`
	Group             GroupDto    `json:"group"`
	CreateAt          string      `json:"createAt"`
	Policies          []PolicyDto `json:"policies"`
	Swagger           interface{} `json:"swagger,omitempty"`
}

func MakePolicies(dtos []PolicyDto) (res []*pb.Policy) {
	for _, dto := range dtos {
		policy := &pb.Policy{
			Category:    dto.Category,
			PolicyId:    dto.PolicyId,
			PolicyName:  dto.PolicyName,
			DisplayName: dto.DisplayName,
			CreateAt:    dto.CreateAt,
		}
		config := map[string]*structpb.Value{}
		for key, value := range dto.Config {
			v, _ := structpb.NewValue(value)
			config[key] = v
		}
		policy.Config = config
		res = append(res, policy)
	}
	return
}
