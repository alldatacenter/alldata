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

package configsetlist

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentConfigsetList struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

func (c *ComponentConfigsetList) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentConfigsetList) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

// OperateChangePage
func (c *ComponentConfigsetList) OperateChangePage(orgID int64, reList bool, identity apistructs.Identity) (err error) {
	var (
		reqPageNo   = apistructs.EdgeDefaultPageNo
		reqPageSize = apistructs.EdgeDefaultPageSize
		cfgSetState apistructs.EdgePageState
	)
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	jsonData, err := json.Marshal(c.component.State)
	if err != nil {
		return err
	}

	err = json.Unmarshal(jsonData, &cfgSetState)
	if err != nil {
		return err
	}

	if cfgSetState.PageNo > 0 && cfgSetState.PageSize > 0 {
		reqPageNo = cfgSetState.PageNo
		reqPageSize = cfgSetState.PageSize
	}

	if reList {
		getALl, err := c.ctxBundle.Bdl.ListEdgeConfigset(&apistructs.EdgeConfigSetListPageRequest{
			OrgID:     orgID,
			NotPaging: true,
		}, identity)

		if err != nil {
			return err
		}

		if getALl.Total <= (reqPageNo-1)*reqPageSize && reqPageNo-1 > 0 {
			reqPageNo--
		}
	}

	req := &apistructs.EdgeConfigSetListPageRequest{
		OrgID:    orgID,
		PageNo:   reqPageNo,
		PageSize: reqPageSize,
	}

	res, err := c.ctxBundle.Bdl.ListEdgeConfigset(req, identity)

	if err != nil {
		return err
	}

	resList := make([]EdgeConfigsetItem, 0)

	for _, data := range res.List {
		item := EdgeConfigsetItem{
			ConfigsetName:  data.Name,
			RelatedCluster: data.ClusterName,
			Operate:        getConfigsetItem(data.ID, data.Name, i18nLocale),
		}

		resList = append(resList, item)
	}

	c.component.Data = map[string]interface{}{
		"list": resList,
	}

	c.component.State["total"] = res.Total
	c.component.State["pageSize"] = reqPageSize
	c.component.State["pageNo"] = reqPageNo

	return
}

func (c *ComponentConfigsetList) OperateDelete(orgID int64, operationData interface{}, identity apistructs.Identity) (err error) {
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

	err = c.ctxBundle.Bdl.DeleteEdgeConfigset(meta.Meta["id"], identity)
	if err != nil {
		return err
	}

	err = c.OperateChangePage(orgID, true, identity)
	if err != nil {
		return err
	}

	return
}

func RenderCreator() protocol.CompRender {
	return &ComponentConfigsetList{}
}
