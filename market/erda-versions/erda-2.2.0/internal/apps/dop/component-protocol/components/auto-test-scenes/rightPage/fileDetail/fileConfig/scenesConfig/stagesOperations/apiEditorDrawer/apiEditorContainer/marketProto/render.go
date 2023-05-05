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

package marketProto

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/apiEditorDrawer/apiEditorContainer/apiEditor"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type MarketProto struct {
	bdl *bundle.Bundle
	sdk *cptype.SDK
}

// OperationData 解析OperationData
type OperationData struct {
	Meta struct {
		KeyWord   string `json:"keyWord"`
		APISpecID uint64 `json:"selectApiSpecId"`
	} `json:"meta"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "marketProto",
		func() servicehub.Provider { return &MarketProto{} })
}

func (mp *MarketProto) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	// 塞市场接口的数据
	tmpStepID, ok := c.State["stepId"]
	if !ok {
		// not exhibition if no stepId
		return nil
	}
	// TODO: 类型要改
	stepID, err := strconv.ParseUint(fmt.Sprintf("%v", tmpStepID), 10, 64)
	if err != nil {
		return err
	}
	if stepID == 0 {
		return nil
	}

	mp.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	mp.sdk = cputil.SDK(ctx)
	userID := mp.sdk.Identity.UserID
	orgIDStr := mp.sdk.Identity.OrgID
	if orgIDStr == "" {
		return errors.New("orgID is empty")
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return err
	}
	// var orgID uint64 = 1

	var opreationData OperationData
	odBytes, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(odBytes, &opreationData); err != nil {
		return err
	}

	if c.Data == nil {
		c.Data = make(map[string]interface{}, 0)
	}

	c.State["apiSpecId"] = nil

	switch event.Operation.String() {
	case "searchAPISpec":
		if opreationData.Meta.KeyWord == "" {
			return nil
		}
		apis, err := mp.bdl.SearchAPIOperations(orgID, userID, opreationData.Meta.KeyWord)
		if err != nil {
			return err
		}
		c.Data["list"] = apis
		return nil
	case "changeAPISpec":
		if opreationData.Meta.APISpecID == 0 {
			// 切换成自定义的api
			c.Data["list"] = []apistructs.APIOperationSummary{}
			if _, ok := c.State["value"]; ok {
				delete(c.State, "value")
			}
			c.State["apiSpecId"] = apiEditor.EmptySpecID
			return nil
		}
		// 切换到api集市另一个api
		apiSpec, err := mp.bdl.GetAPIOperation(orgID, userID, opreationData.Meta.APISpecID)
		if err != nil {
			return err
		}
		c.Data["list"] = []apistructs.APIOperationSummary{
			{
				ID:          opreationData.Meta.APISpecID,
				OperationID: apiSpec.OperationID,
				Path:        apiSpec.Path,
				Version:     apiSpec.Version,
				Method:      apiSpec.Method,
			},
		}
		c.State["apiSpecId"] = opreationData.Meta.APISpecID
		c.State["value"] = opreationData.Meta.APISpecID
		return nil
	}

	// 渲染
	step, err := mp.bdl.GetAutoTestSceneStep(apistructs.AutotestGetSceneStepReq{
		ID:     stepID,
		UserID: userID,
	})
	if err != nil {
		return err
	}
	var apiInfo apistructs.APIInfo
	if step.Value == "" {
		step.Value = "{}"
	}
	if err := json.Unmarshal([]byte(step.Value), &apiInfo); err != nil {
		return err
	}
	if step.APISpecID != 0 {
		apiSpec, err := mp.bdl.GetAPIOperation(orgID, userID, step.APISpecID)
		if err != nil {
			return err
		}
		c.Data["list"] = []apistructs.APIOperationSummary{
			{
				ID:          step.APISpecID,
				OperationID: apiSpec.OperationID,
				Path:        apiSpec.Path,
				Version:     apiSpec.Version,
				Method:      apiSpec.Method,
			},
		}
		c.State["value"] = step.APISpecID
	} else {
		if _, ok := c.State["value"]; ok {
			delete(c.State, "value")
		}
	}

	return nil
}
