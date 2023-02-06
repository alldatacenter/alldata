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

// Package constraintbuilders Generate various executor specific constraints (constraints) according to scheduleInfo
package constraintbuilders

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/constraints"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/k8s"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/marathon"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/metronome"
)

type constraintBuilder interface {
	// Build parameters:
	// podlabels: Used to set podAntiAffinity, for marathon, this parameter has no meaning
	Build(scheduleInfo *apistructs.ScheduleInfo2, service *apistructs.Service, podlabels []constraints.PodLabelsForAffinity, hostnameUtil constraints.HostnameUtil) constraints.Constraints
}

var (
	k8sBuilder       constraintBuilder = &k8s.Builder{}
	marathonBuilder  constraintBuilder = &marathon.Builder{}
	metronomeBuilder constraintBuilder = &metronome.Builder{}
)

// K8S build k8s constraints
func K8S(s *apistructs.ScheduleInfo2, service *apistructs.Service, podlabels []constraints.PodLabelsForAffinity, hostnameUtil constraints.HostnameUtil) *k8s.Constraints {
	return k8sBuilder.Build(s, service, podlabels, hostnameUtil).(*k8s.Constraints)
}

// Marathon build marathon constraints
func Marathon(s *apistructs.ScheduleInfo2, service *apistructs.Service) *marathon.Constraints {
	return marathonBuilder.Build(s, service, nil, nil).(*marathon.Constraints)
}

// Metronome build metronome constraints
func Metronome(s *apistructs.ScheduleInfo2, service *apistructs.Service) *metronome.Constraints {
	return metronomeBuilder.Build(s, service, nil, nil).(*metronome.Constraints)
}
