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

package options

const (
	DefaultRuntimeProxyEndpoint = "/var/run/koord-runtimeproxy/runtimeproxy.sock"

	DefaultContainerdRuntimeServiceEndpoint = "/var/run/containerd/containerd.sock"
	DefaultContainerdImageServiceEndpoint   = "/var/run/containerd/containerd.sock"

	BackendRuntimeModeContainerd = "Containerd"
	BackendRuntimeModeDocker     = "Docker"
	DefaultBackendRuntimeMode    = BackendRuntimeModeContainerd

	DefaultHookServerKey = "runtimeproxy.koordinator.sh/skip-hookserver"
	DefaultHookServerVal = "true"
)

var (
	RuntimeProxyEndpoint         string
	RemoteRuntimeServiceEndpoint string
	RemoteImageServiceEndpoint   string

	// BackendRuntimeMode default to 'containerd'
	BackendRuntimeMode string

	RuntimeHookServerKey string
	RuntimeHookServerVal string
)
