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

type EnvInsertVisitor struct {
	envs map[string]string
}

func NewEnvInsertVisitor(envs map[string]string) *EnvInsertVisitor {
	return &EnvInsertVisitor{envs: envs}
}

func (v *EnvInsertVisitor) Visit(s *Spec) {
	if v.envs == nil {
		return
	}

	if s.Envs == nil {
		s.Envs = make(map[string]string)
	}

	// insert into struct
	for k, v := range v.envs {
		s.Envs[k] = v
	}

	// encode to new yaml
	_, err := GenerateYml(s)
	if err != nil {
		s.appendError(err)
		return
	}
}
