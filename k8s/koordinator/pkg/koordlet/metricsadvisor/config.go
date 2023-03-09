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

package metricsadvisor

import "flag"

type Config struct {
	CollectResUsedIntervalSeconds     int
	CollectNodeCPUInfoIntervalSeconds int
	CPICollectorIntervalSeconds       int
	PSICollectorIntervalSeconds       int
	CPICollectorTimeWindowSeconds     int
}

func NewDefaultConfig() *Config {
	return &Config{
		CollectResUsedIntervalSeconds:     1,
		CollectNodeCPUInfoIntervalSeconds: 60,
		CPICollectorIntervalSeconds:       60,
		PSICollectorIntervalSeconds:       10,
		CPICollectorTimeWindowSeconds:     10,
	}
}

func (c *Config) InitFlags(fs *flag.FlagSet) {
	// todo: replace with DurationVar and modify run feature util
	fs.IntVar(&c.CollectResUsedIntervalSeconds, "collect-res-used-interval-seconds", c.CollectResUsedIntervalSeconds, "Collect node/pod resource usage interval by seconds")
	fs.IntVar(&c.CollectNodeCPUInfoIntervalSeconds, "collect-node-cpu-info-interval-seconds", c.CollectNodeCPUInfoIntervalSeconds, "Collect node cpu info interval by seconds")
	fs.IntVar(&c.CPICollectorIntervalSeconds, "cpi-collector-interval-seconds", c.CPICollectorIntervalSeconds, "Collect cpi interval by seconds")
	fs.IntVar(&c.PSICollectorIntervalSeconds, "psi-collector-interval-seconds", c.PSICollectorIntervalSeconds, "Collect psi interval by seconds")
	fs.IntVar(&c.CPICollectorTimeWindowSeconds, "collect-cpi-timewindow-seconds", c.CPICollectorTimeWindowSeconds, "Collect cpi time window by seconds")
}
