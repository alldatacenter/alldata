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

package aggregator

import (
	"fmt"
	"time"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

type functionCall func(pre, cur *metric.Metric) *metric.Metric

type RuleConfig struct {
	Func      string        `file:"func"`
	Args      []interface{} `file:"args"`
	TargetKey string        `file:"target_key"`
}

type ruler struct {
	Fn    functionCall
	Args  []argGetter
	Alias string
}

type argGetter interface {
	GetValue(data *metric.Metric) (float64, bool)
}

type keySelector struct {
	key string
}

func (ks *keySelector) GetValue(data *metric.Metric) (float64, bool) {
	val, ok := data.Fields[ks.key]
	if !ok {
		return 0, false
	}
	valf, ok := tryGetFloat64(val)
	if !ok {
		return 0, false
	}
	return valf, true
}

func tryGetFloat64(val interface{}) (float64, bool) {
	res := float64(0)
	switch vv := val.(type) {
	case float64:
		res = vv
	case float32:
		res = float64(vv)
	case uint64:
		res = float64(vv)
	case uint32:
		res = float64(vv)
	case int64:
		res = float64(vv)
	case int32:
		res = float64(vv)
	case int:
		res = float64(vv)
	default:
		return 0, false
	}
	return res, true
}

type constSelector struct {
	value float64
}

func (cs *constSelector) GetValue(*metric.Metric) (float64, bool) {
	return cs.value, true
}

func newRuler(cfg RuleConfig) (*ruler, error) {
	r := &ruler{
		Alias: cfg.TargetKey,
	}

	args := make([]argGetter, len(cfg.Args))
	for i := 0; i < len(args); i++ {
		argget, err := parserArgs(cfg.Args[i])
		if err != nil {
			return nil, fmt.Errorf("parserArgs: %w", err)
		}
		args[i] = argget
	}
	r.Args = args

	fn, err := r.parseFunction(cfg.Func)
	if err != nil {
		return nil, err
	}
	r.Fn = fn

	return r, nil
}

func (r *ruler) parseFunction(name string) (functionCall, error) {
	switch name {
	case "rate":
		return r.rateCall, nil
	case "+", "-", "*", "/":
		return r.binaryFactory(name), nil
	default:
		return nil, fmt.Errorf("invalide func: %s", name)
	}
}

func parserArgs(arg interface{}) (argGetter, error) {
	switch v := arg.(type) {
	case string:
		return &keySelector{key: v}, nil
	case float64:
		return &constSelector{value: v}, nil
	case int64:
		return &constSelector{value: float64(v)}, nil
	case int32:
		return &constSelector{value: float64(v)}, nil
	case int:
		return &constSelector{value: float64(v)}, nil
	default:
		return nil, fmt.Errorf("invalid arg: %+v", arg)
	}
}

// counter rate
func (r *ruler) rateCall(pre, cur *metric.Metric) *metric.Metric {
	if len(r.Args) != 1 {
		return cur
	}
	if pre == nil {
		return cur
	}
	preT := getEventTime(pre)
	curT := getEventTime(cur)
	if preT.UnixNano() > curT.UnixNano() {
		return cur
	}
	preV, ok := r.Args[0].GetValue(pre)
	if !ok {
		return cur
	}
	curV, ok := r.Args[0].GetValue(cur)
	if !ok {
		return cur
	}

	ds := curT.Sub(preT).Seconds()
	if curV < preV {
		cur.Fields[r.Alias] = curV / ds
	} else {
		cur.Fields[r.Alias] = (curV - preV) / ds
	}
	return cur
}

func (r *ruler) binaryFactory(op string) functionCall {
	return func(_, cur *metric.Metric) *metric.Metric {
		if len(r.Args) != 2 {
			return cur
		}
		p1, ok := r.Args[0].GetValue(cur)
		if !ok {
			return cur
		}
		p2, ok := r.Args[1].GetValue(cur)
		if !ok {
			return cur
		}
		switch op {
		case "*":
			cur.Fields[r.Alias] = p1 * p2
		case "/":
			if p2 != 0 {
				cur.Fields[r.Alias] = p1 / p2
			}
		case "+":
			cur.Fields[r.Alias] = p1 + p2
		case "-":
			cur.Fields[r.Alias] = p1 - p2
		}
		return cur
	}
}

func getEventTime(data *metric.Metric) time.Time {
	return time.Unix(0, data.Timestamp)
}
