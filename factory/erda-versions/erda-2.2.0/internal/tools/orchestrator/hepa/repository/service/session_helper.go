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
	"github.com/xormplus/xorm"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

type SessionHelper struct {
	session   *xorm.Session
	completed bool
	closed    bool
}

func NewSessionHelper() (*SessionHelper, error) {
	engine, err := orm.GetSingleton()
	if err != nil {
		return nil, errors.WithStack(err)
	}
	session := engine.NewSession()
	err = session.Begin()
	if err != nil {
		session.Close()
		return nil, errors.WithStack(err)
	}
	return &SessionHelper{session, false, false}, nil
}

func (impl *SessionHelper) GetSessionHelper() *SessionHelper {
	return impl
}

func (impl *SessionHelper) Session() *xorm.Session { return impl.session }

func (impl *SessionHelper) Begin() error {
	if impl.closed {
		return errors.New("session already closed")
	}
	if !impl.completed {
		return errors.New("last trans not completed")
	}
	err := impl.session.Begin()
	if err != nil {
		impl.session.Close()
		return errors.WithStack(err)
	}
	impl.completed = false
	return nil
}

func (impl *SessionHelper) Commit() error {
	if impl.completed {
		return nil
	}
	err := impl.session.Commit()
	if err != nil {
		return errors.WithStack(err)
	}
	impl.completed = true
	return nil
}

func (impl *SessionHelper) Rollback() error {
	if impl.completed {
		return nil
	}
	err := impl.session.Rollback()
	if err != nil {
		return errors.WithStack(err)
	}
	impl.completed = true
	return nil
}

func (impl *SessionHelper) Close() {
	if impl.closed {
		return
	}
	impl.session.Close()
	impl.completed = true
	impl.closed = true
}
