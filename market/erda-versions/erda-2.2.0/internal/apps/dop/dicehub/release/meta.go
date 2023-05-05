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

package release

type releaseConfig struct {
	MaxTimeReserved string
}

// ResourceType Release type
type ResourceType string

const (
	// ResourceTypeDiceYml ResourceType is dice.yml
	ResourceTypeDiceYml ResourceType = "diceyml"
	// ResourceTypeAddonYml ResourceType is addon.yml
	ResourceTypeAddonYml ResourceType = "addonyml"
	// ResourceTypeBinary ResourceType is binary executable
	ResourceTypeBinary ResourceType = "binary"
	// ResourceTypeScript ResourceType is executable script file, eg: shell/python/ruby, etc
	ResourceTypeScript ResourceType = "script"
	// ResourceTypeSQL ResourceType is sql
	ResourceTypeSQL ResourceType = "sql"
	// ResourceTypeDataSet ResourceType is Data text file
	ResourceTypeDataSet ResourceType = "data"
	// ResourceTypeAndroid ResourceType is android
	ResourceTypeAndroid ResourceType = "android"
	// ResourceTypeIOS ResourceType is ios
	ResourceTypeIOS ResourceType = "ios"
	// ResourceTypeMigration ResourceType is migration文件releaseID
	ResourceTypeMigration ResourceType = "migration"
	// ResourceTypeH5 ResourceType is h5
	ResourceTypeH5 ResourceType = "h5"
)

const (
	// AliYunRegistry Alibaba Cloud registry prefix
	AliYunRegistry = "registry.cn-hangzhou.aliyuncs.com"
)
