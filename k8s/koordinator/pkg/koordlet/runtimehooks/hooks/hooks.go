/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package hooks

import (
	"fmt"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	rmconfig "github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config"
)

type Hook struct {
	name        string
	stage       rmconfig.RuntimeHookType
	description string
	fn          HookFn
}

type HookFn func(protocol.HooksProtocol) error

var globalStageHooks map[rmconfig.RuntimeHookType][]*Hook

func Register(stage rmconfig.RuntimeHookType, name, description string, hookFn HookFn) *Hook {
	h, err := generateNewHook(stage, name)
	if err != nil {
		klog.Fatalf("hook %s register failed, reason: %v", name, err)
		return h
	}
	klog.V(1).Infof("hook %s is registered", name)
	h.description = description
	h.fn = hookFn
	return h
}

func generateNewHook(stage rmconfig.RuntimeHookType, name string) (*Hook, error) {
	stageHooks, stageExist := globalStageHooks[stage]
	if !stageExist {
		return nil, fmt.Errorf("stage %s is invalid", stage)
	}

	for _, hook := range stageHooks {
		if hook.name == name {
			return hook, fmt.Errorf("hook %s with stage %s is conflict since already registered", name, stage)
		}
	}
	newHook := &Hook{name: name, stage: stage}
	globalStageHooks[stage] = append(globalStageHooks[stage], newHook)
	return newHook, nil
}

func getHooksByStage(stage rmconfig.RuntimeHookType) []*Hook {
	if hooks, exist := globalStageHooks[stage]; exist {
		return hooks
	} else {
		return []*Hook{}
	}
}

func RunHooks(stage rmconfig.RuntimeHookType, protocol protocol.HooksProtocol) {
	hooks := getHooksByStage(stage)
	klog.V(5).Infof("start run %v hooks at %s", len(hooks), stage)
	for _, hook := range hooks {
		klog.V(5).Infof("call hook %v", hook.name)
		if err := hook.fn(protocol); err != nil {
			klog.Warningf("failed to run hook %s in stage %s, reason: %v", hook.name, stage, err)
		}
	}
}

func init() {
	globalStageHooks = map[rmconfig.RuntimeHookType][]*Hook{
		rmconfig.PreRunPodSandbox:            make([]*Hook, 0),
		rmconfig.PreCreateContainer:          make([]*Hook, 0),
		rmconfig.PreStartContainer:           make([]*Hook, 0),
		rmconfig.PostStartContainer:          make([]*Hook, 0),
		rmconfig.PostStopContainer:           make([]*Hook, 0),
		rmconfig.PostStopPodSandbox:          make([]*Hook, 0),
		rmconfig.PreUpdateContainerResources: make([]*Hook, 0),
	}
}

func GetStages(disable map[string]struct{}) []rmconfig.RuntimeHookType {
	var stages []rmconfig.RuntimeHookType
	for stage, hooks := range globalStageHooks {
		if _, ok := disable[string(stage)]; ok {
			continue
		}
		if len(hooks) > 0 {
			stages = append(stages, stage)
		}
	}
	return stages
}
