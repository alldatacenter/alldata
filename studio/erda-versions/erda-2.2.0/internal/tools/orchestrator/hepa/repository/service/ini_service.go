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

package service

import (
	"github.com/pkg/errors"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type IniServiceImpl struct {
	engine *orm.OrmEngine
}

func NewIniServiceImpl() (*IniServiceImpl, error) {
	engine, error := orm.GetSingleton()
	if error != nil {
		return nil, errors.Wrap(error, "new IniServiceImpl failed")
	}
	return &IniServiceImpl{engine}, nil
}

func (impl *IniServiceImpl) GetValueByName(name string) (string, error) {
	if len(name) == 0 {
		return "", errors.New(ERR_INVALID_ARG)
	}
	ini := &orm.Ini{}
	succ, err := orm.Get(impl.engine, ini, "ini_name = ?", name)
	if err != nil {
		return "", errors.Wrap(err, ERR_SQL_FAIL)
	}
	if !succ {
		return "", nil
	}
	return ini.IniValue, nil

}
