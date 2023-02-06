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

package aoptypes

import "github.com/sirupsen/logrus"

// TuneChain 表示一组有序 TunePoint
type TuneChain []TunePoint

// Handle 根据上下文调用 TuneChain
func (chain TuneChain) Handle(ctx *TuneContext) error {
	if len(chain) == 0 {
		return nil
	}
	for _, point := range chain {
		logrus.Debugf("begin handle tune point, type: %s, trigger: %s, name: %s", point.Type(), ctx.SDK.TuneTrigger, point.Name())
		if err := point.Handle(ctx); err != nil {
			logrus.Errorf("end handle tune point, type: %s, trigger: %s, name: %s, failed, err: %v", point.Type(), ctx.SDK.TuneTrigger, point.Name(), err)
		} else {
			logrus.Debugf("end handle tune point, type: %s, trigger: %s, name: %s, success", point.Type(), ctx.SDK.TuneTrigger, point.Name())
		}
	}
	return nil
}
