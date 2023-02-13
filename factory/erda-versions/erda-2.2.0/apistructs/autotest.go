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

package apistructs

import (
	"fmt"
	"time"

	"github.com/erda-project/erda/pkg/strutil"
)

const (
	AutoTestFileTreeNodeMetaKeyPipelineYml   = "pipelineYml"
	AutoTestFileTreeNodeMetaKeyHistoryID     = "historyID"
	AutoTestFileTreeNodeMetaKeySnippetAction = "snippetAction"
	AutoTestFileTreeNodeMetaKeyRunParams     = "runParams"
	AutoTestFileTreeNodeMetaKeyExtra         = "extra"
)

type AutoTestFileTreeNode struct {
	Type      UnifiedFileTreeNodeType
	Scope     string
	ScopeID   string
	Pinode    string `gorm:"type:bigint(20)"` // root dir 的 pinode 为 "0"，表示无 pinode
	Inode     string `gorm:"type:bigint(20)"`
	Name      string
	Desc      string
	CreatorID string
	UpdaterID string
}

type AutoTestNodeMetaSnippetObj struct {
	Alias  string                 `json:"alias"`
	Params map[string]interface{} `json:"params"`
}

type AutoTestsScope string

var (
	AutoTestsScopeProject         AutoTestsScope = "project"
	AutoTestsScopeProjectTestPlan AutoTestsScope = "project-testplan" // 测试计划单独的目录树
)

type AutoTestCaseSavePipelineRequest struct {
	Inode string `json:"inode"`

	PipelineYml string             `json:"pipelineYml"`
	RunParams   []PipelineRunParam `json:"runParams"`

	IdentityInfo
}
type AutoTestCaseSavePipelineResponse struct {
	Header
	Data *UnifiedFileTreeNode `json:"data,omitempty"`

	IdentityInfo
}

func (req AutoTestCaseSavePipelineRequest) BasicValidate() error {
	if err := strutil.Validate(req.Inode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid inode: %v", err)
	}
	if req.PipelineYml == "" && req.RunParams == nil {
		return fmt.Errorf("nothing to save, please specify pipelineYml or runParams")
	}
	return nil
}

type AutoTestGlobalConfigType string

var (
	AutoTestGlobalConfigTypeAPI AutoTestGlobalConfigType = "API"
	AutoTestGlobalConfigTypeUI  AutoTestGlobalConfigType = "UI"
)

type AutoTestGlobalConfigCreateRequest struct {
	Scope   string `json:"scope"`
	ScopeID string `json:"scopeID"`

	DisplayName string `json:"displayName"`
	Desc        string `json:"desc"`

	APIConfig *AutoTestAPIConfig `json:"apiConfig,omitempty"`
	UIConfig  *AutoTestUIConfig  `json:"uiConfig,omitempty"`

	IdentityInfo
}
type AutoTestGlobalConfigCreateResponse struct {
	Header
	Data *AutoTestGlobalConfig `json:"data,omitempty"`
}

func (req AutoTestGlobalConfigCreateRequest) BasicValidate() error {
	// scope
	if err := strutil.Validate(req.Scope, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid scope: %v", err)
	}
	// scope id
	if err := strutil.Validate(req.ScopeID, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid scopeID: %v", err)
	}
	// display name
	if err := strutil.Validate(req.DisplayName, strutil.MinLenValidator(1), strutil.MaxLenValidator(191)); err != nil {
		return fmt.Errorf("invalid displayname: %v", err)
	}
	// desc
	if err := strutil.Validate(req.Desc, strutil.MaxLenValidator(512)); err != nil {
		return fmt.Errorf("invalid desc: %v", err)
	}

	// configs
	hasAtLeastOneTypeConfig := false
	// api config
	if req.APIConfig != nil {
		hasAtLeastOneTypeConfig = true
		if err := req.APIConfig.BasicValidate(); err != nil {
			return fmt.Errorf("invalid apiConfig: %v", err)
		}
	}
	// ui config
	if req.UIConfig != nil {
		hasAtLeastOneTypeConfig = true
		if err := req.UIConfig.BasicValidate(); err != nil {
			return fmt.Errorf("invalid uiConfig: %v", err)
		}
	}

	// doesn't have any configs declared
	if !hasAtLeastOneTypeConfig {
		return fmt.Errorf("empty configs")
	}

	return nil
}

