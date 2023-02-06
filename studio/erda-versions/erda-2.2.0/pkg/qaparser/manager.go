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

package qaparser

import (
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/dop/qa/unittest/pb"
	"github.com/erda-project/erda/pkg/qaparser/types"
)

var m Manager

type Parser interface {
	Parse(endpoint, ak, sk, bucket, objectName string) ([]*pb.TestSuite, error)
	Register()
}

type Manager struct {
	parsers map[types.TestParserType]Parser
}

func GetManager() *Manager {
	return &m
}

func init() {
	m = Manager{
		parsers: map[types.TestParserType]Parser{},
	}

	logrus.Info(">>> init parser manager finished <<<")
}

func (m *Manager) GetParser(t types.TestParserType) Parser {

	if _, ok := m.parsers[t]; !ok {
		logrus.Errorf(">>> not found test type=%s <<<", t)
	}
	return m.parsers[t]
}

func Register(p Parser, types ...types.TestParserType) error {
	for _, t := range types {
		if _, ok := m.parsers[t]; ok {
			return errors.Errorf("duplicated type=%s", t.TPValue())
		}
		logrus.Infof(">>> register type=%s to parser manager success <<<", t)
		m.parsers[t] = p
	}

	return nil
}
