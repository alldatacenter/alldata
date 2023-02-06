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

package schedulepolicy

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
	"github.com/erda-project/erda/pkg/strutil"
)

// Tag scheduling is not turned on (that is, the flag bit "ENABLETAG": "true" is not set),
// No matter whether runtime or job has a label
func TestDisableLabelFilterChain(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	// runtime no tags
	r1 := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-011",
		},
	}

	// runtime whit tags
	r2 := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-011",
			Labels: map[string]string{
				"DICE_ORG_NAME":    "1",
				"DICE_WORKSPACE":   "test",
				"ENABLE_WORKSPACE": "true",
				"ENABLE_ORG":       "true",
			},
		},
	}

	_, _, refinedConfigs_, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r1)
	assert.Nil(t, err)
	assert.Nil(t, refinedConfigs_)

	_, _, refinedConfigs_, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r2)
	assert.Nil(t, err)
	assert.Nil(t, refinedConfigs_)
}

// The cluster has enabled tag scheduling, that is, there is "ENABLETAG": "false" in the basic configuration of the cluster
// In the configuration, only org is configured in the basic configuration, but no workspace is configured
func TestLabelFilterChain1(t *testing.T) {
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-011",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "test",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, 0, len(si.Likes))
	assert.Equal(t, 0, len(si.UnLikes))
	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"workspace-", "project-"},
		ExclusiveLikes: []string{"org-1"},
		Flag:           true,
	}, si)
	assert.Equal(t, "1", s2.Org)
	assert.True(t, s2.HasOrg)

}

// The cluster has enabled tag scheduling, that is, there is "ENABLETAG": "false" in the basic configuration of the cluster
// In the basic configuration of the cluster configuration, only workspace is configured, not org
func TestLabelFilterChain2(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true",
        "ENABLE_WORKSPACE": "true"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-012",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "test",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"org-", "project-"},
		ExclusiveLikes: []string{"workspace-test"},
		Flag:           true,
	}, si)

	assert.Equal(t, []string{"test"}, s2.WorkSpaces)
	assert.True(t, s2.HasWorkSpace)
}

// The cluster has enabled tag scheduling, that is, there is "ENABLETAG": "false" in the basic configuration of the cluster
// In the fine configuration of the cluster configuration, only org is configured, but workspace is not configured
func TestLabelFilterChain3(t *testing.T) {
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-013",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "test",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"workspace-", "project-"},
		ExclusiveLikes: []string{"org-1"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasOrg)
	assert.Equal(t, "1", s2.Org)
}

// The cluster has enabled tag scheduling, that is, there is "ENABLETAG": "false" in the basic configuration of the cluster
// In the fine configuration of the cluster configuration, only workspace is configured, not org
func TestLabelFilterChain4(t *testing.T) {
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
                "options": {},
                "workspaces": [
                    {
                        "name": "test",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "2",
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-014",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "test",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"org-", "project-"},
		ExclusiveLikes: []string{"workspace-test"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "test", s2.WorkSpaces[0])
	assert.False(t, s2.HasOrg)
	// constrains, ok := constrains_.([][]string)
	// assert.True(t, ok)
}

// Org and workspace are configured in the fine configuration in the cluster configuration
func TestLabelFilterChain5(t *testing.T) {
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
                "name": "test1",
                "options": {
                    "CPU_SUBSCRIBE_RATIO": "3",
                    "ENABLE_ORG": "true"
                },
                "workspaces": [
                    {
                        "name": "prod",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "2",
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "prod-017",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "test1",
				"DICE_WORKSPACE": "prod",
			},
		},
	}

	s2, si, refinedConfigs_, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"project-"},
		ExclusiveLikes: []string{"org-test1", "workspace-prod"},
		Flag:           true,
	}, si)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "prod", s2.WorkSpaces[0])

	assert.NotNil(t, refinedConfigs_)
	refinedConfigs, ok := refinedConfigs_.(map[string]string)
	assert.True(t, ok)

	cpu_subscribe_ratio, ok := refinedConfigs["CPU_SUBSCRIBE_RATIO"]
	assert.True(t, ok)
	// The cpu overselling ratio reads the CPU_SUBSCRIBE_RATIO under the workspace (prod) under the org (test1) to which the runtime belongs
	assert.Equal(t, "2", cpu_subscribe_ratio)
}

