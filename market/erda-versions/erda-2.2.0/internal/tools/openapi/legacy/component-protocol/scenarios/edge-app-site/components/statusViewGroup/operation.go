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

package statusviewgroup

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentViewGroup struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

func (c *ComponentViewGroup) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentViewGroup) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

func (c *ComponentViewGroup) OperationChangeViewGroup() error {
	var (
		vgGroup = EdgeViewGroupState{}
	)

	jsonData, err := json.Marshal(c.component.State)
	if err != nil {
		return err
	}

	err = json.Unmarshal(jsonData, &vgGroup)
	if err != nil {
		return err
	}

	if _, ok := c.component.State["value"]; !ok {
		c.component.State["viewGroupSelected"] = "total"
		c.component.State["value"] = "total"
	} else {
		c.component.State["viewGroupSelected"] = vgGroup.Value
		c.component.State["value"] = vgGroup.Value
	}

	return nil
}

func (c *ComponentViewGroup) Operation(identity apistructs.Identity) error {
	var (
		err            error
		successTotal   int
		failureTotal   int
		deployingTotal int
		inParam        = apistructs.EdgeRenderingID{}
		stateEntity    = apistructs.EdgeSearchState{}
	)

	jsonData, err := json.Marshal(c.ctxBundle.InParams)
	if err != nil {
		return fmt.Errorf("marshal id from inparams error: %v", err)
	}

	if err = json.Unmarshal(jsonData, &inParam); err != nil {
		return fmt.Errorf("unmarshal inparam to object error: %v", err)
	}

	res, err := c.ctxBundle.Bdl.GetEdgeAppStatus(&apistructs.EdgeAppStatusListRequest{
		AppID:     inParam.ID,
		NotPaging: true,
	}, identity)

	if err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	jsonData, err = json.Marshal(c.component.State)
	if err != nil {
		return err
	}

	if err = json.Unmarshal(jsonData, &stateEntity); err != nil {
		return err
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

	for _, data := range appSiteStatus {
		if data.STATUS == "deploying" {
			deployingTotal++
		} else if data.STATUS == "succeed" {
			successTotal++
		} else {
			failureTotal++
		}
	}

	c.component.Props = getProps(len(appSiteStatus), successTotal, deployingTotal, failureTotal, i18nLocale)

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentViewGroup{}
}
