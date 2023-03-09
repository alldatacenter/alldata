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

package util

import (
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

type PSIPath struct {
	CPU string
	Mem string
	IO  string
}

type PSIByResource struct {
	CPU system.PSIStats
	Mem system.PSIStats
	IO  system.PSIStats
}

func GetPSIByResource(paths PSIPath) (*PSIByResource, error) {
	cpuStats, err := system.ReadPSI(paths.CPU)
	if err != nil {
		return nil, err
	}
	memStats, err := system.ReadPSI(paths.Mem)
	if err != nil {
		return nil, err
	}
	ioStats, err := system.ReadPSI(paths.IO)
	if err != nil {
		return nil, err
	}
	return &PSIByResource{
		CPU: cpuStats,
		Mem: memStats,
		IO:  ioStats,
	}, nil
}
