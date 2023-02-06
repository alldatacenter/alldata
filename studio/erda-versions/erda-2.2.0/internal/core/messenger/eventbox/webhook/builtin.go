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

package webhook

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
)

// 如果不存在相同名字的 webhook 则创建
func createIfNotExist(impl *WebHookImpl, req *CreateHookRequest) error {
	if impl == nil {
		return fmt.Errorf("nil webhookImpl")
	}
	resp, err := impl.ListHooks(apistructs.HookLocation{
		Org:         req.Org,
		Project:     req.Project,
		Application: req.Application,
	})
	if err != nil {
		return err
	}
	hooks := []apistructs.Hook(resp)
	for i := range hooks {
		if hooks[i].Name == req.Name {
			return nil
		}
	}
	if _, err := impl.CreateHook(req.Org, *req); err != nil {
		return err
	}
	return nil
}

// MakeSureBuiltinHooks 创建默认 webhook (如果不存在)
func MakeSureBuiltinHooks(impl *WebHookImpl) error {
	hooks := make([]CreateHookRequest, 0)

	for i := range hooks {
		if err := createIfNotExist(impl, &hooks[i]); err != nil {
			return err
		}
	}
	return nil
}
