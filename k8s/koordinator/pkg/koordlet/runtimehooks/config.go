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

package runtimehooks

import (
	"flag"

	"k8s.io/apimachinery/pkg/util/runtime"
	cliflag "k8s.io/component-base/cli/flag"
	"k8s.io/component-base/featuregate"

	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks/batchresource"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks/cpuset"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks/gpu"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks/groupidentity"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

const (
	// owner: @zwzhang0107 @saintube
	// alpha: v0.3
	// beta: v1.1
	//
	// GroupIdentity set pod cpu group identity(bvt) according to QoS.
	GroupIdentity featuregate.Feature = "GroupIdentity"

	// owner: @saintube @zwzhang0107
	// alpha: v0.3
	// beta: v1.1
	//
	// CPUSetAllocator set container cpuset according to allocate result from koord-scheduler for LSR/LS pods.
	CPUSetAllocator featuregate.Feature = "CPUSetAllocator"

	// owner: @ZYecho @jasonliu747
	// alpha: v0.3
	// beta: v1.1
	//
	// GPUEnvInject injects gpu allocated env info according to allocate result from koord-scheduler.
	GPUEnvInject featuregate.Feature = "GPUEnvInject"

	// owner: @saintube @zwzhang0107
	// alpha: v1.1
	//
	// BatchResource set request and limits of cpu and memory on cgroup file.
	BatchResource featuregate.Feature = "BatchResource"
)

var (
	defaultRuntimeHooksFG = map[featuregate.Feature]featuregate.FeatureSpec{
		GroupIdentity:   {Default: true, PreRelease: featuregate.Beta},
		CPUSetAllocator: {Default: true, PreRelease: featuregate.Beta},
		GPUEnvInject:    {Default: false, PreRelease: featuregate.Alpha},
		BatchResource:   {Default: true, PreRelease: featuregate.Beta},
	}

	runtimeHookPlugins = map[featuregate.Feature]HookPlugin{
		GroupIdentity:   groupidentity.Object(),
		CPUSetAllocator: cpuset.Object(),
		GPUEnvInject:    gpu.Object(),
		BatchResource:   batchresource.Object(),
	}
)

type Config struct {
	RuntimeHooksNetwork       string
	RuntimeHooksAddr          string
	RuntimeHooksFailurePolicy string
	RuntimeHookConfigFilePath string
	RuntimeHookHostEndpoint   string
	RuntimeHookDisableStages  []string
	FeatureGates              map[string]bool // Deprecated
}

func NewDefaultConfig() *Config {
	return &Config{
		RuntimeHooksNetwork:       "unix",
		RuntimeHooksAddr:          "/host-var-run-koordlet/koordlet.sock",
		RuntimeHooksFailurePolicy: "Ignore",
		RuntimeHookConfigFilePath: system.Conf.RuntimeHooksConfigDir,
		RuntimeHookHostEndpoint:   "/var/run/koordlet/koordlet.sock",
		RuntimeHookDisableStages:  []string{},
		FeatureGates:              map[string]bool{},
	}
}

func (c *Config) InitFlags(fs *flag.FlagSet) {
	fs.StringVar(&c.RuntimeHooksNetwork, "runtime-hooks-network", c.RuntimeHooksNetwork, "rpc server network type for runtime hooks")
	fs.StringVar(&c.RuntimeHooksAddr, "runtime-hooks-addr", c.RuntimeHooksAddr, "rpc server address for runtime hooks")
	fs.StringVar(&c.RuntimeHooksFailurePolicy, "runtime-hooks-failure-policy", c.RuntimeHooksFailurePolicy, "failure policy for runtime hooks")
	fs.StringVar(&c.RuntimeHookConfigFilePath, "runtime-hooks-config-path", c.RuntimeHookConfigFilePath, "config file path for runtime hooks")
	fs.StringVar(&c.RuntimeHookHostEndpoint, "runtime-hooks-host-endpoint", c.RuntimeHookHostEndpoint, "host endpoint of runtime proxy")
	fs.Var(cliflag.NewStringSlice(&c.RuntimeHookDisableStages), "runtime-hooks-disable-stages", "disable stages for runtime hooks")
	fs.Var(cliflag.NewMapStringBool(&c.FeatureGates), "runtime-hooks", "Deprecated because all settings have been moved to --feature-gates parameters")
}

func init() {
	runtime.Must(features.DefaultMutableKoordletFeatureGate.Add(defaultRuntimeHooksFG))
}
