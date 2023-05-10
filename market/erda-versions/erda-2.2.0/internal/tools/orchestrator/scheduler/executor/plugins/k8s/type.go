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

package k8s

import (
	"regexp"

	appsv1 "k8s.io/api/apps/v1"

	"github.com/erda-project/erda/apistructs"
)

const (
	// to be deprecated.
	// ADDON_GROUPS -> SERVICE_GROUPS
	// ADDON_GROUP_ID -> SERVICE_GROUP_ID
	groupNum = "ADDON_GROUPS"
	groupID  = "ADDON_GROUP_ID"

	groupNum2 = "SERVICE_GROUPS"
	groupID2  = "SERVICE_GROUP_ID"

	// aliyun registry
	AliyunRegistry = "aliyun-registry"
	CustomRegistry = "custom-registry"
	// local volume storageclass name
	localStorage = "dice-local-volume"

	// default sa
	defaultServiceAccountName = "default"
	DiceWorkSpace             = "DICE_WORKSPACE"
)

var envReg = regexp.MustCompile(`\$\{([^}]+?)\}`)

type StatefulsetInfo struct {
	sg          *apistructs.ServiceGroup
	namespace   string
	envs        map[string]string
	annotations map[string]string
}

// OneGroupInfo Returns information about the statefulset corresponding to the group
type OneGroupInfo struct {
	sg  *apistructs.ServiceGroup
	sts *appsv1.StatefulSet
}

const (
	ServiceType        = "SERVICE_TYPE"
	StatelessService   = "STATELESS_SERVICE"
	IsStatelessService = "true"
	ServiceAddon       = "ADDONS"
	ServicePerNode     = "per_node"
	ServiceJob         = "JOB"
)
