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
	"fmt"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
)

type Operator struct {
	Modifier  Modifier
	Condition Condition
}

func NewOperator(cfg ModifierCfg) (*Operator, error) {
	if cfg.Separator == "" {
		cfg.Separator = "_"
	}
	mcreator, ok := modifiers[cfg.Action]
	if !ok {
		return nil, fmt.Errorf("unsupported action: %q", cfg.Action)
	}
	ccreator, ok := conditions[cfg.Condition.Op]
	if !ok {
		ccreator = NewNoopCondition
	}
	return &Operator{
		Modifier:  mcreator(cfg),
		Condition: ccreator(cfg.Condition),
	}, nil
}

type Modifier interface {
	Modify(item odata.ObservableData) odata.ObservableData
}

type Condition interface {
	Match(item odata.ObservableData) bool
}

type ModifierCfg struct {
	Key    string `file:"key"`
	Value  string `file:"value"`
	Action string `file:"action"`

	// TODO. may support prometheus-like relabels?
	Keys      []string `file:"keys"`
	Separator string   `file:"separator" default:"_"`
	TargetKey string   `file:"target_key"`

	Condition ConditionCfg `file:"condition"`
}

type ConditionCfg struct {
	Key   string `file:"key"`
	Value string `file:"value"`
	Op    string `file:"op"`
}

type creator func(cfg ModifierCfg) Modifier

var modifiers = map[string]creator{
	"add":         NewAdd,
	"set":         NewSet,
	"drop":        NewDrop,
	"rename":      NewRename,
	"copy":        NewCopy,
	"trim_prefix": NewTrimPrefix,
	"regex":       NewRegex,
	"join":        NewJoin,
}

type condCreator func(cfg ConditionCfg) Condition

var conditions = map[string]condCreator{
	"key_exist":   NewKeyExist,
	"value_match": NewValueMatch,
	"value_empty": NewValueEmpty,
}
