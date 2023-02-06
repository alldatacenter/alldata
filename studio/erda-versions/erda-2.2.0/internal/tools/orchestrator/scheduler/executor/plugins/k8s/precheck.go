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
	"fmt"
	"sort"

	v1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	statusUnschedulable = "unschedulable"
)

func precheck(sg *apistructs.ServiceGroup, resourceinfo apistructs.ClusterResourceInfoData) (
	apistructs.ServiceGroupPrecheckData, error) {
	r := map[string][]apistructs.ServiceGroupPrecheckNodeData{}
	serviceAffinityMap := map[string]string{}
	for _, svc := range sg.Services {
		cons := constraintbuilders.K8S(&sg.ScheduleInfo2, &svc, nil, nil)
		svclabels := extractLabels(cons.Affinity.NodeAffinity.RequiredDuringSchedulingIgnoredDuringExecution.NodeSelectorTerms)
		serviceAffinityMap[svc.Name] = pp(svclabels)
		for ip, node := range resourceinfo.Nodes {
			if !matchNodeLabels(node.Labels, svclabels) {
				r[svc.Name] = append(r[svc.Name],
					apistructs.ServiceGroupPrecheckNodeData{
						IP:     ip,
						Status: statusUnschedulable,
						Info: fmt.Sprintf("标签限制: [%s], 节点标签: %v",
							pp(svclabels), node.Labels),
					})
			} else {
				r[svc.Name] = append(r[svc.Name],
					apistructs.ServiceGroupPrecheckNodeData{
						IP:     ip,
						Status: "ok",
					})
			}
		}
	}
	infoList := []string{}
	statusList := []string{}
	for service, results := range r {
		status := statusUnschedulable
		for _, result := range results {
			if result.Status == "ok" {
				status = "ok"
			}
		}
		if status != "ok" {
			infoList = append(infoList, fmt.Sprintf("%s: %s", service, serviceAffinityMap[service]))
		}
		statusList = append(statusList, status)
	}
	resultStatus := "ok"
	for _, status := range statusList {
		if status != "ok" {
			resultStatus = statusUnschedulable
		}
	}
	info := ""
	if resultStatus == statusUnschedulable {
		info = strutil.Join(infoList, "\n", true)
	}
	return apistructs.ServiceGroupPrecheckData{
		Nodes:  r,
		Status: resultStatus,
		Info:   info,
	}, nil
}

// extractLabels
// return [(exist_label_list, not_exist_label_list), (exist_label_list, not_exist_label_list),...]
// The relationship between different tuples is or
func extractLabels(terms []v1.NodeSelectorTerm) [][2][]string {
	r := [][2][]string{}
	for _, t := range terms {
		exist := []string{}
		notexist := []string{}
		for _, expr := range t.MatchExpressions {
			if expr.Operator == "Exists" {
				exist = append(exist, expr.Key)
			} else if expr.Operator == "DoesNotExist" {
				notexist = append(notexist, expr.Key)
			}
		}
		r = append(r, [2][]string{exist, notexist})
	}
	return r
}

func matchNodeLabels(nodelabels []string, svclabels [][2][]string) bool {
	sort.Strings(nodelabels)
	for _, labelTuple := range svclabels {
		succ := true
		for _, exist := range labelTuple[0] {
			i := sort.SearchStrings(nodelabels, exist)
			if i == len(nodelabels) || nodelabels[i] != exist {
				succ = false
			}
		}
		for _, notexist := range labelTuple[1] {
			i := sort.SearchStrings(nodelabels, notexist)
			if i != len(nodelabels) && nodelabels[i] == notexist {
				succ = false
			}
		}
		if succ {
			return true
		}
	}
	return false
}

func pp(svclabels [][2][]string) string {
	descs := []string{}
	for _, labelTuple := range svclabels {
		desc := fmt.Sprintf("需要存在标签: %v, 需要不存在标签: %v", labelTuple[0], labelTuple[1])
		descs = append(descs, desc)
	}

	return strutil.Join(descs, " OR ")
}
