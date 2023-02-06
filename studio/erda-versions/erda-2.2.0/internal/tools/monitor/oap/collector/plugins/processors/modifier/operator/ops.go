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
	"strings"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
)

type Add struct {
	cfg ModifierCfg
}

func (a *Add) Modify(item odata.ObservableData) odata.ObservableData {
	if _, ok := odata.GetKeyValue(item, a.cfg.Key); ok {
		return item
	}
	if a.cfg.Value == "" {
		return item
	}
	odata.SetKeyValue(item, a.cfg.Key, a.cfg.Value)
	return item
}

func NewAdd(cfg ModifierCfg) Modifier {
	return &Add{cfg: cfg}
}

type Set struct {
	cfg ModifierCfg
}

func (s *Set) Modify(item odata.ObservableData) odata.ObservableData {
	odata.SetKeyValue(item, s.cfg.Key, s.cfg.Value)
	return item
}

func NewSet(cfg ModifierCfg) Modifier {
	return &Set{cfg: cfg}
}

type Drop struct {
	cfg ModifierCfg
}

func (d *Drop) Modify(item odata.ObservableData) odata.ObservableData {
	odata.DeleteKeyValue(item, d.cfg.Key)
	return item
}

func NewDrop(cfg ModifierCfg) Modifier {
	return &Drop{cfg: cfg}
}

type Rename struct {
	cfg ModifierCfg
}

func (r *Rename) Modify(item odata.ObservableData) odata.ObservableData {
	// value is the new key
	val, ok := odata.GetKeyValue(item, r.cfg.Key)
	if !ok {
		return item
	}
	odata.SetKeyValue(item, r.cfg.Value, val)
	odata.DeleteKeyValue(item, r.cfg.Key)
	return item
}

func NewRename(cfg ModifierCfg) Modifier {
	return &Rename{cfg: cfg}
}

type Copy struct {
	cfg ModifierCfg
}

func (c *Copy) Modify(item odata.ObservableData) odata.ObservableData {
	val, ok := odata.GetKeyValue(item, c.cfg.Key)
	if !ok {
		return item
	}
	odata.SetKeyValue(item, c.cfg.Value, val)
	return item
}

func NewCopy(cfg ModifierCfg) Modifier {
	return &Copy{cfg: cfg}
}

type TrimPrefix struct {
	cfg ModifierCfg
}

// TODO. Need better way
func (t *TrimPrefix) Modify(item odata.ObservableData) odata.ObservableData {
	// key is the prefix
	tags := item.GetTags()
	tmp := make(map[string]string)
	for k := range tags {
		if strings.Index(k, t.cfg.Key) != -1 { // found
			tmp[k] = k[len(t.cfg.Key):]
		}
	}
	for k, newk := range tmp {
		val, ok := tags[k]
		if !ok {
			continue
		}
		delete(tags, k)
		tags[newk] = val
	}
	return item
}

func NewTrimPrefix(cfg ModifierCfg) Modifier {
	return &TrimPrefix{cfg: cfg}
}

type Join struct {
	cfg ModifierCfg
}

func NewJoin(cfg ModifierCfg) Modifier {
	return &Join{cfg: cfg}
}

func (j *Join) Modify(item odata.ObservableData) odata.ObservableData {
	if j.cfg.TargetKey == "" {
		return item
	}

	vals := make([]string, len(j.cfg.Keys))
	for i, k := range j.cfg.Keys {
		val, ok := odata.GetKeyValue(item, k)
		if !ok {
			return item
		}

		vals[i] = val.(string)
	}
	odata.SetKeyValue(item, j.cfg.TargetKey, strings.Join(vals, j.cfg.Separator))
	return item
}
