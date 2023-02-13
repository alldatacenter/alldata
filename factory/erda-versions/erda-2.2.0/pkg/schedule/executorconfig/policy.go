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

package executorconfig

import (
	"strconv"

	"github.com/erda-project/erda/pkg/strutil"
)

type ExecutorWholeConfigs struct {
	// Common cluster configuration
	BasicConfig map[string]string
	// Accurate cluster configuration
	PlusConfigs *OptPlus
}

// EnableLabelSchedule Whether tag scheduling is turned on
func (c *ExecutorWholeConfigs) EnableLabelSchedule() bool {
	if v, ok := c.BasicConfig["ENABLETAG"]; ok && len(v) > 0 {
		if enable, err := strconv.ParseBool(v); err == nil {
			return enable
		}
	}
	return false
}

// ProjectIDForCompatibility Isolate the configuration used by the project when scheduling, and no longer use it
// DEPRECATED
func (c *ExecutorWholeConfigs) ProjectIDForCompatibility(projectID string) bool {
	_, exist := c.BasicConfig[projectID]
	return exist
}

// EnableOrgLabelSchedule Whether to enable org-level label scheduling
func (c *ExecutorWholeConfigs) EnableOrgLabelSchedule() bool {
	if !c.EnableLabelSchedule() {
		return false
	}
	if enable, ok := c.BasicConfig["ENABLE_ORG"]; ok && strutil.ToLower(enable) == "true" {
		return true
	}
	return false
}

// EnableWorkspaceLabelSchedule Whether to enable label scheduling at the workspace level
func (c *ExecutorWholeConfigs) EnableWorkspaceLabelSchedule() bool {
	if !c.EnableLabelSchedule() {
		return false
	}
	if enable, ok := c.BasicConfig["ENABLE_WORKSPACE"]; ok && strutil.ToLower(enable) == "true" {
		return true
	}
	return false
}

// WORKSPACETAGSForCompatibility Compatible with old WORKSPACETAGS tags
// DEPRECATED
func (c *ExecutorWholeConfigs) WORKSPACETAGSForCompatibility() (string, bool) {
	v, ok := c.BasicConfig["WORKSPACETAGS"]
	return v, ok
}

// StagingJobAvailDest staging environment where job can run
func (c *ExecutorWholeConfigs) StagingJobAvailDest() ([]string, bool) {
	dests, ok := c.BasicConfig["STAGING_JOB_DEST"]
	if !ok {
		return nil, ok
	}
	return strutil.TrimSlice(strutil.Split(dests, ",", true)), true
}

// ProdJobAvailDest prod environment where job can run
func (c *ExecutorWholeConfigs) ProdJobAvailDest() ([]string, bool) {
	dests, ok := c.BasicConfig["PROD_JOB_DEST"]
	if !ok {
		return nil, ok
	}
	return strutil.TrimSlice(strutil.Split(dests, ",", true)), true
}

// OrgOpt Take out the configuration of `org' from all configurations
func (c *ExecutorWholeConfigs) OrgOpt(org string) *OrgOpt {
	for _, orgopt := range c.PlusConfigs.Orgs {
		if orgopt.Name == org {
			opt := OrgOpt(orgopt)
			return &opt
		}
	}
	return nil
}

// Org organization, Corresponding tenant concept
type Org struct {
	Name       string            `json:"name,omitempty"`
	Workspaces []Workspace       `json:"workspaces,omitempty"`
	Options    map[string]string `json:"options,omitempty"`
}

// Environment
type Workspace struct {
	Name    string            `json:"name,omitempty"`
	Options map[string]string `json:"options,omitempty"`
}

// OrgOpt org level configuration
type OrgOpt Org

// WorkspaceOpt workspace level configuration
type WorkspaceOpt Workspace

// WorkspaceOpt Take out the configuration of `workspace' from the org configuration
func (c *OrgOpt) WorkspaceOpt(workspace string) *WorkspaceOpt {
	for _, workspaceopt := range c.Workspaces {
		if workspaceopt.Name == workspace {
			opt := WorkspaceOpt(workspaceopt)
			return &opt
		}
	}
	return nil
}

// EnableWorkspaceLabelSchedule Whether to enable workspace label scheduling
func (c *WorkspaceOpt) EnableWorkspaceLabelSchedule() bool {
	if enable, ok := c.Options["ENABLE_WORKSPACE"]; ok && strutil.ToLower(enable) == "true" {
		return true
	}
	return false
}

// StagingJobAvailDest Environment where staging job can run
func (c *WorkspaceOpt) StagingJobAvailDest() ([]string, bool) {
	dests, ok := c.Options["STAGING_JOB_DEST"]
	if !ok {
		return nil, ok
	}
	return strutil.TrimSlice(strutil.Split(dests, ",", true)), true
}

// ProdJobAvailDest Environment where prod job can run
func (c *WorkspaceOpt) ProdJobAvailDest() ([]string, bool) {
	dests, ok := c.Options["PROD_JOB_DEST"]
	if !ok {
		return nil, ok
	}
	return strutil.TrimSlice(strutil.Split(dests, ",", true)), true
}

type OptPlus struct {
	Orgs []Org `json:"orgs,omitempty"`
}

type ExecutorConfig struct {
	Kind        string            `json:"kind,omitempty"`
	Name        string            `json:"name,omitempty"`
	ClusterName string            `json:"clusterName,omitempty"`
	Options     map[string]string `json:"options,omitempty"`
	OptionsPlus *OptPlus          `json:"optionsPlus,omitempty"`
}
