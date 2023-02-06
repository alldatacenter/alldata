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

package appconfigform

import (
	"context"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-application/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

const (
	MIRROR     = "MIRROR"
	MIDDLEWARE = "MIDDLEWARE"

	AppNameMatchPattern = "^[a-z][a-z0-9-]*[a-z0-9]$"
)

var (
	AppNameMatchRegexp = fmt.Sprintf("/%v/", AppNameMatchPattern)
)

type Application struct {
	ID                uint64                       `json:"id"`
	AppName           string                       `json:"appName"`
	DeployResource    string                       `json:"deployResource"`
	MiddlewareType    string                       `json:"middlewareType"`
	Cluster           string                       `json:"cluster"`
	Sites             []string                     `json:"sites,omitempty"`
	ConfigSet         string                       `json:"configSets"`
	Depends           []string                     `json:"depends,omitempty"`
	Replicaset        int32                        `json:"copyNum"`
	Storage           ApplicationStorage           `json:"storage"`
	Mirror            ApplicationMirror            `json:"mirror"`
	HealthCheckConfig ApplicationHealthCheckConfig `json:"healthCheckConfig"`
	PortMap           ApplicationPortMap           `json:"portMap"`
}

type ApplicationStorage struct {
	CpuRequest    float64 `json:"cpuRequest"`
	CpuLimit      float64 `json:"cpuLimit"`
	MemoryRequest float64 `json:"memoryRequest"`
	MemoryLimit   float64 `json:"memoryLimit"`
}

type ApplicationMirror struct {
	MirrorAddress    string `json:"mirrorAddress"`
	RegistryUser     string `json:"registryUser"`
	RegistryPassword string `json:"registryPassword"`
}

type ApplicationHealthCheckConfig struct {
	HealthCheckType string `json:"healthCheckType"`
	Path            string `json:"path"`
	Port            int    `json:"port"`
	HealthCheckExec string `json:"HealthCheckExec"`
}

type ApplicationPortMap struct {
	Data []ApplicationPortMapData `json:"data"`
}

type ApplicationPortMapData struct {
	Protocol  string `json:"protocol"`
	Container int    `json:"container"`
	Service   int32  `json:"service"`
}

type ChangeClusterReqForm struct {
	ClusterName string `json:"cluster"`
}

type ChangeClusterReqProps struct {
	Fields []*apistructs.EdgeFormModalField `json:"fields"`
}

func (c ComponentFormModal) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if err := c.SetComponent(component); err != nil {
		return err
	}

	orgID, err := strconv.ParseInt(c.ctxBundle.Identity.OrgID, 10, 64)
	if err != nil {
		return fmt.Errorf("component %s parse org id error: %v", component.Name, err)
	}

	identity := c.ctxBundle.Identity

	if c.component.State == nil {
		c.component.State = make(map[string]interface{})
	}

	switch event.Operation {
	case apistructs.EdgeOperationSubmit:
		if err = c.OperateSubmit(orgID, identity); err != nil {
			return fmt.Errorf("operation submit error: %v", err)
		}
		break
	case apistructs.EdgeOperationCgCluster:
		if err = c.OperateChangeCluster(orgID, identity); err != nil {
			return fmt.Errorf("operation change cluster error: %v", err)
		}
		break
	case apistructs.RenderingOperation:
		if err = c.OperateRendering(orgID, identity); err != nil {
			return fmt.Errorf("rendering operation error: %v", err)
		}
		break
	default:
		c.component.Props = getProps(nil, nil, nil, nil, "", "", i18nLocale)
	}

	c.component.Operations = getOperations()

	return nil
}

func getOperations() apistructs.EdgeOperations {
	return apistructs.EdgeOperations{
		"submit": apistructs.EdgeOperation{
			Key:    "submit",
			Reload: true,
		},
		"cancel": apistructs.EdgeOperation{
			Reload: false,
			Key:    "cancel",
			Command: apistructs.EdgeJumpCommand{
				Key:    "set",
				Target: "addAppDrawer",
				State: apistructs.EdgeJumpCommandState{
					Visible: false,
				},
			},
		},
	}
}