type AutoTestGlobalConfig struct {
	Scope   string `json:"scope"`
	ScopeID string `json:"scopeID"`
	Ns      string `json:"ns"`

	DisplayName string    `json:"displayName,omitempty"`
	Desc        string    `json:"desc,omitempty"`
	CreatorID   string    `json:"creatorID"`
	UpdaterID   string    `json:"updaterID"`
	CreatedAt   time.Time `json:"createdAt,omitempty"`
	UpdatedAt   time.Time `json:"updatedAt,omitempty"`

	APIConfig *AutoTestAPIConfig `json:"apiConfig,omitempty"`
	UIConfig  *AutoTestUIConfig  `json:"uiConfig,omitempty"`
}

type SortByUpdateTimeAutoTestGlobalConfigs []AutoTestGlobalConfig

func (p SortByUpdateTimeAutoTestGlobalConfigs) Len() int {
	return len(p)
}
func (p SortByUpdateTimeAutoTestGlobalConfigs) Less(i, j int) bool {
	return p[i].UpdatedAt.After(p[j].UpdatedAt)
}
func (p SortByUpdateTimeAutoTestGlobalConfigs) Swap(i, j int) {
	p[i], p[j] = p[j], p[i]
}

func (cfg AutoTestGlobalConfig) GetUserIDs() []string {
	return strutil.DedupSlice([]string{cfg.CreatorID, cfg.UpdaterID}, true)
}

type AutoTestAPIConfig struct {
	Domain string                        `json:"domain"`
	Header map[string]string             `json:"header"`
	Global map[string]AutoTestConfigItem `json:"global"`
}

func (cfg AutoTestAPIConfig) BasicValidate() error {
	// domain 格式校验
	if cfg.Domain != "" {
		if !strutil.HasPrefixes(cfg.Domain, "http://", "https://") {
			return fmt.Errorf("invalid domain protocol")
		}
	}
	// global
	for key, item := range cfg.Global {
		if item.Type == "" {
			return fmt.Errorf("invalid type of global key: %s", key)
		}
	}
	return nil
}

type AutoTestConfigItem struct {
	Name  string `json:"name"`
	Type  string `json:"type"`
	Value string `json:"value"`
	Desc  string `json:"desc,omitempty"`
}

type AutoTestUIConfig struct {
}

func (cfg AutoTestUIConfig) BasicValidate() error {
	return nil
}

type AutoTestGlobalConfigUpdateRequest struct {
	PipelineCmsNs string `json:"ns"`

	DisplayName string `json:"displayName"`
	Desc        string `json:"desc"`

	APIConfig *AutoTestAPIConfig `json:"apiConfig,omitempty"`
	UIConfig  *AutoTestUIConfig  `json:"uiConfig,omitempty"`

	IdentityInfo
}
type AutoTestGlobalConfigUpdateResponse struct {
	Header
	Data *AutoTestAPIConfig `json:"data,omitempty"`
}

func (req AutoTestGlobalConfigUpdateRequest) BasicValidate() error {
	// ns
	if err := strutil.Validate(req.PipelineCmsNs, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid pipeline cms ns, err: %v", err)
	}
	// display name
	if err := strutil.Validate(req.DisplayName, strutil.MaxLenValidator(191)); err != nil {
		return fmt.Errorf("invalid displayname: %v", err)
	}
	// desc
	if err := strutil.Validate(req.Desc, strutil.MaxLenValidator(512)); err != nil {
		return fmt.Errorf("invalid desc: %v", err)
	}

	if req.APIConfig != nil {
		if err := req.APIConfig.BasicValidate(); err != nil {
			return fmt.Errorf("invalid apiConfig: %v", err)
		}
	}
	if req.UIConfig != nil {
		if err := req.UIConfig.BasicValidate(); err != nil {
			return fmt.Errorf("invalid uiConfig: %v", err)
		}
	}

	return nil
}

type AutoTestGlobalConfigDeleteRequest struct {
	PipelineCmsNs string `json:"ns"`

	IdentityInfo
}
type AutoTestGlobalConfigDeleteResponse struct {
	Header
	Data *AutoTestAPIConfig `json:"data,omitempty"`
}

func (req AutoTestGlobalConfigDeleteRequest) BasicValidate() error {
	if err := strutil.Validate(req.PipelineCmsNs, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid ns: %v", err)
	}
	return nil
}

type AutoTestGlobalConfigListRequest struct {
	Scope   string `json:"scope"`
	ScopeID string `json:"scopeID"`

	IdentityInfo
}
type AutoTestGlobalConfigListResponse struct {
	Header
	Data []AutoTestGlobalConfig `json:"data,omitempty"`
}

func (req AutoTestGlobalConfigListRequest) BasicValidate() error {
	if err := strutil.Validate(req.Scope, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid scope: %v", err)
	}
	if err := strutil.Validate(req.ScopeID, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid scopeID: %v", err)
	}
	return nil
}
