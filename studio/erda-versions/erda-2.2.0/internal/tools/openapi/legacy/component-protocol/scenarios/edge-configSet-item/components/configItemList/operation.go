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

package configitemlist

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentConfigItemList struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

type EdgeCfgItemListState struct {
	IsFirstFilter bool `json:"isFirstFilter"`
	apistructs.EdgePageState
	apistructs.EdgeSearchState
}

func (c *ComponentConfigItemList) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentConfigItemList) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

// OperateChangePage
func (c *ComponentConfigItemList) OperateChangePage(reList bool, identity apistructs.Identity) (err error) {
	var (
		reqPageNo         = apistructs.EdgeDefaultPageNo
		reqPageSize       = apistructs.EdgeDefaultPageSize
		inParam           = apistructs.EdgeRenderingID{}
		searchStateObject = EdgeCfgItemListState{}
		timeFormatLayout  = "2006-01-02 15:04:05"
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

	if err = json.Unmarshal(jsonData, &searchStateObject); err != nil {
		return err
	}
	if searchStateObject.IsFirstFilter {
		reqPageNo = 1
	} else if !searchStateObject.IsFirstFilter && searchStateObject.PageNo != 0 {
		reqPageNo = searchStateObject.PageNo
	}

	if searchStateObject.PageSize != 0 {
		reqPageSize = searchStateObject.PageSize
	}

	if reList {
		totalItemReq := &apistructs.EdgeCfgSetItemListPageRequest{
			ConfigSetID: inParam.ID,
			NotPaging:   true,
		}

		if searchStateObject.SearchCondition != "" {
			totalItemReq.Search = searchStateObject.SearchCondition
		}

		cfgSetItems, err := c.ctxBundle.Bdl.ListEdgeCfgSetItem(totalItemReq, identity)

		if err != nil {
			return fmt.Errorf("count configSet item error: %v, configset id: %d", err, inParam.ID)
		}

		if cfgSetItems.Total <= (reqPageNo-1)*reqPageSize && reqPageNo-1 > 0 {
			reqPageNo--
		}
	}

	req := &apistructs.EdgeCfgSetItemListPageRequest{
		ConfigSetID: inParam.ID,
		PageNo:      reqPageNo,
		PageSize:    reqPageSize,
		Search:      searchStateObject.SearchCondition,
	}

	res, err := c.ctxBundle.Bdl.ListEdgeCfgSetItem(req, identity)

	if err != nil {
		return fmt.Errorf("list edge cofnigset item error:%v", err)
	}

	resList := make([]EdgeConfigItem, 0)

	for _, data := range res.List {
		item := EdgeConfigItem{
			ID:          data.ID,
			ConfigName:  data.ItemKey,
			ConfigValue: data.ItemValue,
			SiteName:    data.SiteName,
			CreateTime:  data.CreatedAt.Format(timeFormatLayout),
			UpdateTime:  data.UpdatedAt.Format(timeFormatLayout),
			Operate:     getConfigsetItem(data, i18nLocale),
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

func (c *ComponentConfigItemList) OperateDelete(operationData interface{}, identity apistructs.Identity) (err error) {
	var (
		meta apistructs.EdgeEventMeta
	)

	jsonData, err := json.Marshal(operationData)
	if err != nil {
		return err
	}

	err = json.Unmarshal(jsonData, &meta)
	if err != nil {
		return err
	}

	err = c.ctxBundle.Bdl.DeleteEdgeCfgSetItem(meta.Meta["id"], identity)
	if err != nil {
		return err
	}

	err = c.OperateChangePage(true, identity)
	if err != nil {
		return err
	}

	return
}

func (c *ComponentConfigItemList) OperateReload(operationData map[string]interface{}) (err error) {
	var (
		meta = apistructs.EdgeEventMeta{}
	)

	jsonData, err := json.Marshal(operationData)
	if err != nil {
		return fmt.Errorf("marshal operation data error: %v", err)
	}

	err = json.Unmarshal(jsonData, &meta)
	if err != nil {
		return fmt.Errorf("unmarshal operation data error: %v", err)
	}

	c.component.State["configItemFormModalVisible"] = true
	c.component.State["configSetItemID"] = meta.Meta["id"]

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentConfigItemList{}
}
