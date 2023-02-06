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
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"
)

type VersionVisitor struct{}

func NewVersionVisitor() *VersionVisitor {
	return &VersionVisitor{}
}

func (v *VersionVisitor) Visit(s *Spec) {
	switch s.Version {
	case "":
		s.appendError(errors.New("no version"))
	case Version1dot1:
		return
	default:
		s.appendError(errors.Errorf("invalid version: %s, only support 1.1", s.Version))
	}
}

func GetVersion(data []byte) (string, error) {
	type version struct {
		Version string `yaml:"version"`
	}
	var ver version
	if err := yaml.Unmarshal(data, &ver); err != nil {
		return "", err
	}
	return ver.Version, nil
}
