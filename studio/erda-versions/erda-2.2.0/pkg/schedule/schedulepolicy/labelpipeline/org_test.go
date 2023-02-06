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
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
)

// Tested in the following situations:

// The flag bit ("ENABLETAG": "true") has been set to enable tag scheduling，
// The cluster configuration has only basic configuration
// Org and no org are included in the label of the test runtime
func TestOrgLabelFilter1(t *testing.T) {
	// There is no fine configuration in the cluster configuration. If org is not configured in the basic configuration, then (all orgs) do not enable org scheduling
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
		// There is no org tag in the label
		Label:          make(map[string]string),
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	OrgLabelFilter(&result, &result2, li)
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.UnLikes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.False(t, result.Flag)
	assert.Equal(t, 0, len(result.UnLikePrefixs))
	//assert.Equal(t, labelconfig.ORG_VALUE_PREFIX, result.UnLikePrefixs[0])

	result = labelconfig.RawLabelRuleResult{}
	result2 = labelconfig.RawLabelRuleResult2{}

	li2 := &labelconfig.LabelInfo{
		// There is an org tag in the label, but org scheduling is not enabled (there is no "ENABLE_ORG" setting)
		Label: map[string]string{
			"DICE_ORG_NAME":  "org-1",
			"DICE_WORKSPACE": "test",
		},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	OrgLabelFilter(&result, &result2, li2)
	// The result is the same as result1, because there is no fine configuration in the cluster configuration and the org scheduling is not enabled in the basic configuration (the "ENABLE_ORG" is not set in the cluster configuration)
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.UnLikes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.False(t, result.Flag)
	assert.Equal(t, 1, len(result.UnLikePrefixs))
	assert.Equal(t, labelconfig.ORG_VALUE_PREFIX, result.UnLikePrefixs[0])

	result = labelconfig.RawLabelRuleResult{}
	result2 = labelconfig.RawLabelRuleResult2{}

	li3 := &labelconfig.LabelInfo{
		Label:          map[string]string{},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-12345",
		Selectors: map[string]diceyml.Selectors{
			"placehold": {
				"org":       diceyml.Selector{Values: []string{"org-1"}},
				"workspace": diceyml.Selector{Values: []string{"test"}},
			},
		},
	}

	OrgLabelFilter(&result, &result2, li3)
	// The result is the same as result1, because there is no fine configuration in the cluster configuration and the org scheduling is not enabled in the basic configuration (the "ENABLE_ORG" is not set in the cluster configuration)
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.UnLikes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.False(t, result.Flag)
	assert.Equal(t, 1, len(result.UnLikePrefixs))
	assert.Equal(t, labelconfig.ORG_VALUE_PREFIX, result.UnLikePrefixs[0])
}

// The org scheduling is set in the basic configuration of the cluster (the recommended usage is to enable it in the fine configuration, see TestOrgLabelFilter3)
func TestOrgLabelFilter2(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true",
        "ENABLE_ORG": "true"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2

	li := &labelconfig.LabelInfo{
		// org label in label
		Label: map[string]string{
			"DICE_ORG_NAME":  "1xx",
			"DICE_WORKSPACE": "test",
		},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1221",
	}

	OrgLabelFilter(&result, &result2, li)
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.UnLikes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.UnLikePrefixs))
	assert.False(t, result.Flag)
	assert.Equal(t, []string{"org-1xx"}, result.ExclusiveLikes)
}

// Org scheduling is set in the fine configuration of the cluster, and org is set in the runtime label
func TestOrgLabelFilter3(t *testing.T) {
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
                "name": "2",
                "options": {
                    "ENABLE_ORG": "true"
                },
                "workspaces": [
                    {
                        "name": "test",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "2"
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

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2

	li := &labelconfig.LabelInfo{
		Label: map[string]string{
			"DICE_ORG_NAME":  "2",
			"DICE_WORKSPACE": "test",
		},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	OrgLabelFilter(&result, &result2, li)
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.UnLikes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.UnLikePrefixs))
	assert.Equal(t, []string{"org-2"}, result.ExclusiveLikes)
}

// The org scheduling is set in the fine configuration of the cluster, but org is not set in the runtime label
func TestOrgLabelFilter4(t *testing.T) {
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
                            "CPU_SUBSCRIBE_RATIO": "2"
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

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2

	li2 := &labelconfig.LabelInfo{
		Label:          map[string]string{},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1111",
	}

	OrgLabelFilter(&result, &result2, li2)
	assert.Zero(t, len(result.UnLikes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.ExclusiveLikes))
	//assert.Equal(t, []string{"org-"}, result.UnLikePrefixs)
}

// Org scheduling is set in the fine configuration of the cluster, and org is set in the runtime label
// But the org name set in the runtime label does not appear in the orgs of the cluster's fine configuration
func TestOrgLabelFilter5(t *testing.T) {
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
                            "CPU_SUBSCRIBE_RATIO": "2"
                        }
                    }
                ]
            },
            {
                "name": "test",
                "options": {
                    "ENABLE_ORG": "true"
                }
            }
        ]
    }
}`)
	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)

	var result labelconfig.RawLabelRuleResult
	var result2 labelconfig.RawLabelRuleResult2

	li := &labelconfig.LabelInfo{
		Label: map[string]string{
			"DICE_ORG_NAME":  "3",
			"DICE_WORKSPACE": "test",
		},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
	}

	OrgLabelFilter(&result, &result2, li)
	assert.Zero(t, len(result.UnLikes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.Equal(t, []string{"org-"}, result.UnLikePrefixs)

	li1 := &labelconfig.LabelInfo{
		Label:          map[string]string{},
		ExecutorName:   eConfig.Name,
		ExecutorKind:   eConfig.Kind,
		ExecutorConfig: &executorconfig.ExecutorWholeConfigs{BasicConfig: eConfig.Options, PlusConfigs: eConfig.OptionsPlus},
		OptionsPlus:    eConfig.OptionsPlus,
		ObjName:        "test-1234",
		Selectors: map[string]diceyml.Selectors{
			"placehold": {
				"org":       diceyml.Selector{Values: []string{"3"}},
				"workspace": diceyml.Selector{Values: []string{"test"}},
			},
		},
	}
	result = labelconfig.RawLabelRuleResult{}
	result2 = labelconfig.RawLabelRuleResult2{}

	OrgLabelFilter(&result, &result2, li1)
	assert.Zero(t, len(result.UnLikes))
	assert.Zero(t, len(result.LikePrefixs))
	assert.Zero(t, len(result.Likes))
	assert.Zero(t, len(result.ExclusiveLikes))
	assert.Equal(t, []string{"org-"}, result.UnLikePrefixs)

}
