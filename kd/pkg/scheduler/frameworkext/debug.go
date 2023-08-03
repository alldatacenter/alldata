/*
Copyright 2022 The Koordinator Authors.

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

package frameworkext

import (
	"fmt"
	"sort"
	"strconv"

	prettytable "github.com/jedib0t/go-pretty/v6/table"
	"github.com/spf13/pflag"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"
)

var (
	debugTopNScores    = 0
	debugFilterFailure = false
)

func AddFlags(fs *pflag.FlagSet) {
	fs.IntVarP(&debugTopNScores, "debug-scores", "s", debugTopNScores, "logging topN nodes score and scores for each plugin after running the score extension, disable if set to 0")
	fs.BoolVarP(&debugFilterFailure, "debug-filters", "f", debugFilterFailure, "logging filter failures")
}

// DebugScoresSetter updates debugTopNScores to specified value
func DebugScoresSetter(val string) (string, error) {
	topN, err := strconv.Atoi(val)
	if err != nil {
		return "", fmt.Errorf("failed set debugTopNScores %s: %v", val, err)
	}
	debugTopNScores = topN
	return fmt.Sprintf("successfully set debugTopNScores to %s", val), nil
}

// DebugFiltersSetter updates debugFilterFailure to specified value
func DebugFiltersSetter(val string) (string, error) {
	filterFailure, err := strconv.ParseBool(val)
	if err != nil {
		return "", fmt.Errorf("failed set debugFilterFailure %s: %v", val, err)
	}
	debugFilterFailure = filterFailure
	return fmt.Sprintf("successfully set debugFilterFailure to %s", val), nil
}

func debugScores(topN int, pod *corev1.Pod, pluginToNodeScores map[string]framework.NodeScoreList, nodes []*corev1.Node) prettytable.Writer {
	// Summarize all scores.
	result := make(framework.NodeScoreList, 0, len(nodes))
	nodeOrders := make(map[string]int, len(nodes))

	for i, node := range nodes {
		nodeOrders[node.Name] = i
		result = append(result, framework.NodeScore{Name: node.Name, Score: 0})
		for j := range pluginToNodeScores {
			result[i].Score += pluginToNodeScores[j][i].Score
		}
	}
	sort.Slice(result, func(i, j int) bool {
		return result[i].Score > result[j].Score
	})

	pluginNames := make([]string, 0, len(pluginToNodeScores))
	for name := range pluginToNodeScores {
		pluginNames = append(pluginNames, name)
	}
	sort.Strings(pluginNames)

	w := prettytable.NewWriter()
	headerRow := prettytable.Row{"#", "Pod", "Node", "Score"}
	for _, name := range pluginNames {
		headerRow = append(headerRow, name)
	}
	w.AppendHeader(headerRow)

	podRef := klog.KObj(pod)
	for i, node := range result {
		if i >= topN {
			break
		}
		row := prettytable.Row{strconv.Itoa(i), podRef.String(), node.Name, node.Score}
		if nodeIndex, ok := nodeOrders[node.Name]; ok {
			for _, pluginName := range pluginNames {
				if scores, ok := pluginToNodeScores[pluginName]; ok {
					row = append(row, scores[nodeIndex].Score)
				} else {
					row = append(row, -1)
				}
			}
		}
		w.AppendRow(row)
	}
	klog.Infof("Top%d scores for Pod: %v, feasibleNodes: %v, plugins:%v\n%v", topN, podRef, len(nodes), pluginNames, w.RenderMarkdown())
	return w // return writer for UT
}
