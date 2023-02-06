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

package testplan

import (
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/strutil"
)

const TestPlanExecuteCallback = "/api/autotests/actions/plan-execute-callback"

func (p *provider) registerWebHook() error {
	ev := apistructs.CreateHookRequest{
		Name:   "auto_test_plan_update",
		Events: []string{bundle.AutoTestPlanExecuteEvent},
		URL:    strutil.Concat("http://", discover.DOP(), TestPlanExecuteCallback),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}
	if err := p.bundle.CreateWebhook(ev); err != nil {
		logrus.Errorf("failed to register %s event to eventbox, (%v)", ev.Name, err)
		return err
	}
	logrus.Infof("register release event to eventbox, event:%+v", ev)
	return nil
}
