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

package k8s

import (
	k8s "k8s.io/api/core/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders/constraints"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// e.g. dice/org=1
	labelPrefix     = labelconfig.K8SLabelPrefix
	labelBigdataJob = "bigdata-job"
)

// Constraints k8s constraints
type Constraints struct {
	k8s.Affinity
}

func (*Constraints) IsConstraints() {}

type Builder struct{}

// +------------------------------------------------------------------+
// | labels affect service & job & platform:                          |
// |                         locked, location                         |
// |+-------------------------------------------------+  +-----------+|
// ||  Labels affect service and job:                 |  | [platform]||
// || org                                             |  |           ||
// ||                                                 |  |LABELS:    ||
// ||+---------------++--------------++--------------+|  | *platform ||
// |||[ statefulSvc ]||[statelessSvc]||   [job]      ||  |           ||
// |||               ||              ||              ||  |           ||
// |||LABELS:        ||LABELS:       ||LABELS:       ||  |           ||
// ||| workspace     ||  workspace   ||   *job       ||  |           ||
// ||| *stateful-svc ||*stateless-svc||              ||  |           ||
// |||               ||              ||              ||  |           ||
// |||               ||              ||              ||  |           ||
// ||+---------------++--------------++--------------+|  |           ||
// ||+---------------++--------------++--------------+|  |           ||
// |||[bigdata job]  ||[pack job]    || [daemonset]  ||  |           ||
// |||LABELS:        ||LABELS:       || LABELS:      ||  |           ||
// ||| *bigdata-job  || *pack-job    ||              ||  |           ||
// |||               ||              ||              ||  |           ||
// |||               ||              ||              ||  |           ||
// ||+---------------++--------------++--------------+|  |           ||
// |+-------------------------------------------------+  +-----------+|
// +------------------------------------------------------------------+
// A total of 7 types of nodes, 'stateful-service','stateless-service','job','bigdata-job','pack-job','platform','daemonset'
// A _node_ can belong to several types at the same time

// Build impl constraintBuilder interface
func (Builder) Build(s *apistructs.ScheduleInfo2, service *apistructs.Service, podLabels []constraints.PodLabelsForAffinity, hostnameUtil constraints.HostnameUtil) constraints.Constraints {
	cons := &Constraints{
		Affinity: k8s.Affinity{},
	}
	initConstraints(cons)

	buildStatefulServiceAffinity(s, cons, service)
	buildStatelessServiceAffinity(s, cons, service)
	buildJobAffinity(s, cons, service)
	buildBigdataJobAffinity(s, cons, service)
	buildPackJobAffinity(s, cons, service)
	buildPlatformAffinity(s, cons, service)
	buildDaemonsetAffinity(s, cons, service)

	// decentralized job deployments
	buildJobAntiAffinity(s.Job || s.BigData || s.Pack, cons)

	// decentralized service deployments
	buildServiceAntiAffinity(podLabels, cons)

	buildSpecificHost(s.SpecificHost, cons, hostnameUtil)

	if len(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms) == 0 {
		cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution = nil
	}
	if len(cons.NodeAffinity.PreferredDuringSchedulingIgnoredDuringExecution) == 0 {
		cons.NodeAffinity.PreferredDuringSchedulingIgnoredDuringExecution = nil
	}

	return cons
}

func buildDaemonsetAffinity(s *apistructs.ScheduleInfo2, cons *Constraints, service *apistructs.Service) {
	if !isDaemonset(s) {
		return
	}
	terms := &(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)
	isUnlockTerm := buildIsUnlocked(s.IsUnLocked)
	locationTerms := buildLocation(s.Location, service)
	orgTerm := buildOrg(s.HasOrg, s.Org)
	workspaceTerms := buildWorkspace(s.HasWorkSpace, s.WorkSpaces)

	for _, locationTerm := range locationTerms {
		for _, workspaceTerm := range workspaceTerms {
			requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, workspaceTerm)
			*terms = append(*terms, requiredTerms)
		}
		if !s.HasWorkSpace {
			requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
			*terms = append(*terms, requiredTerms)
		}
	}
}

