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

package labelpipeline

import (
	"encoding/json"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/schedule/executorconfig"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

// The test cases that are not enabled for tag scheduling are placed in policy_test.go for execution

// The flag bit ("ENABLETAG": "true") has been set to enable tag scheduling,
// And when the runtime or job does not carry any label
// The cluster configuration has only basic configuration
func TestIdentityLabelFilter1(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2

	li := &labelconfig.LabelInfo{
		Label:          make(map[string]string),
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	IdentityFilter(&result, &result2, li)
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.False(t, result.IsPlatform)
	assert.True(t, result.IsUnLocked)

	// Temporary support for the project tag
	assert.Equal(t, []string{"project-"}, result.UnLikePrefixs)
	// The flag of any is set to true, but the any label is only combined with the label in Likes, and Likes is empty, so any will not appear in the constraints
	assert.True(t, result.Flag)
}

// The flag bit ("ENABLETAG": "true") has been set to enable tag scheduling,
// And when the runtime or job has a label set
// The cluster configuration has only basic configuration
func TestIdentityLabelFilter2(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2
	li := &labelconfig.LabelInfo{
		Label: map[string]string{
			"DICE_ORG_NAME":  "1",
			"DICE_WORKSPACE": "dev",
			"SERVICE_TYPE":   "STATELESS"},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	IdentityFilter(&result, &result2, li)
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.Equal(t, []string{"service-stateless"}, result.Likes)
	assert.False(t, result.IsPlatform)
	assert.True(t, result.IsUnLocked)

	// Temporary support for the project tag
	assert.Equal(t, []string{"project-"}, result.UnLikePrefixs)
	assert.True(t, result.Flag)
}

// Test addons stateful service
func TestIdentityLabelFilter2B(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2
	li := &labelconfig.LabelInfo{
		Label: map[string]string{
			"SERVICE_TYPE": "ADDONS"},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	IdentityFilter(&result, &result2, li)
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.Equal(t, []string{"service-stateful"}, result.Likes)
	assert.False(t, result.IsPlatform)
	assert.True(t, result.IsUnLocked)

	// Temporary support for the project tag
	assert.Equal(t, []string{"project-"}, result.UnLikePrefixs)
	assert.True(t, result.Flag)
}

// The flag bit ("ENABLETAG": "true") has been set to enable tag scheduling,
// And when the runtime or job has a label set
// Cluster configuration and fine configuration
func TestIdentityLabelFilter3(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true"
    },
    "optionsPlus": {
        "orgs": [
            {
                "name": "1",
                "options": {
                    "ENABLE_ORG": "true"
                },
                "workspaces": [
                    {
                        "name": "test",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "3",
                            "ENABLE_WORKSPACE": "true"
                        }
                    }
                ]
            }
        ]
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.NotNil(t, eConfig.OptionsPlus)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2
	li := &labelconfig.LabelInfo{
		Label: map[string]string{
			"DICE_ORG_NAME":  "1",
			"DICE_WORKSPACE": "dev",
			"SERVICE_TYPE":   "STATELESS"},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	// Same result as TestIdentityLabelFilter2, because the fine configuration of the cluster only affects ORG and WORKSPACE labels
	IdentityFilter(&result, &result2, li)
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.Equal(t, []string{"service-stateless"}, result.Likes)
	assert.False(t, result.IsPlatform)
	assert.True(t, result.IsUnLocked)

	// Temporary support for the project tag
	assert.Equal(t, []string{"project-"}, result.UnLikePrefixs)
	assert.True(t, result.Flag)
}

// The flag bit ("ENABLETAG": "true") has been set to enable tag scheduling,
// And the job has a label set
func TestIdentityLabelFilter4(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "METRONOME",
    "name": "METRONOMEFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "ENABLETAG": "true"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2
	// The org and workspace information of the current job are stored in ENV
	li := &labelconfig.LabelInfo{
		Label:          map[string]string{},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	IdentityFilter(&result, &result2, li)
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.Equal(t, []string{"job"}, result.Likes)
	assert.False(t, result.IsPlatform)
	assert.True(t, result.IsUnLocked)

	assert.Equal(t, []string{"project-"}, result.UnLikePrefixs)
	assert.True(t, result.Flag)

	result = labelconfig.RawLabelRuleResult{}
	result2 = labelconfig.RawLabelRuleResult2{}
	li2 := &labelconfig.LabelInfo{
		Label: map[string]string{
			"DICE_ORG_NAME":  "xx",
			"DICE_WORKSPACE": "TEST",
		},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	IdentityFilter(&result, &result2, li2)
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.Equal(t, []string{"job"}, result.Likes)
	assert.False(t, result.IsPlatform)
	assert.True(t, result.IsUnLocked)
	assert.Equal(t, []string{"project-"}, result.UnLikePrefixs)
	assert.True(t, result.Flag)
}

// Test bigdata type job
func TestIdentityLabelFilter6(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "METRONOME",
    "name": "METRONOMEFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "ENABLETAG": "true"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2
	// The org and workspace information of the current job are stored in ENV
	li := &labelconfig.LabelInfo{
		Label:          map[string]string{"JOB_KIND": "bigdata"},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	IdentityFilter(&result, &result2, li)
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.Likes))
	assert.Equal(t, []string{"bigdata"}, result.ExclusiveLikes)
	assert.False(t, result.IsPlatform)
	assert.True(t, result.IsUnLocked)
	assert.Equal(t, []string{"project-"}, result.UnLikePrefixs)
	assert.False(t, result.Flag)
}

func TestBigdataAndAny(t *testing.T) {
	matchTags := make([]string, 0)
	println(strings.Join(matchTags, ","))
	x1 := strings.Join(matchTags, ",")

	x := strings.Split(x1, ",")

	x = append(x, "bigdata")
	y := strings.Join(x, ",")

	assert.Equal(t, ",bigdata", y)
}

func TestIdentityLabelFilterBigdataLabel(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "K8SJOB",
    "name": "K8SJOBFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2
	li := &labelconfig.LabelInfo{
		Label: map[string]string{
			"DICE_ORG_NAME":             "1",
			"DICE_WORKSPACE":            "dev",
			"SERVICE_TYPE":              "STATELESS",
			"__bigdata_affinity_labels": "bigdata-job,bigdata-job2",
			"JOB_KIND":                  "bigdata",
		},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	IdentityFilter(&result, &result2, li)
	assert.Equal(t, true, result2.BigData)
	assert.Equal(t, []string{"bigdata-job", "bigdata-job2"}, result2.BigDataLabels)
}
