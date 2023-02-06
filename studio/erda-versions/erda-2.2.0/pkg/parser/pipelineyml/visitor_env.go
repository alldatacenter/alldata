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
	"regexp"

	"github.com/pkg/errors"
)

var envKeyRegexp = regexp.MustCompile("^[a-zA-Z_][a-zA-Z0-9_]*$")

type EnvVisitor struct {
	envs map[string]string
}

func NewEnvVisitor(envs map[string]string) *EnvVisitor {
	return &EnvVisitor{envs: envs}
}

func (v *EnvVisitor) Visit(s *Spec) {
	if s.Envs == nil {
		s.Envs = make(map[string]string, 0)
	}

	for k, v := range v.envs {
		s.Envs[k] = v
	}

	checkErr := CheckEnvs(s.Envs)
	for _, err := range checkErr {
		s.appendError(err)
	}
}

func CheckEnvs(envs map[string]string) []error {
	var errs []error
	for k := range envs {
		if !envKeyRegexp.MatchString(k) {
			errs = append(errs, errors.Errorf("invalid env key: %s (must match: %s)", k, envKeyRegexp.String()))
		}
	}
	return errs
}
