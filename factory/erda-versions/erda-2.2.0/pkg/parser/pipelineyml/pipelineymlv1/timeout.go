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

package pipelineymlv1

import (
	"time"

	"github.com/hashicorp/go-multierror"
	"github.com/pkg/errors"
)

// parseTimeout 返回值:
// -2 用户未指定(nil)
// -1 forever
// >0 具体的 timeout
func parseTimeout(input interface{}) (timeout time.Duration, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = errors.Errorf("%v", r)
		}
		if err != nil {
			err = errors.Errorf("failed to parse timeout by input [%#v], err: [%v], "+
				`only support: -1 or format such as "300ms", "-1.5h" or "2h45m"(time units are "ns", "us" (or "µs"), "ms", "s", "m", "h")`, input, err)
		}

	}()

	switch input.(type) {
	case nil:
		return -2, nil
	case string:
		d, err := time.ParseDuration(input.(string))
		if err != nil {
			return 0, err
		}
		if d < -2 || d == 0 {
			return 0, errors.Errorf("invalid parsed result [%vs], must be -1 or >0", time.Duration(d).Seconds())
		}
		return d, nil
	case int, int32, int64, float32, float64:
		switch input {
		case int(-1), int32(-1), int64(-1), float32(-1), float64(-1):
			return -1, nil
		default:
			return 0, errors.New("int type must be -1 to represent forever")
		}
	default:
		return 0, errors.Errorf("not supported type [%T]", input)
	}
}

func (y *PipelineYml) checkTimeout() error {
	var me *multierror.Error
	for _, stage := range y.obj.Stages {
		for _, step := range stage.Tasks {
			if _, err := step.GetTimeout(); err != nil {
				me = multierror.Append(me, err)
			}
		}
	}
	return me.ErrorOrNil()
}
