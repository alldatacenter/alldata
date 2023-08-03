/*
Copyright 2022 The Koordinator Authors.
Copyright 2020 The Kubernetes Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package coscheduling

import (
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/controller"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/core"
)

var _ frameworkext.ControllerProvider = &Coscheduling{}

func (cs *Coscheduling) NewControllers() ([]frameworkext.Controller, error) {
	handle := cs.frameworkHandler
	podInformer := handle.SharedInformerFactory().Core().V1().Pods()
	pgMgr := cs.pgMgr.(*core.PodGroupManager)
	var controllerWorkers int
	if cs.args == nil {
		controllerWorkers = 1
	} else {
		controllerWorkers = int(*cs.args.ControllerWorkers)
	}
	podGroupController := controller.NewPodGroupController(cs.pgInformer, podInformer, cs.pgClient, pgMgr, controllerWorkers)
	return []frameworkext.Controller{podGroupController}, nil
}
