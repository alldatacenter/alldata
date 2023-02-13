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

package pipelineyml

import (
	"strings"

	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/expression"
)

type LabelsVisitor struct {
	data   []byte
	labels map[string]string
}

func NewLabelsVisitor(data []byte, labels map[string]string) *LabelsVisitor {
	v := LabelsVisitor{}
	v.data = data
	v.labels = labels
	return &v
}

// yaml global text replacement
func (v *LabelsVisitor) Visit(s *Spec) {
	var (
		replaced = v.data
	)
	replaced, _ = RenderLabels(v.data, v.labels)
	v.data = replaced
	// Use the rendered data to deserialize to ensure that no hints are lost, for example: !!str
	if err := yaml.Unmarshal(replaced, s); err != nil {
		s.appendError(err)
	}
}

func RenderLabels(input []byte, labels map[string]string) ([]byte, error) {
	replaced := string(input)
	// replace ${{ triggers.event.label }}
	for k, v := range labels {
		ss := strings.SplitN(k, ".", 4)
		if len(ss) != 3 {
			continue
		}
		if ss[0] != expression.TriggerLabel {
			continue
		}
		oldString := expression.LeftPlaceholder + " " + expression.TriggerLabel + "." + ss[1] + "." + ss[2] + " " + expression.RightPlaceholder
		replaced = strings.ReplaceAll(replaced, oldString, v)
	}
	return []byte(replaced), nil
}
