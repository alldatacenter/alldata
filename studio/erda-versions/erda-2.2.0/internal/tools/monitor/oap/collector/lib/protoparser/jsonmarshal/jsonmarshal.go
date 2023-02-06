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

package jsonmarshal

import (
	"encoding/json"
	"fmt"
	"sync"

	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/common/unmarshalwork"
)

func ParseInterface(src interface{}, callback func(buf []byte) error) error {
	uc := newUnmarshalCtx(src, callback)
	uc.wg.Add(1)
	unmarshalwork.Schedule(uc)
	uc.wg.Wait()
	if uc.err != nil {
		return fmt.Errorf("unmarshal %T: %w", src, uc.err)
	}
	return nil
}

type unmarshalCtx struct {
	src      interface{}
	err      error
	callback func([]byte) error
	wg       sync.WaitGroup
}

func newUnmarshalCtx(src interface{}, callback func(buf []byte) error) *unmarshalCtx {
	return &unmarshalCtx{src: src, callback: callback}
}

func (uc *unmarshalCtx) Unmarshal() {
	defer uc.wg.Done()
	buf, err := json.Marshal(uc.src)
	if err != nil {
		uc.err = fmt.Errorf("unmarshal uc.span: %s", err)
		return
	}
	if err := uc.callback(buf); err != nil {
		uc.err = fmt.Errorf("callback buf: %s", err)
		return
	}
}