// Note: this is an intentional limitation by k8s, only allow targeting single nodes.
// requiredDuringSchedulingIgnoredDuringExecution:
//   nodeSelectorTerms:
//   - matchFields:
//     - key: metadata.name
//       operator: In
//       values:
//       - node-010168000080
func buildSpecificHost(specificHosts []string, cons *Constraints, hostnameUtil constraints.HostnameUtil) {
	if len(specificHosts) == 0 || hostnameUtil == nil {
		return
	}
	terms := &(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)

	host := hostnameUtil.IPToHostname(specificHosts[0])
	if host != "" {
		*terms = []k8s.NodeSelectorTerm{{MatchFields: []k8s.NodeSelectorRequirement{
			{
				Key:      "metadata.name",
				Operator: "In",
				Values:   []string{host},
			},
		}}}
	}
}
func buildStatefulServiceAffinity(s *apistructs.ScheduleInfo2, cons *Constraints, service *apistructs.Service) {
	if !isStateful(s) {
		return
	}
	terms := &(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)

	isUnlockTerm := buildIsUnlocked(s.IsUnLocked)
	locationTerms := buildLocation(s.Location, service)
	orgTerm := buildOrg(s.HasOrg, s.Org)
	workspaceTerms := buildWorkspace(s.HasWorkSpace, s.WorkSpaces)
	statefulServiceTerm := buildStatefulService(s)

	for _, locationTerm := range locationTerms {
		for _, workspaceTerm := range workspaceTerms {
			requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, workspaceTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, statefulServiceTerm)
			*terms = append(*terms, requiredTerms)
		}
		if !s.HasWorkSpace {
			requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
			// requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, workspaceTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, statefulServiceTerm)
			*terms = append(*terms, requiredTerms)
		}
	}
}

func buildStatelessServiceAffinity(s *apistructs.ScheduleInfo2, cons *Constraints, service *apistructs.Service) {
	if !isStateless(s) {
		return
	}
	terms := &(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)

	isUnlockTerm := buildIsUnlocked(s.IsUnLocked)
	locationTerms := buildLocation(s.Location, service)
	orgTerm := buildOrg(s.HasOrg, s.Org)
	workspaceTerms := buildWorkspace(s.HasWorkSpace, s.WorkSpaces)
	statelessServiceTerm := buildStatelessService(s)

	for _, locationTerm := range locationTerms {
		for _, workspaceTerm := range workspaceTerms {
			requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, workspaceTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, statelessServiceTerm)
			*terms = append(*terms, requiredTerms)
		}
		if !s.HasWorkSpace {
			requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
			// requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, workspaceTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, statelessServiceTerm)
			*terms = append(*terms, requiredTerms)
		}
	}
}

func buildJobAffinity(s *apistructs.ScheduleInfo2, cons *Constraints, service *apistructs.Service) {
	if !s.Job {
		return
	}
	if s.Job && s.IsPlatform { // platform的job, buildPlatformaffinity 处理
		return
	}
	terms := &(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)

	isUnlockTerm := buildIsUnlocked(s.IsUnLocked)
	locationTerms := buildLocation(s.Location, service)
	orgTerm := buildOrg(s.HasOrg, s.Org)
	jobTerm := buildJob(s)
	for _, locationTerm := range locationTerms {
		requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, jobTerm)
		*terms = append(*terms, requiredTerms)
	}
}

func buildBigdataJobAffinity(s *apistructs.ScheduleInfo2, cons *Constraints, service *apistructs.Service) {
	if !s.BigData {
		return
	}
	terms := &(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)

	isUnlockTerm := buildIsUnlocked(s.IsUnLocked)
	locationTerms := buildLocation(s.Location, service)
	orgTerm := buildOrg(s.HasOrg, s.Org)
	workspaceTerms := buildWorkspace(s.HasWorkSpace, s.WorkSpaces)
	bigdataJobTerm := buildBigdataJob(s)

	for _, locationTerm := range locationTerms {
		for _, workspaceTerm := range workspaceTerms {
			requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, workspaceTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, bigdataJobTerm...)
			*terms = append(*terms, requiredTerms)
		}
		if !s.HasWorkSpace {
			requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
			requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, bigdataJobTerm...)
			*terms = append(*terms, requiredTerms)
		}
	}
}

func buildPackJobAffinity(s *apistructs.ScheduleInfo2, cons *Constraints, service *apistructs.Service) {
	if !s.Pack {
		return
	}
	terms := &(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)

	isUnlockTerm := buildIsUnlocked(s.IsUnLocked)
	locationTerms := buildLocation(s.Location, service)
	orgTerm := buildOrg(s.HasOrg, s.Org)
	packJobTerm := buildPackJob(s)

	for _, locationTerm := range locationTerms {
		requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, orgTerm...)
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, packJobTerm)
		*terms = append(*terms, requiredTerms)
	}
}