// GenerateOperation:
// - viewDetail->all disabled;
// - update: deployResource:
//           - mirror disabled: appName, deployResource, cluster;
//           - middleware disabled: appName, deployResource, middlewareType, cluster;
// Param: cluster, sites, configSets； select part.
func getProps(clusters, sites, configSets, depends []map[string]interface{}, operation, deployResource string, lr *i18r.LocaleResource) apistructs.EdgeFormModalPointProps {
	formModal := apistructs.EdgeFormModalPointProps{
		Fields:      []*apistructs.EdgeFormModalField{},
		FooterAlign: "right",
	}

	appNameField := apistructs.EdgeFormModalField{
		Key:       "appName",
		Label:     lr.Get(i18n.I18nKeyApplicationName),
		Component: "input",
		Required:  true,
		Rules: []apistructs.EdgeFormModalFieldRule{
			{
				Pattern: AppNameMatchRegexp,
				Message: lr.Get(i18n.I18nKeyInputApplicationNameTip),
			},
		},
		ComponentProps: map[string]interface{}{
			"maxLength": ApplicationNameLength,
		},
	}
	deployResourceField := apistructs.EdgeFormModalField{
		Key:          "deployResource",
		Label:        lr.Get(i18n.I18nKeyDeploySource),
		Component:    "select",
		Required:     true,
		DefaultValue: MIDDLEWARE,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeySelectDeploySource),
			"options": []map[string]interface{}{
				{
					"name":  lr.Get(i18n.I18nKeyImage),
					"value": MIRROR,
				},
				{
					"name":  lr.Get(i18n.I18nKeyAddon),
					"value": MIDDLEWARE,
				},
			},
		},
		Operations: map[string]interface{}{
			"onChange": apistructs.EdgeOperation{
				Key:    "changeDepResource",
				Reload: true,
			},
		},
	}

	middleWareTypeField := apistructs.EdgeFormModalField{
		Key:       "middlewareType",
		Label:     lr.Get(i18n.I18nKeyAddonType),
		Component: "select",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyPleaseSelectAddonType),
			"options": []map[string]interface{}{
				{
					"name":  MysqlAddonName,
					"value": fmt.Sprintf("%s:%s", MysqlAddonName, MysqlAddonVersion),
				},
			},
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "deployResource",
					"operator": "=",
					"value":    "MIRROR",
				},
			},
		},
	}

	clusterField := apistructs.EdgeFormModalField{
		Key:       "cluster",
		Label:     lr.Get(i18n.I18nKeyCluster),
		Component: "select",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyPleaseSelectCluster),
			"options":     clusters,
		},
		Operations: map[string]interface{}{
			"change": apistructs.EdgeOperation{
				Key:    "clusterChange",
				Reload: true,
			},
		},
	}

	sitesField := apistructs.EdgeFormModalField{
		Key:       "sites",
		Label:     lr.Get(i18n.I18nKeySite),
		Component: "select",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeySelectSite),
			"mode":        "multiple",
			"options":     sites,
			"selectAll":   true,
		},
	}
	cfgSets := apistructs.EdgeFormModalField{
		Key:       "configSets",
		Label:     lr.Get(i18n.I18nKeyConfigSet),
		Component: "select",
		Required:  false,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeySelectSite),
			"allowClear":  true,
			"options":     configSets,
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "deployResource",
					"operator": "=",
					"value":    "MIDDLEWARE",
				},
			},
		},
	}
	dependApps := apistructs.EdgeFormModalField{
		Key:       "depends",
		Label:     lr.Get(i18n.I18nKeyDepend),
		Component: "select",
		Required:  false,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeySelectDepend),
			"mode":        "multiple",
			"allowClear":  true,
			"options":     depends,
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "deployResource",
					"operator": "=",
					"value":    "MIDDLEWARE",
				},
			},
		},
	}

	replicaField := apistructs.EdgeFormModalField{
		Key:       "copyNum",
		Label:     lr.Get(i18n.I18nKeyReplica),
		Component: "inputNumber",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyInputReplica),
			"className":   "full-width",
			"precision":   0,
			"min":         0,
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "deployResource",
					"operator": "=",
					"value":    "MIDDLEWARE",
				},
			},
		},
	}

	storageGroup := apistructs.EdgeFormModalField{
		Label:     "",
		Key:       "storage",
		Component: "formGroup",
		Group:     "storage",
		ComponentProps: map[string]interface{}{
			"indentation": true,
			"showDivider": false,
			"title":       lr.Get(i18n.I18nKeyCpuAndMemory),
			"direction":   "row",
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "deployResource",
					"operator": "=",
					"value":    "MIDDLEWARE",
				},
			},
		},
	}
	cpuRequestField := apistructs.EdgeFormModalField{
		Key:       "storage.cpuRequest",
		Group:     "storage",
		Label:     lr.Get(i18n.I18nKeyRequestCpu),
		Component: "inputNumber",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyInputRequestCpu),
			"className":   "full-width",
			"precision":   1,
		},
	}
	cpuLimitField := apistructs.EdgeFormModalField{
		Key:       "storage.cpuLimit",
		Group:     "storage",
		Label:     lr.Get(i18n.I18nKeyCpuLimit),
		Component: "inputNumber",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyInputCpuLimit),
			"className":   "full-width",
			"precision":   1,
		},
	}
	memRequestField := apistructs.EdgeFormModalField{
		Key:       "storage.memoryRequest",
		Label:     lr.Get(i18n.I18nKeyRequestMemory),
		Component: "inputNumber",
		Required:  true,
		Group:     "storage",
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyInputRequestMemory),
			"className":   "full-width",
			"precision":   0,
		},
	}
	memLimitField := apistructs.EdgeFormModalField{
		Key:       "storage.memoryLimit",
		Group:     "storage",
		Label:     lr.Get(i18n.I18nKeyLimitMemory),
		Component: "inputNumber",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyInputLimitMemory),
			"className":   "full-width",
			"precision":   0,
		},
	}

	mirrorGroup := apistructs.EdgeFormModalField{
		Label:     "",
		Key:       "mirror",
		Component: "formGroup",
		Group:     "mirror",
		ComponentProps: map[string]interface{}{
			"indentation": true,
			"title":       lr.Get(i18n.I18nKeyImageSetting),
			"direction":   "row",
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "deployResource",
					"operator": "=",
					"value":    "MIDDLEWARE",
				},
			},
		},
	}
	mirrorAddrField := apistructs.EdgeFormModalField{
		Key:       "mirror.mirrorAddress",
		Group:     "mirror",
		Label:     lr.Get(i18n.I18nKeyImageAddress),
		Component: "input",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"maxLength": apistructs.EdgeDefaultValueMaxLength,
		},
	}
	mirrorUser := apistructs.EdgeFormModalField{
		Key:       "mirror.registryUser",
		Group:     "mirror",
		Label:     lr.Get(i18n.I18nKeyImageRepoUsername),
		Component: "input",
		Required:  false,
		ComponentProps: map[string]interface{}{
			"maxLength": apistructs.EdgeDefaultNameMaxLength,
		},
	}
	mirrorPassword := apistructs.EdgeFormModalField{
		Key:        "mirror.registryPassword",
		Group:      "mirror",
		Label:      lr.Get(i18n.I18nKeyImageRepoPassword),
		Component:  "input",
		Required:   false,
		IsPassword: true,
		ComponentProps: map[string]interface{}{
			"maxLength": apistructs.EdgeDefaultNameMaxLength,
		},
	}

	healthCheckGroup := apistructs.EdgeFormModalField{
		Label:     "",
		Key:       "healthCheckConfig",
		Component: "formGroup",
		Group:     "healthCheckConfig",
		ComponentProps: map[string]interface{}{
			"indentation": true,
			"showDivider": false,
			"direction":   "row",
			"title":       lr.Get(i18n.I18nKeyConfigHealthCheck),
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "deployResource",
					"operator": "=",
					"value":    "MIDDLEWARE",
				},
			},
		},
	}
	healthCheckType := apistructs.EdgeFormModalField{
		Key:          "healthCheckConfig.healthCheckType",
		Group:        "healthCheckConfig",
		Label:        lr.Get(i18n.I18nKeyCheckType),
		Component:    "select",
		Required:     true,
		DefaultValue: "HTTP",
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeySelectCheckType),
			"options": []map[string]interface{}{
				{
					"name":  "HTTP",
					"value": "HTTP",
				},
				{
					"name":  "COMMAND",
					"value": "COMMAND",
				},
			},
		},
	}
	healthCheckExec := apistructs.EdgeFormModalField{
		Key:       "healthCheckConfig.HealthCheckExec",
		Group:     "healthCheckConfig",
		Label:     lr.Get(i18n.I18nKeyCheckCommand),
		Component: "input",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyInputCheckCommand),
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "healthCheckConfig.healthCheckType",
					"operator": "=",
					"value":    "HTTP",
				},
			},
		},
	}
	healthCheckPath := apistructs.EdgeFormModalField{
		Key:       "healthCheckConfig.path",
		Group:     "healthCheckConfig",
		Label:     lr.Get(i18n.I18nKeyCheckUrl),
		Component: "input",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"placeholder": lr.Get(i18n.I18nKeyInputCheckUrl),
			"maxLength":   apistructs.EdgeDefaultValueMaxLength,
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "healthCheckConfig.healthCheckType",
					"operator": "=",
					"value":    "COMMAND",
				},
			},
		},
	}
	healthCheckPort := apistructs.EdgeFormModalField{
		Key:       "healthCheckConfig.port",
		Group:     "healthCheckConfig",
		Label:     lr.Get(i18n.I18nKeyPort),
		Component: "inputNumber",
		Required:  true,
		ComponentProps: map[string]interface{}{
			"min":       1,
			"max":       65535,
			"precision": 0,
			"className": "full-width",
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "healthCheckConfig.healthCheckType",
					"operator": "=",
					"value":    "COMMAND",
				},
			},
		},
	}

	portMapGroup := apistructs.EdgeFormModalField{
		Label:     "",
		Key:       "portMap",
		Component: "formGroup",
		Group:     "portMap",
		ComponentProps: map[string]interface{}{
			"expandable":    true,
			"defaultExpand": false,
			"showDivider":   false,
			"indentation":   true,
			"title":         lr.Get(i18n.I18nKeyPortMapping),
		},
		RemoveWhen: [][]map[string]interface{}{
			{
				{
					"field":    "deployResource",
					"operator": "=",
					"value":    "MIDDLEWARE",
				},
			},
		},
	}

	if operation == apistructs.EdgeOperationAddApp {
		portMapGroup.ComponentProps["defaultExpand"] = true
	}

	portMapFieldDisabled := false

	if operation == apistructs.EdgeOperationViewDetail {
		portMapFieldDisabled = true
		formModal.ReadOnly = true
	}

	portMapDataField := apistructs.EdgeFormModalField{
		Key:       "portMap.data",
		Label:     lr.Get(i18n.I18nKeyPortMappingRule),
		Group:     "portMap",
		Component: "arrayObj",
		LabelTip:  lr.Get(i18n.I18nKeyPortMappingInputTip),
		Required:  true,
		ComponentProps: map[string]interface{}{
			"direction": "row",
			"disabled":  portMapFieldDisabled,
			"objItems": []map[string]interface{}{
				{
					"key":       "protocol",
					"component": "select",
					"labelTip":  lr.Get(i18n.I18nKeySelectProtocol),
					"required":  true,
					"options":   "k1:TCP",
					"componentProps": map[string]interface{}{
						"placeholder": lr.Get(i18n.I18nKeySelectProtocol),
						"disabled":    portMapFieldDisabled,
					},
				},
				{
					"key":       "container",
					"component": "inputNumber",
					"required":  true,
					"componentProps": map[string]interface{}{
						"className":   "full-width",
						"precision":   0,
						"min":         1,
						"max":         65535,
						"placeholder": lr.Get(i18n.I18nKeyInputContinerPort),
						"disabled":    portMapFieldDisabled,
					},
				},
				{
					"key":       "service",
					"component": "inputNumber",
					"required":  true,
					"componentProps": map[string]interface{}{
						"className":   "full-width",
						"precision":   0,
						"min":         1,
						"max":         65535,
						"placeholder": lr.Get(i18n.I18nKeyInputServicePort),
						"disabled":    portMapFieldDisabled,
					},
				},
			},
		},
	}

	if operation == apistructs.EdgeOperationUpdate {
		appNameField.Disabled = true
		deployResourceField.Disabled = true
		clusterField.Disabled = true
		sitesField.Label = lr.Get(i18n.I18nKeySiteChange)
		cfgSets.Label = lr.Get(i18n.I18nKeyConfigSetChange)
		dependApps.Label = lr.Get(i18n.I18nKeyDependChange)
		replicaField.Label = lr.Get(i18n.I18nKeyReplicaChange)
		storageGroup.Label = lr.Get(i18n.I18nKeyCpuAndMemoryChange)
		mirrorGroup.Label = lr.Get(i18n.I18nKeyImageSettingChange)
		healthCheckGroup.Label = lr.Get(i18n.I18nKeyHealthCheckChange)
		if deployResource == MIDDLEWARE {
			middleWareTypeField.Disabled = true
		}
	}

	formModal.Fields = append(formModal.Fields, &appNameField)
	formModal.Fields = append(formModal.Fields, &deployResourceField)

	formModal.Fields = append(formModal.Fields, &middleWareTypeField)

	formModal.Fields = append(formModal.Fields, &clusterField)
	formModal.Fields = append(formModal.Fields, &sitesField)
	formModal.Fields = append(formModal.Fields, &cfgSets)
	formModal.Fields = append(formModal.Fields, &dependApps)

	formModal.Fields = append(formModal.Fields, &replicaField)

	formModal.Fields = append(formModal.Fields, &storageGroup)
	formModal.Fields = append(formModal.Fields, &cpuRequestField)
	formModal.Fields = append(formModal.Fields, &cpuLimitField)
	formModal.Fields = append(formModal.Fields, &memRequestField)
	formModal.Fields = append(formModal.Fields, &memLimitField)

	formModal.Fields = append(formModal.Fields, &mirrorGroup)
	formModal.Fields = append(formModal.Fields, &mirrorAddrField)
	formModal.Fields = append(formModal.Fields, &mirrorUser)
	formModal.Fields = append(formModal.Fields, &mirrorPassword)

	formModal.Fields = append(formModal.Fields, &healthCheckGroup)
	formModal.Fields = append(formModal.Fields, &healthCheckType)
	formModal.Fields = append(formModal.Fields, &healthCheckExec)
	formModal.Fields = append(formModal.Fields, &healthCheckPath)
	formModal.Fields = append(formModal.Fields, &healthCheckPort)

	formModal.Fields = append(formModal.Fields, &portMapGroup)
	formModal.Fields = append(formModal.Fields, &portMapDataField)

	// All disabled.
	if operation == apistructs.EdgeOperationViewDetail {
		for _, field := range formModal.Fields {
			field.Disabled = true
		}
	} else if operation != apistructs.EdgeOperationUpdate {
		for _, field := range formModal.Fields {
			field.Disabled = false
		}
	}

	return formModal
}
