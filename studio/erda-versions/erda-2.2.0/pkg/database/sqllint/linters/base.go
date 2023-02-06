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

package linters

import (
	"reflect"
	"sync"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

var (
	// factory hub singleton, it make sure every method will be registered once.
	h    hub
	once sync.Once
)

func init() {
	once.Do(register)
}

type baseLinter struct {
	s    script.Script
	err  error
	text string
}

func (b baseLinter) Error() error {
	return b.err
}

func newBaseLinter(script script.Script) baseLinter {
	return baseLinter{s: script}
}

// hub implements rules.Factory
type hub struct{}

// register all rules.Factory into linters
func register() {
	valueOf := reflect.ValueOf(h)
	typeOf := reflect.TypeOf(h)
	for i := 0; i < valueOf.NumMethod(); i++ {
		method := valueOf.Method(i)
		v := method.Interface()
		if f, ok := v.(func(script.Script, sqllint.Config) (sqllint.Rule, error)); ok {
			sqllint.Register(typeOf.Method(i).Name, f)
		}
	}
}
