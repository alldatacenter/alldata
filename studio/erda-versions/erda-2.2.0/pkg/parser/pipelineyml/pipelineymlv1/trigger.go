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
	"github.com/hashicorp/go-multierror"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/cron"
)

func (y *PipelineYml) validateTriggers() error {
	var me *multierror.Error
	var legalCronTriggerNum int
	for _, trigger := range y.obj.Triggers {
		// cron
		if len(trigger.Schedule.Cron) == 0 {
			me = multierror.Append(me, errors.Wrap(errTriggerScheduleCron, trigger.Schedule.Cron))
		}
		if _, err := cron.Parse(trigger.Schedule.Cron); err != nil {
			me = multierror.Append(me, errors.Wrap(err, errTriggerScheduleCron.Error()))
		}
		if !trigger.Schedule.Filters.needDisable(y.option.branch, y.obj.Envs) {
			legalCronTriggerNum++
		}
		// filters
		me = multierror.Append(me, errors.Wrap(trigger.Schedule.Filters.parse(), errTriggerScheduleFilters.Error()))
	}
	if legalCronTriggerNum > 1 {
		me = multierror.Append(me, errTooManyLegalTriggerFound)
	}
	return me.ErrorOrNil()
}

// 可以有多个 schedule cron 声明，但只能有一个生效
func (y *PipelineYml) GetTriggerScheduleCron() (string, bool) {
	for _, trigger := range y.obj.Triggers {
		if !trigger.Schedule.Filters.needDisable(y.option.branch, y.obj.Envs) {
			return trigger.Schedule.Cron, true
		}
	}
	return "", false
}
