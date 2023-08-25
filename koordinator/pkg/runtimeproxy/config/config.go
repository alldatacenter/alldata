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

package config

import (
	"fmt"
	"strings"
)

type FailurePolicyType string

const (
	// PolicyFail returns error to caller when got an error cri hook server
	PolicyFail FailurePolicyType = "Fail"
	// PolicyIgnore transfer cri request to containerd/dockerd when got an error to cri serer
	PolicyIgnore FailurePolicyType = "Ignore"
	// PolicyNone when no Policy configured. Proxy would ignore errors for PolicyNone like PolicyIgnore.
	PolicyNone = ""
)

func GetFailurePolicyType(typeString string) (FailurePolicyType, error) {
	switch typeString {
	case "Fail":
		return PolicyFail, nil
	case "Ignore":
		return PolicyIgnore, nil
	default:
		return "", fmt.Errorf("failure policy type not supported")
	}
}

type RuntimeHookType string

const (
	defaultRuntimeHookConfigPath string = "/etc/runtime/hookserver.d"
)

const (
	PreRunPodSandbox            RuntimeHookType = "PreRunPodSandbox"
	PostStopPodSandbox          RuntimeHookType = "PostStopPodSandbox"
	PreCreateContainer          RuntimeHookType = "PreCreateContainer"
	PreStartContainer           RuntimeHookType = "PreStartContainer"
	PostStartContainer          RuntimeHookType = "PostStartContainer"
	PreUpdateContainerResources RuntimeHookType = "PreUpdateContainerResources"
	PostStopContainer           RuntimeHookType = "PostStopContainer"
	NoneRuntimeHookType         RuntimeHookType = "NoneRuntimeHookType"
)

type RuntimeHookConfig struct {
	RemoteEndpoint string            `json:"remote-endpoint,omitempty"`
	FailurePolicy  FailurePolicyType `json:"failure-policy,omitempty"`
	RuntimeHooks   []RuntimeHookType `json:"runtime-hooks,omitempty"`
}

type RuntimeRequestPath string

const (
	RunPodSandbox            RuntimeRequestPath = "RunPodSandbox"
	StopPodSandbox           RuntimeRequestPath = "StopPodSandbox"
	CreateContainer          RuntimeRequestPath = "CreateContainer"
	StartContainer           RuntimeRequestPath = "StartContainer"
	UpdateContainerResources RuntimeRequestPath = "UpdateContainerResources"
	StopContainer            RuntimeRequestPath = "StopContainer"
	NoneRuntimeHookPath      RuntimeRequestPath = "NoneRuntimeHookPath"
)

func (ht RuntimeHookType) OccursOn(path RuntimeRequestPath) bool {
	switch ht {
	case PreRunPodSandbox:
		if path == RunPodSandbox {
			return true
		}
	case PostStopPodSandbox:
		if path == StopPodSandbox {
			return true
		}
	case PreCreateContainer:
		if path == CreateContainer {
			return true
		}
	case PreStartContainer:
		if path == StartContainer {
			return true
		}
	case PostStartContainer:
		if path == StartContainer {
			return true
		}
	case PreUpdateContainerResources:
		if path == UpdateContainerResources {
			return true
		}
	case PostStopContainer:
		if path == StopContainer {
			return true
		}
	}
	return false
}

func (hp RuntimeRequestPath) PreHookType() RuntimeHookType {
	if hp == RunPodSandbox {
		return PreRunPodSandbox
	}
	return NoneRuntimeHookType
}

func (hp RuntimeRequestPath) PostHookType() RuntimeHookType {
	if hp == RunPodSandbox {
		return NoneRuntimeHookType
	}
	return NoneRuntimeHookType
}

type RuntimeHookStage string

const (
	PreHook     RuntimeHookStage = "PreHook"
	PostHook    RuntimeHookStage = "PostHook"
	UnknownHook RuntimeHookStage = "UnknownHook"
)

func (ht RuntimeHookType) HookStage() RuntimeHookStage {
	if strings.HasPrefix(string(ht), "Pre") {
		return PreHook
	} else if strings.HasPrefix(string(ht), "Post") {
		return PostHook
	}
	return UnknownHook
}
