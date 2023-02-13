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

package siteformmodal

import (
	"encoding/json"
	"fmt"
	"regexp"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	SiteNameLength = 30
)

var (
	SiteNameReservedWordMap = map[string]bool{
		"public": true,
	}
)

type ComponentFormModal struct {
	ctxBundle protocol.ContextBundle
	component *apistructs.Component
}

func (c *ComponentFormModal) SetBundle(ctxBundle protocol.ContextBundle) error {
	if ctxBundle.Bdl == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.ctxBundle = ctxBundle
	return nil
}

func (c *ComponentFormModal) SetComponent(component *apistructs.Component) error {
	if component == nil {
		return fmt.Errorf("invalie bundle")
	}
	c.component = component
	return nil
}

func (c *ComponentFormModal) OperateRendering(orgID int64, identity apistructs.Identity) error {
	var (
		siteState = apistructs.EdgeSiteState{}
	)
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	jsonData, err := json.Marshal(c.component.State)
	if err != nil {
		return err
	}

	err = json.Unmarshal(jsonData, &siteState)
	if err != nil {
		return err
	}

	if siteState.FormClear {
		c.component.State["formData"] = map[string]interface{}{}
		edgeClusters, err := c.ctxBundle.Bdl.ListEdgeCluster(uint64(orgID), apistructs.EdgeListValueTypeID, identity)
		if err != nil {
			return fmt.Errorf("get avaliable edge clusters error: %v", err)
		}
		c.component.Props = getProps(edgeClusters, false, i18nLocale)
	} else if siteState.SiteID != 0 {
		siteInfo, err := c.ctxBundle.Bdl.GetEdgeSite(siteState.SiteID, identity)
		if err != nil {
			return err
		}
		c.component.Props = getProps(nil, true, i18nLocale)
		c.component.State["formData"] = map[string]interface{}{
			"id":             siteInfo.ID,
			"siteName":       siteInfo.Name,
			"relatedCluster": siteInfo.ClusterName,
			"desc":           siteInfo.Description,
		}
	}

	return nil
}

func (c *ComponentFormModal) OperateSubmit(orgID int64, identity apistructs.Identity) error {
	var (
		err           error
		isUpdate      bool
		formDataJson  []byte
		createRequest SiteFormCreate
		baseRequest   SiteFormSubmitBase
	)

	if data, ok := c.component.State["formData"]; ok {
		formDataJson, err = json.Marshal(data)
		if err != nil {
			return err
		}

		if formData, ok := data.(map[string]interface{}); !ok {
			return fmt.Errorf("request form data assert error")
		} else {
			if _, ok := formData["id"]; ok {
				isUpdate = true
			}
		}
	} else {
		return fmt.Errorf("must provide formdata")
	}

	if isUpdate {
		err = json.Unmarshal(formDataJson, &baseRequest)
		if err != nil {
			return err
		}

		if err = validateSubmitData(baseRequest.SiteName, baseRequest.Desc); err != nil {
			return err
		}

		if err = c.ctxBundle.Bdl.UpdateEdgeSite(
			&apistructs.EdgeSiteUpdateRequest{
				Description: baseRequest.Desc,
			},
			baseRequest.ID,
			identity,
		); err != nil {
			return err
		}
	} else {
		err = json.Unmarshal(formDataJson, &createRequest)
		if err != nil {
			return err
		}

		if err = validateSubmitData(createRequest.SiteName, createRequest.Desc); err != nil {
			return err
		}

		if _, ok := SiteNameReservedWordMap[createRequest.SiteName]; ok {
			return fmt.Errorf("name %s is forbidden, choose a new one please", createRequest.SiteName)
		}

		if err = c.ctxBundle.Bdl.CreateEdgeSite(
			&apistructs.EdgeSiteCreateRequest{
				OrgID:       orgID,
				Name:        createRequest.SiteName,
				ClusterID:   createRequest.RelatedCluster,
				Description: createRequest.Desc,
			},
			identity,
		); err != nil {
			return err
		}
	}

	return nil
}

func validateSubmitData(siteName, desc string) error {
	if err := strutil.Validate(siteName, strutil.MaxRuneCountValidator(SiteNameLength)); err != nil {
		return err
	}

	if err := strutil.Validate(desc, strutil.MaxRuneCountValidator(apistructs.EdgeDefaultLagerLength/2)); err != nil {
		return err
	}

	isRight, err := regexp.MatchString(SiteNameMatchPattern, siteName)
	if err != nil {
		return err
	}
	if !isRight {
		return fmt.Errorf(apistructs.EdgeDefaultRegexpError)
	}

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentFormModal{}
}
