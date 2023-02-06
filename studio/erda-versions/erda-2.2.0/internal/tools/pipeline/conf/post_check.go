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

// post_check.go 定义了 Load 之后需要进行的检查

package conf

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/strutil"
)

func checkActionTypeMapping(cfg *Conf) {
	cfg.ActionTypeMapping = make(map[string]string)
	// 可以 环境变量 中临时覆盖映射关系
	for _, v := range strutil.Split(cfg.ActionTypeMappingStr, ",", true) {
		vv := strutil.Split(v, ":", true)
		if len(vv) != 2 {
			logrus.Errorf("[alert] invalid action type mapping: %q", v)
			continue
		}
		cfg.ActionTypeMapping[vv[0]] = vv[1]
	}
}
