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

// Package labelconfig label 调度相关 key 名, 以及中间结构定义
package labelconfig

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
)

const (
	// executor 名字列表
	// EXECUTOR_K8S k8s
	EXECUTOR_K8S = "K8S"
	// EXECUTOR_METRONOME metronome
	EXECUTOR_METRONOME = "METRONOME"
	// EXECUTOR_MARATHON marathon
	EXECUTOR_MARATHON = "MARATHON"
	// EXECUTOR_CHRONOS chronos
	EXECUTOR_CHRONOS = "CHRONOS"
	// EXECUTOR_EDAS edas
	EXECUTOR_EDAS = "EDAS"
	// EXECUTOR_EDAS edas
	EXECUTOR_EDASV2 = "EDASV2"
	// EXECUTOR_SPARK spark
	EXECUTOR_SPARK = "SPARK"
	// EXECUTOR_SPARK k8s spark
	EXECUTOR_K8SSPARK = "K8SSPARK"
	// EXECUTOR_FLINK k8s flink
	EXECUTOR_K8SFLINK = "K8SFLINK"
	// EXECUTOR_FLINK flink
	EXECUTOR_FLINK = "FLINK"
	// EXECUTOR_K8SJOB k8sjob
	EXECUTOR_K8SJOB = "K8SJOB"

	// ENABLETAG Whether to enable label scheduling
	ENABLETAG = "ENABLETAG"

	// DCOS_ATTRIBUTE The identification of the restriction conditions agreed by dice
	DCOS_ATTRIBUTE = "dice_tags"

	// ORG_KEY key of pistructs.ServiceGroup.Labels
	// org label example: "DICE_ORG_NAME": "org-xxxx"
	ORG_KEY = "DICE_ORG_NAME"
	// ORG_VALUE_PREFIX org label prefix
	ORG_VALUE_PREFIX = "org-"
	// ENABLE_ORG Whether to open org scheduling
	ENABLE_ORG = "ENABLE_ORG"

	// WORKSPACE_KEY apistructs.ServiceGroup.Labels 中的 key
	// workspace label example: "DICE_WORKSPACE" : "workspace-xxxx"
	WORKSPACE_KEY = "DICE_WORKSPACE"
	// WORKSPACE_VALUE_PREFIX workspace label prefix
	WORKSPACE_VALUE_PREFIX = "workspace-"
	// ENABLE_WORKSPACE Whether to open workspace scheduling
	ENABLE_WORKSPACE = "ENABLE_WORKSPACE"

	// CPU_SUBSCRIBE_RATIO The key of the oversold ratio in the configuration
	CPU_SUBSCRIBE_RATIO = "CPU_SUBSCRIBE_RATIO"
	// CPU_NUM_QUOTA Configure the quota value
	CPU_NUM_QUOTA = "CPU_NUM_QUOTA"

	// WORKSPACE_DEV dev environment
	WORKSPACE_DEV = "dev"
	// WORKSPACE_TEST test environment
	WORKSPACE_TEST = "test"
	// WORKSPACE_STAGING staging environment
	WORKSPACE_STAGING = "staging"
	// WORKSPACE_PROD prod environment
	WORKSPACE_PROD = "prod"

	// You can configure the destination workspace for Staging and Prod jobs, such as:
	// "STAGING_JOB_DEST":"dev"
	// "PROD_JOB_DEST":"dev,test"

	// STAGING_JOB_DEST The environment where JOB of the staging environment can run. If there is no configuration, only the environment where it belongs (staging)
	STAGING_JOB_DEST = "STAGING_JOB_DEST"
	// PROD_JOB_DEST The environment in which the JOB of the prod environment can run. If there is no configuration, it is only allowed in the original environment (prod)
	PROD_JOB_DEST = "PROD_JOB_DEST"

	// SPECIFIC_HOSTS Specify a specific key for node scheduling
	SPECIFIC_HOSTS = "SPECIFIC_HOSTS"

	// HOST_UNIQUE Services are scattered in different nodes
	// value: Is a string of json structure
	// e.g. [["service1-name", "service2-name"], ["service3-name"]]
	// value Represents broken groups. For example, in the above example, service1 and service2 are separated from each other, and service3 itself is a group
	HOST_UNIQUE = "HOST_UNIQUE"

	// PLATFORM Whether the service is a platform component
	PLATFORM = "PLATFORM"

	LOCATION_PREFIX = "LOCATION-"

	// K8SLabelPrefix K8S label prefix
	// Both node and pod labels use this prefix
	K8SLabelPrefix = "dice/"

	BIGDATA_AFFINITY_LABELS = "__bigdata_affinity_labels"
)

// LabelPipelineFunc Types of all filter functions in labelpipeline
type LabelPipelineFunc func(*RawLabelRuleResult, *RawLabelRuleResult2, *LabelInfo)

// RawLabelRuleResult label The final output structure of the scheduling module
type RawLabelRuleResult = apistructs.ScheduleInfo

type RawLabelRuleResult2 = apistructs.ScheduleInfo2

// LabelInfo label Scheduling module input structure
type LabelInfo struct {
	// The label field of job or runtime
	Label map[string]string
	// The executor name corresponding to the job or runtime where the label is located
	ExecutorName string
	// The executor kind corresponding to the job or runtime where the label is located
	ExecutorKind string
	// ExecutorConfig cluster configure
	ExecutorConfig *executorconfig.ExecutorWholeConfigs
	// executor optionsPlus corresponding to the job or runtime where the label is located
	OptionsPlus *executorconfig.OptPlus
	// label host (runtime or job) name
	ObjName string
	// Selectors map[servicename]diceyml.Selectors
	Selectors map[string]diceyml.Selectors
}
