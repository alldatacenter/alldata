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

package aop

import (
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
)

// Handle 外部调用统一入口
func Handle(ctx *aoptypes.TuneContext) error {
	if !initialized {
		return fmt.Errorf("AOP not initialized")
	}
	typ := ctx.SDK.TuneType
	trigger := ctx.SDK.TuneTrigger
	logrus.Debugf("AOP: type: %s, trigger: %s", typ, trigger)
	chain := tuneGroup.GetTuneChainByTypeAndTrigger(typ, trigger)
	if chain == nil || len(chain) == 0 {
		logrus.Debugf("AOP: type: %s, trigger: %s, tune chain is empty", typ, trigger)
		return nil
	}
	err := chain.Handle(ctx)
	if err != nil {
		logrus.Errorf("AOP: type: %s, trigger: %s, handle failed, err: %v", typ, trigger, err)
		return err
	}
	logrus.Debugf("AOP: type: %s, trigger: %s, handle success", typ, trigger)
	return nil
}
