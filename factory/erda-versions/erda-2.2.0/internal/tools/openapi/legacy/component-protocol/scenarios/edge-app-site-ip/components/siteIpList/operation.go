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

package siteiplist

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

const (
	DefaultSelected = "success"
)

type ComponentList struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

type EdgeAppSiteIPInParam struct {
	ID       int64  `json:"id"`
	AppName  string `json:"appName"`
	SiteName string `json:"siteName"`
}

type EdgeAppSiteIPState struct {
	apistructs.EdgeViewGroupSelectState
	apistructs.EdgePageState
}

func (c *ComponentList) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentList) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

// OperateChangePage
func (c *ComponentList) OperateChangePage(orgID int64, identity apistructs.Identity) (err error) {
	var (
		reqPageNo   = apistructs.EdgeDefaultPageNo
		reqPageSize = apistructs.EdgeDefaultPageSize
		selectScope = DefaultSelected
		inParam     = EdgeAppSiteIPInParam{}
		stateEntity = EdgeAppSiteIPState{}
	)
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	jsonData, err := json.Marshal(c.ctxBundle.InParams)
	if err != nil {
		return fmt.Errorf("marshal id from inparams error: %v", err)
	}

	if err = json.Unmarshal(jsonData, &inParam); err != nil {
		return fmt.Errorf("unmarshal inparam to object error: %v", err)
	}

	jsonData, err = json.Marshal(c.component.State)
	if err != nil {
		return err
	}

	if err = json.Unmarshal(jsonData, &stateEntity); err != nil {
		return err
	} else {
		if stateEntity.PageSize != 0 && stateEntity.PageNo != 0 {
			reqPageSize = stateEntity.PageSize
			reqPageNo = stateEntity.PageNo
		}
		if stateEntity.ViewGroupSelected != "" {
			selectScope = stateEntity.ViewGroupSelected
		}
	}

	resList := make([]EdgeSiteMachineItem, 0)

	res, err := c.ctxBundle.Bdl.GetEdgeInstanceInfo(orgID, inParam.AppName, inParam.SiteName, identity)
	if err != nil {
		return err
	}

	for index, data := range res {
		item := EdgeSiteMachineItem{
			ID:        int64(index),
			IP:        data.ContainerIP,
			Address:   data.HostIP,
			CreatedAt: data.StartedAt.Format("2006-01-02 15:04:05"),
			Status:    getStatus(data.Phase),
			Operate:   getItemOperations(data.ContainerID, data.ContainerIP, data.Cluster, i18nLocale),
		}

		switch selectScope {
		case "success":
			if GetEdgeApplicationContainerStatus(data.Phase) == "success" {
				resList = append(resList, item)
			}
			break
		case "error":
			if GetEdgeApplicationContainerStatus(data.Phase) == "error" {
				resList = append(resList, item)
			}
			break
		default:
			resList = append(resList, item)
		}
	}

	total := len(resList)

	start := (reqPageNo - 1) * reqPageSize
	end := start + reqPageSize

	if end > len(resList) {
		resList = resList[start:]
	} else {
		resList = resList[start:end]
	}

	c.component.Data = map[string]interface{}{
		"list": resList,
	}

	c.component.State["total"] = total
	c.component.State["pageSize"] = reqPageSize
	c.component.State["pageNo"] = reqPageNo

	return
}

func RenderCreator() protocol.CompRender {
	return &ComponentList{}
}
