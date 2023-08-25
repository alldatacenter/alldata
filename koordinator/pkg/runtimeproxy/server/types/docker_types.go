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

package types

import (
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/network"

	"github.com/koordinator-sh/koordinator/cmd/koord-runtime-proxy/options"
)

const (
	ContainerTypeLabelKey       = "io.kubernetes.docker.type"
	ContainerTypeLabelSandbox   = "podsandbox"
	ContainerTypeLabelContainer = "container"
	SandboxIDLabelKey           = "io.kubernetes.sandbox.id"
)

type ConfigWrapper struct {
	*container.Config
	HostConfig       *container.HostConfig
	NetworkingConfig *network.NetworkingConfig
}

func SkipRuntimeHook(labels map[string]string) bool {
	if val, ok := labels[options.RuntimeHookServerKey]; ok && val == options.RuntimeHookServerVal {
		return true
	}
	return false
}
