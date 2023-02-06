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
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib"
)

type Regex struct {
	cfg     ModifierCfg
	pattern *regexp.Regexp
}

func (r *Regex) Modify(item odata.ObservableData) odata.ObservableData {
	val, ok := odata.GetKeyValue(item, r.cfg.Key)
	if !ok {
		return item
	}
	// TODO. May use group index
	for k, v := range lib.RegexGroupMap(r.pattern, val.(string)) {
		odata.SetKeyValue(item, odata.TagsPrefix+k, v)
	}
	return item
}

func NewRegex(cfg ModifierCfg) Modifier {
	return &Regex{cfg: cfg, pattern: regexp.MustCompile(cfg.Value)}
}
