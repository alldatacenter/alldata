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

package appsitemanage

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-app-site/i18n"
)

type ComponentList struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

type EdgeAppSiteInParam struct {
	ID      int64  `json:"id"`
	AppName string `json:"appName"`
}

type EdgeAppSiteMeta struct {
	AppID    uint64 `json:"appID"`
	SiteName string `json:"siteName"`
}

type EdgeAppSiteState struct {
	apistructs.EdgeViewGroupSelectState
	apistructs.EdgeSearchState
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
func (c *ComponentList) OperateChangePage(isRestart bool, restartSiteName string, identity apistructs.Identity) (err error) {
	var (
		inParam     = EdgeAppSiteInParam{}
		stateEntity = EdgeAppSiteState{}
		resList     = make([]EdgeAppDetailItem, 0)
		// TODO: change sting to const
		selectScope = "total"
		reqPageNo   = apistructs.EdgeDefaultPageNo
		reqPageSize = apistructs.EdgeDefaultPageSize
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

	// TODO: 分页大数据存在问题

	res, err := c.ctxBundle.Bdl.GetEdgeAppStatus(&apistructs.EdgeAppStatusListRequest{
		AppID:     inParam.ID,
		NotPaging: true,
	}, identity)

	if err != nil {
		return fmt.Errorf("list edge site error: %v", err)
	}

	appSiteStatus := make([]apistructs.EdgeAppSiteStatus, 0)

	if stateEntity.SearchCondition != "" {
		for _, site := range res.SiteList {
			if strings.Contains(site.SITE, stateEntity.SearchCondition) {
				appSiteStatus = append(appSiteStatus, site)
			}
		}
	} else {
		appSiteStatus = res.SiteList
	}

	for index, data := range appSiteStatus {
		isAllOperate := false
		item := EdgeAppDetailItem{
			ID:       index,
			SiteName: renderSiteName(inParam.ID, data.SITE, inParam.AppName),
			DeployStatus: apistructs.EdgeTextBadge{
				RenderType: "textWithBadge",
			},
		}

		if isRestart && data.SITE == restartSiteName {
			data.STATUS = "deploying"
		}

		if data.STATUS == "deploying" {
			item.DeployStatus.Status = "processing"
			item.DeployStatus.Value = i18nLocale.Get(i18n.I18nKeyDeploying)
		} else if data.STATUS == "succeed" {
			item.DeployStatus.Status = "success"
			item.DeployStatus.Value = i18nLocale.Get(i18n.I18nKeySuccess)
			isAllOperate = true
		} else {
			item.DeployStatus.Status = "error"
			item.DeployStatus.Value = i18nLocale.Get(i18n.I18nKeyFailed)
		}

		item.Operate = getSiteItemOperate(inParam, data.SITE, isAllOperate, i18nLocale)

		switch selectScope {
		case "success":
			if data.STATUS == "succeed" {
				resList = append(resList, item)
			}
			break
		case "processing":
			if data.STATUS == "deploying" {
				resList = append(resList, item)
			}
			break
		case "error":
			if item.DeployStatus.Status == "error" {
				resList = append(resList, item)
			}
			break
		default:
			resList = append(resList, item)
		}
	}

	total := len(resList)

	if total <= (reqPageNo-1)*reqPageSize && reqPageNo-1 > 0 {
		reqPageNo--
	}

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

func (c *ComponentList) OperateOffline(operationData map[string]interface{}, identity apistructs.Identity) (err error) {
	var (
		meta = EdgeAppSiteMeta{}
	)

	jsonData, err := json.Marshal(operationData["meta"])
	if err != nil {
		return err
	}

	err = json.Unmarshal(jsonData, &meta)
	if err != nil {
		return err
	}

	if err = c.ctxBundle.Bdl.OfflineEdgeAppSite(meta.AppID, &apistructs.EdgeAppSiteRequest{
		SiteName: meta.SiteName,
	}, identity); err != nil {
		return err
	}

	err = c.OperateChangePage(false, "", identity)
	if err != nil {
		return err
	}

	return
}

func (c *ComponentList) OperateRestart(operationData map[string]interface{}, identity apistructs.Identity) (err error) {
	var (
		meta = EdgeAppSiteMeta{}
	)

	jsonData, err := json.Marshal(operationData["meta"])
	if err != nil {
		return err
	}

	err = json.Unmarshal(jsonData, &meta)
	if err != nil {
		return err
	}

	if err = c.ctxBundle.Bdl.RestartEdgeAppSite(meta.AppID, &apistructs.EdgeAppSiteRequest{
		SiteName: meta.SiteName,
	}, identity); err != nil {
		return err
	}

	err = c.OperateChangePage(true, meta.SiteName, identity)
	if err != nil {
		return err
	}

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentList{}
}
