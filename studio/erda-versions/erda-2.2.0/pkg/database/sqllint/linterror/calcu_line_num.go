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

package linterror

import (
	"bufio"
	"bytes"
)

// CalcLintLine calculates the lint error line number of the SQL script
func CalcLintLine(source, scope []byte, goal func(line []byte) bool) (line string, num int) {
	var firstLine []byte
	scanner := bufio.NewScanner(bytes.NewBuffer(scope))
	for scanner.Scan() {
		line := scanner.Bytes()
		if len(bytes.TrimPrefix(line, []byte(" "))) > 0 {
			firstLine = line
			break
		}
	}

	idx := bytes.Index(source, scope)
	if idx < 0 {
		return "", -1
	}
	var (
		i       = 0
		lineNum = 0
		minNum  = 0
	)
	scanner = bufio.NewScanner(bytes.NewBuffer(source))
	for scanner.Scan() {
		line := scanner.Bytes()
		i += len(line) + 1
		lineNum++
		if i < idx {
			continue
		}
		if bytes.Equal(line, firstLine) {
			minNum = lineNum
		}
		if goal(line) && !bytes.HasPrefix(bytes.TrimSpace(line), []byte("--")) {
			return string(line), lineNum
		}
	}
	return string(firstLine), minNum
}
