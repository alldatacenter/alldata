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

package sitelist

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentList struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

type EdgeSiteState struct {
	apistructs.EdgeSiteState
	IsFirstFilter bool `json:"isFirstFilter"`
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
func (c *ComponentList) OperateChangePage(orgID int64, reList bool, identity apistructs.Identity) (err error) {
	var (
		reqPageNo   = apistructs.EdgeDefaultPageNo
		reqPageSize = apistructs.EdgeDefaultPageSize
		stateEntity = EdgeSiteState{}
	)
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	jsonData, err := json.Marshal(c.component.State)
	if err != nil {
		return err
	}

	if err = json.Unmarshal(jsonData, &stateEntity); err != nil {
		return err
	}

	if stateEntity.IsFirstFilter {
		reqPageNo = 1
	} else if !stateEntity.IsFirstFilter && stateEntity.PageNo != 0 {
		reqPageNo = stateEntity.PageNo
	}

	if stateEntity.PageSize != 0 {
		reqPageSize = stateEntity.PageSize
	}

	if reList {
		totalListReq := &apistructs.EdgeSiteListPageRequest{
			OrgID:     orgID,
			NotPaging: true,
		}

		if stateEntity.SearchCondition != "" {
			totalListReq.Search = stateEntity.SearchCondition
		}

		allSite, err := c.ctxBundle.Bdl.ListEdgeSite(totalListReq, identity)

		if err != nil {
			return err
		}

		if allSite.Total <= (reqPageNo-1)*reqPageSize && reqPageNo-1 > 0 {
			reqPageNo--
		}
	}

	req := &apistructs.EdgeSiteListPageRequest{
		OrgID:    orgID,
		PageNo:   reqPageNo,
		PageSize: reqPageSize,
		Search:   stateEntity.SearchCondition,
	}

	res, err := c.ctxBundle.Bdl.ListEdgeSite(req, identity)

	if err != nil {
		return fmt.Errorf("list edge site error: %v", err)
	}

	resList := make([]EdgeSiteItem, 0)

	for _, data := range res.List {
		item := EdgeSiteItem{
			ID:             data.ID,
			NodeNum:        data.NodeCount,
			SiteName:       renderSiteName(data.ClusterName, data.Name, data.ID),
			RelatedCluster: data.ClusterName,
			Operate:        getSiteItemOperate(data, i18nLocale),
		}

		resList = append(resList, item)
	}

	c.component.Data = map[string]interface{}{
		"list": resList,
	}

	c.component.State["total"] = res.Total
	c.component.State["pageSize"] = reqPageSize
	c.component.State["pageNo"] = reqPageNo
	c.component.State["isFirstFilter"] = false

	return
}

func (c *ComponentList) OperateDelete(orgID int64, operationData interface{}, identity apistructs.Identity) (err error) {
	var (
		meta = apistructs.EdgeEventMeta{}
	)

	jsonData, err := json.Marshal(operationData)
	if err != nil {
		return err
	}

	err = json.Unmarshal(jsonData, &meta)
	if err != nil {
		return err
	}

	err = c.ctxBundle.Bdl.DeleteEdgeSite(meta.Meta["id"], identity)
	if err != nil {
		return err
	}

	err = c.OperateChangePage(orgID, true, identity)
	if err != nil {
		return err
	}

	return
}

func (c *ComponentList) OperateReload(operationData interface{}, operation string) (err error) {
	var (
		meta = apistructs.EdgeEventMeta{}
	)

	jsonData, err := json.Marshal(operationData)
	if err != nil {
		return err
	}

	err = json.Unmarshal(jsonData, &meta)
	if err != nil {
		return err
	}

	if operation == apistructs.EdgeOperationAdd {
		c.component.State["sitePreviewVisible"] = true
		c.component.State["siteAddDrawerVisible"] = true
	} else if operation == apistructs.EdgeOperationUpdate {
		c.component.State["siteFormModalVisible"] = true
	}

	c.component.State["siteID"] = meta.Meta["id"]

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentList{}
}
