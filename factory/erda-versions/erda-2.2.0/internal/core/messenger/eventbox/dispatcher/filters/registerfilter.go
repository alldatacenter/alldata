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

package filters

import (
	"fmt"
	"reflect"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/constant"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher/errors"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/register"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
)

type RegisterFilter struct {
	reg register.Register
}

func NewRegisterFilter(r register.Register) Filter {
	return &RegisterFilter{
		reg: r,
	}
}

func (*RegisterFilter) Name() string {
	return "RegisterFilter"
}

func (r *RegisterFilter) Filter(m *types.Message) *errors.DispatchError {
	derr := errors.New()
	keys, ok := m.Labels[types.LabelKey(constant.RegisterLabelKey)]
	if !ok {
		return derr
	}
	keys_, err := getLabelKeys(keys)
	if err != nil {
		errStr := fmt.Errorf("RegisterFilter: register value illegal type: %v", err)
		logrus.Error(errStr)
		derr.FilterErr = errStr
		return derr
	}
	for _, key := range keys_ {
		keyLabels := r.reg.PrefixGet(key)
		if keyLabels == nil {
			infoStr := fmt.Sprintf("RegisterFilter: prefixget labelkey: %s, notfound", key)
			logrus.Warn(infoStr)
			derr.FilterInfo = infoStr
		}
		for _, labels := range keyLabels {
			m.Labels = mergeLabels(labels, m.Labels)
		}
	}
	return derr

}

// l2 has higher priority
func mergeLabels(l1, l2 map[types.LabelKey]interface{}) map[types.LabelKey]interface{} {
	l := make(map[types.LabelKey]interface{})
	for k, v := range l1 {
		l[k] = v
	}
	for k, v := range l2 {
		l[k] = v
	}
	return l
}

func getLabelKeys(keys interface{}) ([]string, error) {
	switch i := keys.(type) {
	case []interface{}:
		r := []string{}
		for _, k := range i {
			k_, ok := k.(string)
			if !ok {
				return nil, fmt.Errorf("expect string or []string, got []%s", reflect.TypeOf(k))
			}
			r = append(r, k_)
		}
		return r, nil
	case string:
		return []string{i}, nil
	case []string:
		return i, nil
	default:
		return nil, fmt.Errorf("expect string or []string, got %s", reflect.TypeOf(keys))
	}

}