func buildPlatformAffinity(s *apistructs.ScheduleInfo2, cons *Constraints, service *apistructs.Service) {
	if !s.IsPlatform {
		return
	}
	terms := &(cons.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)

	isUnlockTerm := buildIsUnlocked(s.IsUnLocked)
	locationTerms := buildLocation(s.Location, service)
	platformTerm := buildPlatform(s)
	for _, locationTerm := range locationTerms {
		requiredTerms := k8s.NodeSelectorTerm{MatchExpressions: []k8s.NodeSelectorRequirement{}}
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, isUnlockTerm)
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, locationTerm)
		requiredTerms.MatchExpressions = append(requiredTerms.MatchExpressions, platformTerm)
		*terms = append(*terms, requiredTerms)
	}
}

func buildStatefulService(s *apistructs.ScheduleInfo2) k8s.NodeSelectorRequirement {
	return buildAux("stateful-service", isStateful(s))
}

func buildStatelessService(s *apistructs.ScheduleInfo2) k8s.NodeSelectorRequirement {
	return buildAux("stateless-service", isStateless(s))
}

func buildJob(s *apistructs.ScheduleInfo2) k8s.NodeSelectorRequirement {
	return buildAux("job", s.Job)
}

func buildBigdataJob(s *apistructs.ScheduleInfo2) []k8s.NodeSelectorRequirement {
	if len(s.BigDataLabels) == 0 {
		return []k8s.NodeSelectorRequirement{buildAux(labelBigdataJob, s.BigData)}
	}
	selectors := make([]k8s.NodeSelectorRequirement, 0)
	for _, label := range s.BigDataLabels {
		selectors = append(selectors, buildAux(label, s.BigData))
	}
	return selectors
}

func buildPackJob(s *apistructs.ScheduleInfo2) k8s.NodeSelectorRequirement {
	return buildAux("pack-job", s.Pack)
}

func buildPlatform(s *apistructs.ScheduleInfo2) k8s.NodeSelectorRequirement {
	return buildAux("platform", s.IsPlatform)
}

func buildIsUnlocked(unlocked bool) k8s.NodeSelectorRequirement {
	return buildAux("locked", !unlocked)
}

func buildLocation(locations map[string]interface{}, service *apistructs.Service) []k8s.NodeSelectorRequirement {
	var (
		selector diceyml.Selector
		ok       bool
	)
	if service != nil {
		selector, ok = locations[service.Name].(diceyml.Selector)
	}
	switch {
	case !ok || len(selector.Values) == 0:
		return []k8s.NodeSelectorRequirement{
			{
				Key:      labelPrefix + "location",
				Operator: k8s.NodeSelectorOpDoesNotExist,
			},
		}
	case selector.Not:
		return []k8s.NodeSelectorRequirement{
			{
				// see also diceyml.Selector comments

				// len(selector.Values) > 0, see the previous condition
				Key:      strutil.Concat(labelPrefix, "location-", selector.Values[0]),
				Operator: k8s.NodeSelectorOpDoesNotExist,
			},
		}
	default: // selector.Not == false
		terms := []k8s.NodeSelectorRequirement{}
		for _, v := range selector.Values {
			terms = append(terms, k8s.NodeSelectorRequirement{
				Key:      strutil.Concat(labelPrefix, "location-", v),
				Operator: k8s.NodeSelectorOpExists,
			})
		}
		return terms
	}
}

func buildOrg(hasOrg bool, org string) []k8s.NodeSelectorRequirement {
	if !hasOrg {
		return []k8s.NodeSelectorRequirement{}
	}
	return []k8s.NodeSelectorRequirement{
		{
			Key:      strutil.Concat(labelPrefix, "org-", org),
			Operator: k8s.NodeSelectorOpExists,
		},
	}
}

func buildWorkspace(hasWorkspace bool, workspaces []string) []k8s.NodeSelectorRequirement {
	if !hasWorkspace {
		return []k8s.NodeSelectorRequirement{}
	}
	terms := []k8s.NodeSelectorRequirement{}
	for _, ws := range workspaces {
		terms = append(terms, k8s.NodeSelectorRequirement{
			Key:      strutil.Concat(labelPrefix, "workspace-", ws),
			Operator: k8s.NodeSelectorOpExists,
		})
	}
	return terms
}

