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

package resourceexecutor

import "flag"

const (
	ReasonUpdateCgroups      = "UpdateCgroups"
	ReasonUpdateSystemConfig = "UpdateSystemConfig"
	ReasonUpdateResctrl      = "UpdateResctrl" // update resctrl tasks, schemata

	EvictPodByNodeMemoryUsage = "EvictPodByNodeMemoryUsage"

	AdjustBEByNodeCPUUsage = "AdjustBEByNodeCPUUsage"
)

var Conf = NewDefaultConfig()

type Config struct {
	ResourceForceUpdateSeconds int
}

func NewDefaultConfig() *Config {
	return &Config{
		ResourceForceUpdateSeconds: 60,
	}
}

func (c *Config) InitFlags(fs *flag.FlagSet) {
	fs.IntVar(&c.ResourceForceUpdateSeconds, "resource-force-update-seconds", c.ResourceForceUpdateSeconds, "executor force update resources interval by seconds")
}
