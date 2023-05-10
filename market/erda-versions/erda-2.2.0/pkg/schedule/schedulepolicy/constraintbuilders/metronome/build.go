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

package metronome

import (
	"github.com/erda-project/erda/apistructs"
	constraints2 "github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/constraints"
	marathon2 "github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/marathon"
)

// Constraints  metronome constraints
type Constraints = marathon2.Constraints

type Builder struct {
	marathon marathon2.Builder
}

func (b *Builder) Build(s *apistructs.ScheduleInfo2, service *apistructs.Service, _ []constraints2.PodLabelsForAffinity, _ constraints2.HostnameUtil) constraints2.Constraints {
	return b.Build(s, service, nil, nil)
}