// Org and workspace are configured in the fine configuration in the cluster configuration
// label 中的 org 匹配上，但是 workspace 没有匹配上
func TestLabelFilterChain6(t *testing.T) {
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
                "name": "test1",
                "options": {
                    "CPU_SUBSCRIBE_RATIO": "4",
                    "ENABLE_ORG": "true"
                },
                "workspaces": [
                    {
                        "name": "test",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "2",
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "prod-018",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "test1",
				"DICE_WORKSPACE": "prod",
			},
		},
	}

	s2, si, refinedConfigs_, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"workspace-", "project-"},
		ExclusiveLikes: []string{"org-test1"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasOrg)
	assert.Equal(t, "test1", s2.Org)
	assert.False(t, s2.HasWorkSpace)

	// constrains, ok := constrains_.([][]string)
	// assert.True(t, ok)

	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bworkspace-[^,]+\\b.*"}, constrains[0])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bproject-[^,]+\\b.*"}, constrains[1])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bplatform\\b.*"}, constrains[2])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\blocked\\b.*"}, constrains[3])
	// assert.Equal(t, []string{"dice_tags", "LIKE", ".*\\borg-test1\\b.*"}, constrains[4])

	// In the fine configuration, the org matches, the workspace does not match, and the cpu oversold ratio is set to the oversold ratio under the org
	refinedConfigs, ok := refinedConfigs_.(map[string]string)
	assert.True(t, ok)
	cpu_subscribe_ratio, ok := refinedConfigs["CPU_SUBSCRIBE_RATIO"]
	assert.True(t, ok)
	assert.Equal(t, "4", cpu_subscribe_ratio)
}

// Org and workspace are configured in the fine configuration in the cluster configuration
// The org in the label does not match (equivalent to that neither org nor workspace matches)
func TestLabelFilterChain7(t *testing.T) {
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
                "name": "test1",
                "options": {
                    "CPU_SUBSCRIBE_RATIO": "2",
                    "ENABLE_ORG": "true"
                },
                "workspaces": [
                    {
                        "name": "test",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "2",
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "prod-018",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "test2",
				"DICE_WORKSPACE": "prod",
			},
		},
	}

	s2, si, refinedConfigs_, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:    true,
		Location:      map[string]interface{}{},
		UnLikePrefixs: []string{"org-", "workspace-", "project-"},
		Flag:          true,
	}, si)

	assert.False(t, s2.HasOrg)
	assert.False(t, s2.HasWorkSpace)
	assert.False(t, s2.HasProject)

	// constrains, ok := constrains_.([][]string)
	// assert.True(t, ok)

	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\borg-[^,]+\\b.*"}, constrains[0])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bworkspace-[^,]+\\b.*"}, constrains[1])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bproject-[^,]+\\b.*"}, constrains[2])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bplatform\\b.*"}, constrains[3])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\blocked\\b.*"}, constrains[4])

	// In the fine configuration, org is not matched, so the cpu oversold ratio is the oversold ratio of the default cluster configuration
	_, ok := refinedConfigs_.(map[string]string)
	assert.False(t, ok)
}

// Org and workspace are configured in the fine configuration in the cluster configuration
// The org in the label does not match (equivalent to that neither org nor workspace matches)
// runtime 带上 "SERVICE_TYPE": "STATELESS"
func TestLabelFilterChain8(t *testing.T) {
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
                "name": "test1",
                "options": {
                    "CPU_SUBSCRIBE_RATIO": "2",
                    "ENABLE_ORG": "true"
                },
                "workspaces": [
                    {
                        "name": "test",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "2",
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "prod-018",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "test2",
				"DICE_WORKSPACE": "prod",
				"SERVICE_TYPE":   "STATELESS",
				"SPECIFIC_HOSTS": "192.168.1.2",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"service-stateless"},

		UnLikePrefixs: []string{"org-", "workspace-", "project-"},
		Flag:          true,
		SpecificHost:  []string{"192.168.1.2"},
	}, si)

	assert.Equal(t, 1, len(s2.SpecificHost))
	assert.Equal(t, "192.168.1.2", s2.SpecificHost[0])
	assert.True(t, s2.Stateless)
	assert.True(t, s2.PreferStateless)

	// constrains, ok := constrains_.([][]string)
	// assert.True(t, ok)

	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\borg-[^,]+\\b.*"}, constrains[0])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bworkspace-[^,]+\\b.*"}, constrains[1])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bproject-[^,]+\\b.*"}, constrains[2])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\bplatform\\b.*"}, constrains[3])
	// assert.Equal(t, []string{"dice_tags", "UNLIKE", ".*\\blocked\\b.*"}, constrains[4])
	// assert.Equal(t, []string{"dice_tags", "LIKE", ".*\\bany\\b.*|.*\\bservice-stateless\\b.*"}, constrains[5])
	// assert.Equal(t, []string{"hostname", "LIKE", "192.168.1.2"}, constrains[6])
}