func buildBigData(bigdata bool) k8s.NodeSelectorRequirement {
	return buildAux("bigdata", bigdata)
}

func buildPack(pack bool) k8s.NodeSelectorRequirement {
	return buildAux("pack", pack)
}

// buildAux `exist' = true, add constraints 'LABEL exist'
// `exist' = false, add constraints 'LABEL notexist'
func buildAux(label string, exist bool) k8s.NodeSelectorRequirement {
	return k8s.NodeSelectorRequirement{
		Key: labelPrefix + label,
		Operator: map[bool]k8s.NodeSelectorOperator{
			true:  k8s.NodeSelectorOpExists,
			false: k8s.NodeSelectorOpDoesNotExist,
		}[exist],
	}
}

func buildJobAntiAffinity(job bool, cons *Constraints) {
	terms := &(cons.PodAntiAffinity.PreferredDuringSchedulingIgnoredDuringExecution)
	if job {
		*terms = append(*terms, k8s.WeightedPodAffinityTerm{
			Weight: 100,
			PodAffinityTerm: k8s.PodAffinityTerm{
				LabelSelector: &v1.LabelSelector{
					MatchExpressions: []v1.LabelSelectorRequirement{
						{
							Key:      labelPrefix + "job",
							Operator: v1.LabelSelectorOpExists,
						},
					},
				},
				TopologyKey: "kubernetes.io/hostname",
			},
		})
	}
}

func buildServiceAntiAffinity(podLabellist []constraints.PodLabelsForAffinity, cons *Constraints) {
	preferredTerms := &(cons.PodAntiAffinity.PreferredDuringSchedulingIgnoredDuringExecution)
	requiredTerms := &(cons.PodAntiAffinity.RequiredDuringSchedulingIgnoredDuringExecution)
	for _, podlabels := range podLabellist {
		for lk, lv := range podlabels.PodLabels {
			podAffinityTerm1 := k8s.PodAffinityTerm{
				LabelSelector: &v1.LabelSelector{
					MatchExpressions: []v1.LabelSelectorRequirement{
						{
							Key:      lk,
							Operator: v1.LabelSelectorOpIn,
							Values:   []string{lv},
						},
					},
				},
				TopologyKey: "dice/topology-zone",
			}
			podAffinityTerm2 := k8s.PodAffinityTerm{
				LabelSelector: &v1.LabelSelector{
					MatchExpressions: []v1.LabelSelectorRequirement{
						{
							Key:      lk,
							Operator: v1.LabelSelectorOpIn,
							Values:   []string{lv},
						},
					},
				},
				TopologyKey: "kubernetes.io/hostname",
			}
			if podlabels.Required {
				*requiredTerms = append(*requiredTerms, podAffinityTerm1, podAffinityTerm2)
			} else {
				*preferredTerms = append(*preferredTerms, k8s.WeightedPodAffinityTerm{
					Weight:          100,
					PodAffinityTerm: podAffinityTerm1,
				}, k8s.WeightedPodAffinityTerm{
					Weight:          100,
					PodAffinityTerm: podAffinityTerm2,
				})
			}
		}
	}
}

func isDaemonset(s *apistructs.ScheduleInfo2) bool {
	return !s.BigData && !s.Pack && !s.Job && !s.IsPlatform && !s.Stateless && !s.Stateful && s.IsDaemonset
}
func isStateful(s *apistructs.ScheduleInfo2) bool {
	return !s.BigData && !s.Pack && !s.Job && !s.IsPlatform && !s.Stateless && !s.IsDaemonset && s.Stateful
}
func isStateless(s *apistructs.ScheduleInfo2) bool {
	return !s.BigData && !s.Pack && !s.Job && !s.IsPlatform && !s.Stateful && !s.IsDaemonset && s.Stateless
}

func initConstraints(cons *Constraints) {
	if cons.Affinity.NodeAffinity != nil {
		return
	}
	cons.Affinity.NodeAffinity = &k8s.NodeAffinity{
		RequiredDuringSchedulingIgnoredDuringExecution: &k8s.NodeSelector{
			NodeSelectorTerms: []k8s.NodeSelectorTerm{},
		},
	}
	cons.Affinity.PodAntiAffinity = &k8s.PodAntiAffinity{
		PreferredDuringSchedulingIgnoredDuringExecution: []k8s.WeightedPodAffinityTerm{},
		RequiredDuringSchedulingIgnoredDuringExecution:  []k8s.PodAffinityTerm{},
	}

}
