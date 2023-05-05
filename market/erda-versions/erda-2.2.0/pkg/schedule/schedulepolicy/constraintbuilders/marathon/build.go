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

package marathon

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/constraints"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
	"github.com/erda-project/erda/pkg/strutil"
)

type constraintsOp string

const (
	constraintsOpLike   constraintsOp = "LIKE"
	constraintsOpUnLike constraintsOp = "UNLIKE"
)

type constraintTerm struct {
	Key    string
	Op     constraintsOp
	Values []string
}

// Constraints marathon or metronome constraints
type Constraints struct {
	terms []constraintTerm
	// Cons Constraints used by marathon
	Cons [][]string
}

func (Constraints) IsConstraints() {}

type Builder struct{}

// Build build marathon constraints
func (Builder) Build(s *apistructs.ScheduleInfo2, service *apistructs.Service, _ []constraints.PodLabelsForAffinity, _ constraints.HostnameUtil) constraints.Constraints {
	cons := &Constraints{
		terms: []constraintTerm{},
	}
	buildSpecificHost(s.SpecificHost, cons)
	buildPlatform(s.IsPlatform, cons)
	buildUnlocked(s.IsUnLocked, cons)
	buildLocation(s.Location, service, cons)
	buildOrg(s.HasOrg, s.Org, cons)
	buildWorkspace(s.HasWorkSpace, s.WorkSpaces, cons)
	buildJob(s.Job, s.PreferJob, cons)
	// metronome Do not consider the pack standard
	// buildPack(s.Pack, s.PreferPack, cons)
	buildStateful(s.Stateful, s.PreferStateful, cons)
	buildStateless(s.Stateless, s.PreferStateless, cons)
	buildBigdata(s.BigData, cons)
	buildProject(s.HasProject, s.Project, cons)

	for _, t := range cons.terms {
		cons.Cons = append(cons.Cons, t.generate())
	}
	return cons
}

func buildSpecificHost(specificHosts []string, cons *Constraints) {
	if len(specificHosts) == 0 {
		return
	}
	terms := &cons.terms
	*terms = append(*terms, constraintTerm{
		Key:    "hostname",
		Op:     constraintsOpLike,
		Values: specificHosts,
	})
}

func buildPlatform(platform bool, cons *Constraints) {
	terms := &cons.terms
	*terms = append(*terms, constraintTerm{
		Key: labelconfig.DCOS_ATTRIBUTE,
		Op: map[bool]constraintsOp{
			true:  constraintsOpLike,
			false: constraintsOpUnLike}[platform],
		Values: []string{"platform"},
	})
}

func buildUnlocked(unlocked bool, cons *Constraints) {
	terms := &cons.terms
	*terms = append(*terms, constraintTerm{
		Key: labelconfig.DCOS_ATTRIBUTE,
		Op: map[bool]constraintsOp{
			true:  constraintsOpLike,
			false: constraintsOpUnLike}[!unlocked],
		Values: []string{"locked"},
	})
}

func buildLocation(locations map[string]interface{}, service *apistructs.Service, cons *Constraints) {
	terms := &cons.terms
	var (
		ok       bool
		selector diceyml.Selector
	)

	if service != nil {
		selector, ok = locations[service.Name].(diceyml.Selector)
	}
	switch {
	case !ok || len(selector.Values) == 0:
		*terms = append(*terms, constraintTerm{
			Key:    labelconfig.DCOS_ATTRIBUTE,
			Op:     constraintsOpUnLike,
			Values: []string{`location-[^,]+`}, // prefix of location

		})
	case selector.Not:
		*terms = append(*terms, constraintTerm{
			Key:    labelconfig.DCOS_ATTRIBUTE,
			Op:     constraintsOpUnLike,
			Values: []string{strutil.Concat("location-", selector.Values[0])}, // see also diceyml.Selector comments
		})
	default:
		*terms = append(*terms, constraintTerm{
			Key: labelconfig.DCOS_ATTRIBUTE,
			Op:  constraintsOpLike,
			Values: strutil.Map(selector.Values, func(s string) string {
				return strutil.Concat("location-", s)
			}),
		})
	}
}

func buildOrg(hasorg bool, org string, cons *Constraints) {
	terms := &cons.terms
	if !hasorg {
		*terms = append(*terms, constraintTerm{
			Key:    labelconfig.DCOS_ATTRIBUTE,
			Op:     constraintsOpUnLike,
			Values: []string{`org-[^,]+`}, // prefix of org
		})
		return
	}
	*terms = append(*terms, constraintTerm{
		Key:    labelconfig.DCOS_ATTRIBUTE,
		Op:     constraintsOpLike,
		Values: []string{strutil.Concat("org-", org)},
	})
}

func buildWorkspace(hasworkspace bool, workspaces []string, cons *Constraints) {
	terms := &cons.terms
	if !hasworkspace {
		*terms = append(*terms, constraintTerm{
			Key:    labelconfig.DCOS_ATTRIBUTE,
			Op:     constraintsOpUnLike,
			Values: []string{`workspace-[^,]+`},
		})
		return
	}
	*terms = append(*terms, constraintTerm{
		Key: labelconfig.DCOS_ATTRIBUTE,
		Op:  constraintsOpLike,
		Values: strutil.Map(workspaces, func(s string) string {
			return strutil.Concat("workspace-", s)
		}),
	})
}

func buildJob(job, prefer bool, cons *Constraints) {
	buildAux("job", job, prefer, cons)
}

func buildPack(pack, prefer bool, cons *Constraints) {
	buildAux("pack", pack, prefer, cons)
}

func buildStateful(stateful, prefer bool, cons *Constraints) {
	buildAux("stateful", stateful, prefer, cons)
}

func buildStateless(stateless, prefer bool, cons *Constraints) {
	buildAux("stateless", stateless, prefer, cons)
}

func buildBigdata(bigdata bool, cons *Constraints) {
	buildAux("bigdata", bigdata, false, cons)
}

func buildProject(hasproject bool, project string, cons *Constraints) {
	terms := &cons.terms
	if !hasproject {
		*terms = append(*terms, constraintTerm{
			Key:    labelconfig.DCOS_ATTRIBUTE,
			Op:     constraintsOpUnLike,
			Values: []string{`project-[^,]+`},
		})
		return
	}
	*terms = append(*terms, constraintTerm{
		Key:    labelconfig.DCOS_ATTRIBUTE,
		Op:     constraintsOpLike,
		Values: []string{strutil.Concat("project-", project)},
	})
}

// add dice_tag LIKE `label' when buildAux `exist' = true,
// No constraint is added when `exist' = false
// `exist' = true && prefer = true 时 dice_tag LIKE (`label' or `any')
func buildAux(label string, exist, prefer bool, cons *Constraints) {
	if !exist {
		return
	}
	terms := &cons.terms
	values := []string{label}
	if prefer {
		values = append(values, "any")
	}
	*terms = append(*terms, constraintTerm{
		Key:    labelconfig.DCOS_ATTRIBUTE,
		Op:     constraintsOpLike,
		Values: values,
	})
}