// Test the compatibility with the "WORKSPACETAGS" tag, and note that "ENABLE_WORKSPACE" is not turned on
func TestLabelFilterChain9(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true",
		"WORKSPACETAGS": "staging"
    }
}`)

	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "test-011",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "test",
				"SERVICE_TYPE":   "STATELESS",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"service-stateless"},

		UnLikePrefixs: []string{"org-", "workspace-", "project-"},
		Flag:          true,
	}, si)

	assert.True(t, s2.Stateless)
	assert.True(t, s2.PreferStateless)
	assert.False(t, s2.HasWorkSpace)

	r2 := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-012",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "staging",
				"SERVICE_TYPE":   "STATELESS",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r2)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"service-stateless"},

		UnLikePrefixs:  []string{"org-", "project-"},
		ExclusiveLikes: []string{"workspace-staging"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "staging", s2.WorkSpaces[0])
	assert.True(t, s2.Stateless)
	assert.True(t, s2.PreferStateless)

	r2a := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-012",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "staging",
				"SERVICE_TYPE":   "ADDONS",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r2a)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"service-stateful"},

		UnLikePrefixs:  []string{"org-", "project-"},
		ExclusiveLikes: []string{"workspace-staging"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "staging", s2.WorkSpaces[0])
	assert.True(t, s2.Stateful)
	assert.True(t, s2.PreferStateful)

	r3 := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "prod-013",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "prod",
				"SERVICE_TYPE":   "STATELESS",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r3)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"service-stateless"},

		UnLikePrefixs: []string{"org-", "workspace-", "project-"},
		Flag:          true,
	}, si)

	assert.False(t, s2.HasWorkSpace)
	assert.True(t, s2.Stateless)
	assert.True(t, s2.PreferStateless)

	r3a := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "prod-013",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "1",
				"DICE_WORKSPACE": "prod",
				"SERVICE_TYPE":   "ADDONS",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r3a)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"service-stateful"},

		UnLikePrefixs: []string{"org-", "workspace-", "project-"},
		Flag:          true,
	}, si)

	assert.True(t, s2.Stateful)
	assert.False(t, s2.HasWorkSpace)
}

// Use cluster configuration in multiple enterprise situations (non-refined configuration)
func TestMultiOrg(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "MARATHON",
    "name": "MARATHONFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "CPU_SUBSCRIBE_RATIO": "10",
        "ENABLETAG": "true",
        "ENABLE_ORG": "true",
        "ENABLE_WORKSPACE": "true"
    },
    "optionsPlus": {
        "orgs": [
            {
                "name": "test1",
                "options": {
                    "CPU_SUBSCRIBE_RATIO": "3"
                },
                "workspaces": [
                    {
                        "name": "prod",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "2"
                        }
                    }
                ]
            },
            {
                "name": "hangzhou",
                "options": {
                    "CPU_SUBSCRIBE_RATIO": "5"
                },
                "workspaces": [
                    {
                        "name": "prod",
                        "options": {
                            "CPU_SUBSCRIBE_RATIO": "4"
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	r := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "prod-017",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "hangzhou",
				"DICE_WORKSPACE": "prod",
			},
		},
	}

	s2, si, refinedConfigs_, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"project-"},
		ExclusiveLikes: []string{"org-hangzhou", "workspace-prod"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasOrg)
	assert.Equal(t, "hangzhou", s2.Org)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "prod", s2.WorkSpaces[0])

	assert.NotNil(t, refinedConfigs_)
	refinedConfigs, ok := refinedConfigs_.(map[string]string)
	assert.True(t, ok)

	cpu_subscribe_ratio, ok := refinedConfigs["CPU_SUBSCRIBE_RATIO"]
	assert.True(t, ok)
	// The cpu overselling ratio reads the CPU_SUBSCRIBE_RATIO under the workspace (prod) under the org (hangzhou) to which the runtime belongs
	assert.Equal(t, "4", cpu_subscribe_ratio)

	r2 := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "prod-017",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "test1",
				"DICE_WORKSPACE": "dev",
			},
		},
	}

	s2, si, refinedConfigs_, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r2)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"project-"},
		ExclusiveLikes: []string{"org-test1", "workspace-dev"},
		Flag:           true,
	}, si)
	assert.True(t, s2.HasOrg)
	assert.Equal(t, "test1", s2.Org)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "dev", s2.WorkSpaces[0])

	assert.NotNil(t, refinedConfigs_)
	refinedConfigs, ok = refinedConfigs_.(map[string]string)
	assert.True(t, ok)

	cpu_subscribe_ratio, ok = refinedConfigs["CPU_SUBSCRIBE_RATIO"]
	assert.True(t, ok)
	// The cpu overselling ratio reads the configuration under the org (test1) to which the runtime belongs, and the workspace does not match the fine configuration
	assert.Equal(t, "3", cpu_subscribe_ratio)

	r3 := apistructs.ServiceGroup{
		ClusterName: "terminus-dev",
		Dice: apistructs.Dice{
			ID: "staging-0179",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "wrong",
				"DICE_WORKSPACE": "staging",
			},
		},
	}

	s2, si, refinedConfigs_, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, r3)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"project-"},
		ExclusiveLikes: []string{"org-wrong", "workspace-staging"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasOrg)
	assert.Equal(t, "wrong", s2.Org)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "staging", s2.WorkSpaces[0])

	assert.Nil(t, refinedConfigs_)
}

// Ordinary job and big data job
func TestLabelFilterChainForJob(t *testing.T) {
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

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	j := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name:   "xx",
			Labels: map[string]string{},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j)
	assert.Nil(t, err)

	assert.NotEqual(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs: []string{"org-", "workspace-", "project-"},
		Flag:          true,
	}, si)

	assert.True(t, s2.Job)
	assert.False(t, s2.HasOrg)
	assert.False(t, s2.HasWorkSpace)
	assert.False(t, s2.HasProject)

	j2 := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"JOB_KIND": "bigdata",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j2)
	assert.Nil(t, err)

	assert.NotEqual(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"org-", "workspace-", "project-"},
		ExclusiveLikes: []string{"bigdata"},
	}, si)

	assert.True(t, s2.BigData)
}

// metronome has enabled workspace scheduling and the job has a corresponding label
func TestLabelFilterChainForJob2(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "METRONOME",
    "name": "METRONOMEFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "ENABLETAG": "true",
        "ENABLE_WORKSPACE": "true"
    }
}`)

	// Job of the test environment
	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	j := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xx",
				"DICE_WORKSPACE": "TEST",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs:  []string{"org-", "project-"},
		ExclusiveLikes: []string{"workspace-test"},
		Flag:           true,
	}, si)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "test", s2.WorkSpaces[0])
	assert.True(t, s2.Job)

	// Job of the staging environment
	j2 := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xx",
				"DICE_WORKSPACE": "STAGING",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j2)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs:  []string{"org-", "project-"},
		InclusiveLikes: []string{"workspace-dev", "workspace-test"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.False(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "dev"))
	assert.False(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "test"))

	// Job of the prod environment
	j3 := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xx",
				"DICE_WORKSPACE": "PROD",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j3)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs:  []string{"org-", "project-"},
		InclusiveLikes: []string{"workspace-dev", "workspace-test"},
		Flag:           true,
	}, si)

	assert.True(t, s2.Job)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.False(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "dev"))
	assert.False(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "test"))
}

