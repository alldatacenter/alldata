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

package aop

import (
	"errors"
	"strings"

	. "github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
)

var pluginsMap = map[TuneType]map[string]TunePoint{}

// RegisterTunePoint register tunePoint
func RegisterTunePoint(tunePoint TunePoint) error {
	if _, ok := pluginsMap[tunePoint.Type()]; !ok {
		pluginsMap[tunePoint.Type()] = make(map[string]TunePoint)
	}

	// if tunePoint name is existed
	if _, ok := pluginsMap[tunePoint.Type()][tunePoint.Name()]; ok {
		return errors.New("failed to register TuneType: " + string(tunePoint.Type()) + " tunePoint is already exist, TuneName: " + tunePoint.Name())
	}
	pluginsMap[tunePoint.Type()][tunePoint.Name()] = tunePoint
	return nil
}

func register(tuneType TuneType, tuneTrigger TuneTrigger, tunePoint TunePoint) error {
	if tuneGroup == nil {
		tuneGroup = make(map[TuneType]map[TuneTrigger]TuneChain)
	}

	group, ok := tuneGroup[tuneType]
	if !ok {
		group = make(map[TuneTrigger]TuneChain)
	}
	group[tuneTrigger] = append(group[tuneTrigger], tunePoint)
	tuneGroup[tuneType] = group
	return nil
}

// handleTuneConfigs Convert TuneConfigs to TuneGroup
func handleTuneConfigs(gp TuneConfigs) error {
	for tuneType := range gp {
		for tuneTrigger := range gp[tuneType] {
			for _, tuneName := range gp[tuneType][tuneTrigger] {
				err := register(tuneType, tuneTrigger, pluginsMap[tuneType][tuneName])
				if err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func NewProviderNameByPluginName(point TunePoint) string {
	// please use "-" replace all "_"
	// if provider name is "plugin_name" in conf/pipeline.yml, it will be registered as "plugin-name"
	if strings.Contains(point.Name(), "_") {
		panic("provider name invaild, tuneType: " + string(point.Type()) + ", name: " + point.Name())
	}
	return "erda.core.pipeline.aop.plugins." + string(point.Type()) + "." + point.Name()
}
