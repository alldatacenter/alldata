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

package operator

import (
	"regexp"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
)

type KeyExist struct {
	cfg ConditionCfg
}

func (k *KeyExist) Match(item odata.ObservableData) bool {
	_, ok := odata.GetKeyValue(item, k.cfg.Key)
	return ok
}

func NewKeyExist(cfg ConditionCfg) Condition {
	return &KeyExist{cfg: cfg}
}

type ValueMatch struct {
	cfg     ConditionCfg
	pattern *regexp.Regexp
}

func (v *ValueMatch) Match(item odata.ObservableData) bool {
	value, ok := odata.GetKeyValue(item, v.cfg.Key)
	if !ok {
		return false
	}
	return v.pattern.MatchString(value.(string))
}

func NewValueMatch(cfg ConditionCfg) Condition {
	return &ValueMatch{cfg: cfg, pattern: regexp.MustCompile(cfg.Value)}
}

type ValueEmpty struct {
	cfg ConditionCfg
}

func (ve *ValueEmpty) Match(item odata.ObservableData) bool {
	value, ok := odata.GetKeyValue(item, ve.cfg.Key)
	if !ok {
		return false
	}
	return value == ""
}

func NewValueEmpty(cfg ConditionCfg) Condition {
	return &ValueEmpty{cfg: cfg}
}

type NoopCondition struct {
	cfg ConditionCfg
}

func (n *NoopCondition) Match(item odata.ObservableData) bool {
	return true
}

func NewNoopCondition(cfg ConditionCfg) Condition {
	return &NoopCondition{cfg: cfg}
}
