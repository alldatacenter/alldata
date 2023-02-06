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

package pipelineymlv1

import (
	"fmt"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/metadata"
)

func (y *PipelineYml) evaluate(variables []metadata.MetadataField) error {
	rendered, err := RenderPlaceholders(string(y.byteData), variables)
	if err != nil {
		return err
	}
	y.byteData = []byte(rendered)
	return nil
}

func findFirstPlaceholder(line string) (string, bool) {
	// li: left index
	// ri: right index
	for li := 0; li < len(line); li++ {
		var left, right int
		// find first ((
		var findRight bool
		if line[li] == '(' && li+1 < len(line) && line[li+1] == '(' {
			left = li
			// find matched ))
			for ri := li; ri < len(line); ri++ {
				if line[ri] == ')' && ri+1 < len(line) && line[ri+1] == ')' {
					right = ri + 1
					findRight = true
					break
				}
			}
			if findRight {
				return fmt.Sprintf("%s", line[left:right+1]), true
			}
		}
	}
	return "", false
}

func removeComment(line string) (string, string) {
	i := strings.IndexByte(line, '#')
	if i == -1 {
		return line, ""
	}
	return line[:i], line[i:]
}

func RenderPlaceholders(input string, placeholders []metadata.MetadataField) (string, error) {
	lines := strings.Split(input, "\n")

	m := make(map[string]string, len(placeholders))
	for _, ph := range placeholders {
		m[ph.Name] = ph.Value
	}

	for i := range lines {
		// remove comment
		prefix, comment := removeComment(lines[i])

		// 遍历替换一行中的所有 ((placeholder))
		for {
			placeholder, find := findFirstPlaceholder(prefix)
			if !find {
				break
			}
			value, ok := m[placeholder]
			if !ok {
				return "", errors.Errorf("failed to render placeholder: %s at line: %d", placeholder, i+1)
			}
			prefix = strings.Replace(prefix, placeholder, value, 1)
			lines[i] = prefix + comment
		}
	}
	return strings.Join(lines, "\n"), nil
}
