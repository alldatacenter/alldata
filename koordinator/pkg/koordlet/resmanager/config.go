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

package resmanager

import (
	"flag"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/resmanager/plugins"
)

type Config struct {
	ReconcileIntervalSeconds   int
	CPUSuppressIntervalSeconds int
	CPUEvictIntervalSeconds    int
	MemoryEvictIntervalSeconds int
	MemoryEvictCoolTimeSeconds int
	CPUEvictCoolTimeSeconds    int
	QOSExtensionCfg            *plugins.QOSExtensionConfig
}

func NewDefaultConfig() *Config {
	return &Config{
		ReconcileIntervalSeconds:   1,
		CPUSuppressIntervalSeconds: 1,
		CPUEvictIntervalSeconds:    1,
		MemoryEvictIntervalSeconds: 1,
		MemoryEvictCoolTimeSeconds: 4,
		CPUEvictCoolTimeSeconds:    20,
		QOSExtensionCfg:            &plugins.QOSExtensionConfig{FeatureGates: map[string]bool{}},
	}
}

func (c *Config) InitFlags(fs *flag.FlagSet) {
	fs.IntVar(&c.ReconcileIntervalSeconds, "reconcile-interval-seconds", c.ReconcileIntervalSeconds, "reconcile be pod cgroup interval by seconds")
	fs.IntVar(&c.CPUSuppressIntervalSeconds, "cpu-suppress-interval-seconds", c.CPUSuppressIntervalSeconds, "suppress be pod cpu resource interval by seconds")
	fs.IntVar(&c.CPUEvictIntervalSeconds, "cpu-evict-interval-seconds", c.CPUEvictIntervalSeconds, "evict be pod(cpu) interval by seconds")
	fs.IntVar(&c.MemoryEvictIntervalSeconds, "memory-evict-interval-seconds", c.MemoryEvictIntervalSeconds, "evict be pod(memory) interval by seconds")
	fs.IntVar(&c.MemoryEvictCoolTimeSeconds, "memory-evict-cool-time-seconds", c.MemoryEvictCoolTimeSeconds, "cooling time: memory next evict time should after lastEvictTime + MemoryEvictCoolTimeSeconds")
	fs.IntVar(&c.CPUEvictCoolTimeSeconds, "cpu-evict-cool-time-seconds", c.CPUEvictCoolTimeSeconds, "cooltime: CPU next evict time should after lastEvictTime + CPUEvictCoolTimeSeconds")
	c.QOSExtensionCfg.InitFlags(fs)
}