// metronome has enabled workspace scheduling and the job has a corresponding label
func TestLabelFilterChainForJob3(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "METRONOME",
    "name": "METRONOMEFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "ENABLETAG": "true",
        "ENABLE_ORG": "true",
        "ENABLE_WORKSPACE": "true"
    }
}`)

	// Job of the test environment
	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	j := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xx",
				"DICE_WORKSPACE": "TEST",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs:  []string{"project-"},
		ExclusiveLikes: []string{"org-xx", "workspace-test"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasOrg)
	assert.Equal(t, "xx", s2.Org)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "test", s2.WorkSpaces[0])
	assert.True(t, s2.Job)
	// Job of the staging environment
	j2 := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xy",
				"DICE_WORKSPACE": "STAGING",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j2)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs:  []string{"project-"},
		ExclusiveLikes: []string{"org-xy"},
		InclusiveLikes: []string{"workspace-dev", "workspace-test"},
		Flag:           true,
	}, si)

	assert.True(t, s2.HasOrg)
	assert.Equal(t, "xy", s2.Org)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.False(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "dev"))
	assert.False(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "test"))

	// Job of the prod environment
	j3 := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xyz",
				"DICE_WORKSPACE": "PROD",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j3)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs:  []string{"project-"},
		ExclusiveLikes: []string{"org-xyz"},
		InclusiveLikes: []string{"workspace-dev", "workspace-test"},
		Flag:           true,
	}, si)

	assert.True(t, s2.Job)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.False(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "test"))
	assert.False(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "dev"))
	assert.True(t, s2.HasOrg)
	assert.Equal(t, "xyz", s2.Org)
}

// Test the configuration of STAGING_JOB_DEST and PROD_JOB_DEST
func TestLabelFilterChainForJob4(t *testing.T) {
	var jsonBlob = []byte(`{
    "clusterName": "terminus-dev",
    "kind": "METRONOME",
    "name": "METRONOMEFORTERMINUSDEV",
    "options": {
        "ADDR": "http://master.mesos/service/marathon",
        "ENABLETAG": "true",
        "ENABLE_WORKSPACE": "true",
		"PROD_JOB_DEST": "staging",
		"STAGING_JOB_DEST": "test,prod"
    }
}`)

	// Job of the test environment
	var eConfig executorconfig.ExecutorConfig
	err := json.Unmarshal(jsonBlob, &eConfig)
	assert.Nil(t, err)
	assert.Nil(t, eConfig.OptionsPlus)

	labelConfigs := &executorconfig.ExecutorWholeConfigs{
		BasicConfig: eConfig.Options,
		PlusConfigs: eConfig.OptionsPlus,
	}

	j := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xx",
				"DICE_WORKSPACE": "TEST",
			},
		},
	}

	s2, si, _, err := LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs:  []string{"org-", "project-"},
		ExclusiveLikes: []string{"workspace-test"},
		Flag:           true,
	}, si)

	assert.True(t, s2.Job)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 1, len(s2.WorkSpaces))
	assert.Equal(t, "test", s2.WorkSpaces[0])

	// Job of the staging environment
	j2 := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xx",
				"DICE_WORKSPACE": "STAGING",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j2)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked: true,
		Location:   map[string]interface{}{},
		Likes:      []string{"job"},

		UnLikePrefixs:  []string{"org-", "project-"},
		InclusiveLikes: []string{"workspace-test", "workspace-prod"},
		Flag:           true,
	}, si)

	assert.True(t, s2.Job)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, 2, len(s2.WorkSpaces))
	assert.True(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "prod"))
	assert.True(t, strutil.Contains(strutil.Concat(s2.WorkSpaces...), "test"))

	// Job of the prod environment
	j3 := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name: "xx",
			Labels: map[string]string{
				"DICE_ORG_NAME":  "xx",
				"DICE_WORKSPACE": "PROD",
			},
		},
	}

	s2, si, _, err = LabelFilterChain(labelConfigs, eConfig.Name, eConfig.Kind, j3)
	assert.Nil(t, err)

	assert.Equal(t, apistructs.ScheduleInfo{
		IsUnLocked:     true,
		Likes:          []string{"job"},
		Location:       map[string]interface{}{},
		UnLikePrefixs:  []string{"org-", "project-"},
		InclusiveLikes: []string{"workspace-staging"},
		Flag:           true,
	}, si)

	assert.True(t, s2.Job)
	assert.True(t, s2.HasWorkSpace)
	assert.Equal(t, "staging", s2.WorkSpaces[0])
}
