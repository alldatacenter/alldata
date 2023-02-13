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

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	siteiplist "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-app-site-ip/components/siteIpList"
)

const (
	DefaultSelected = "success"
)

type ComponentViewGroup struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

type EdgeAppSiteIPInParam struct {
	ID       int64  `json:"id"`
	AppName  string `json:"appName"`
	SiteName string `json:"siteName"`
}

type EdgeViewGroupState struct {
	Value string `json:"value"`
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
		c.component.State["viewGroupSelected"] = DefaultSelected
		c.component.State["value"] = DefaultSelected
	} else {
		c.component.State["viewGroupSelected"] = vgGroup.Value
		c.component.State["value"] = vgGroup.Value
	}

	return nil
}

func (c *ComponentViewGroup) Operation(orgID int64, identity apistructs.Identity) error {
	var (
		err          error
		successTotal int
		failureTotal int
		inParam      = EdgeAppSiteIPInParam{}
	)
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	jsonData, err := json.Marshal(c.ctxBundle.InParams)
	if err != nil {
		return fmt.Errorf("marshal id from inparams error: %v", err)
	}

	if err = json.Unmarshal(jsonData, &inParam); err != nil {
		return fmt.Errorf("unmarshal inparam to object error: %v", err)
	}

	res, err := c.ctxBundle.Bdl.GetEdgeInstanceInfo(orgID, inParam.AppName, inParam.SiteName, identity)
	if err != nil {
		return err
	}

	for _, data := range res {
		if siteiplist.GetEdgeApplicationContainerStatus(data.Phase) == "success" {
			successTotal++
		} else {
			failureTotal++
		}
	}

	c.component.Props = getProps(len(res), successTotal, failureTotal, i18nLocale)

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentViewGroup{}
}
