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

package dbclient

import (
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// EdgeSite edge site model
type EdgeSite struct {
	dbengine.BaseModel
	OrgID       int64
	Name        string
	DisplayName string
	Description string
	Logo        string
	ClusterID   int64
	Status      int64
}

func (EdgeSite) TableName() string {
	return "edge_sites"
}

// EdgeConfigSet edge config set,  union key: clusterName and name
type EdgeConfigSet struct {
	dbengine.BaseModel
	OrgID       int64
	ClusterID   int64
	Name        string
	DisplayName string
	Description string
}

func (EdgeConfigSet) TableName() string {
	return "edge_configsets"
}

// EdgeConfigSetItem edge config data model, union key: clusterID, siteID, configKey
type EdgeConfigSetItem struct {
	dbengine.BaseModel
	ConfigsetID int64
	Scope       string
	SiteID      int64
	ItemKey     string
	ItemValue   string
}

func (EdgeConfigSetItem) TableName() string {
	return "edge_configsets_item"
}

// EdgeApp edge app model
type EdgeApp struct {
	dbengine.BaseModel
	OrgID               int64
	Name                string
	ClusterID           int64
	Type                string
	Image               string
	ProductID           int64
	AddonName           string
	AddonVersion        string
	RegistryAddr        string
	RegistryUser        string
	RegistryPassword    string
	HealthCheckType     string
	HealthCheckHttpPort int
	HealthCheckHttpPath string
	HealthCheckExec     string
	ConfigSetName       string
	Replicas            int32
	Description         string
	EdgeSites           string
	DependApp           string
	LimitCpu            float64
	RequestCpu          float64
	LimitMem            float64
	RequestMem          float64
	PortMaps            string
	ExtraData           string
}

func (EdgeApp) TableName() string {
	return "edge_apps"
}
