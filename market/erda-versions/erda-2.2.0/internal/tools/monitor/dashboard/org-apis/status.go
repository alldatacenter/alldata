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

package orgapis

import (
	"net/http"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/providers/i18n"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

type statusQuery struct {
	ClusterName string `query:"clusterName" validate:"required"`
}

type StatusResp struct {
	*statusDTO
	Components map[string]*statusDTO `json:"components,omitempty"`
}

type statusDTO struct {
	Name        string `json:"name" mapstructure:"component_name"`
	DisplayName string `json:"displayName"`
	Status      uint8  `json:"status" mapstructure:"status"`
}

func (p *provider) clusterStatus(params *statusQuery, r *http.Request) interface{} {
	componentStatus, err := p.getComponentStatus(params.ClusterName)
	if err != nil {
		return api.Errors.Internal(err)
	}

	res := p.createStatusResp(componentStatus)
	p.translateStatus(res, api.Language(r))
	return api.Success(res)
}

func (p *provider) translateStatus(resp *StatusResp, lang i18n.LanguageCodes) {
	resp.DisplayName = p.t.Text(lang, resp.Name)
	for _, item := range resp.Components {
		item.DisplayName = p.t.Text(lang, item.Name)
	}
}

func (p *provider) getComponentStatus(clusterName string) (statuses []*statusDTO, err error) {
	componentStatuses, err := p.service.queryStatus(clusterName)
	if err != nil {
		return nil, errors.Wrap(err, "query failed")
	}
	return componentStatuses, nil
}

func (p *provider) createStatusResp(componentStatus []*statusDTO) *StatusResp {
	components := map[string]*statusDTO{
		"machine":        {Name: "machine", Status: 0},
		"kubernetes":     {Name: "kubernetes", Status: 0},
		"dice_addon":     {Name: "dice_addon", Status: 0},
		"dice_component": {Name: "dice_component", Status: 0},
	}
	var maxStatus uint8

	for _, item := range componentStatus {
		v, ok := components[item.Name]
		if ok {
			v.Status = item.Status
			if v.Status > maxStatus {
				maxStatus = v.Status
			}
		}
	}
	return &StatusResp{
		statusDTO: &statusDTO{
			Name:   "cluster_status",
			Status: maxStatus,
		},
		Components: components,
	}
}
