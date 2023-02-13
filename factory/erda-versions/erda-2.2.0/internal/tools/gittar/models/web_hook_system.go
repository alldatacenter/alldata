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

package models

func (svc *Service) GetSystemHooksByEvent(event string, active bool) ([]WebHook, error) {
	var hooks []WebHook
	err := svc.db.Where("hook_type=? AND "+event+"=1 AND is_active=?",
		HOOK_TYPE_SYSTEM, active).Find(&hooks).Error

	if err != nil {
		return nil, err
	}
	return hooks, nil
}

func (svc *Service) AddSystemHook(hook *WebHook) (*WebHook, error) {
	hook.HookType = HOOK_TYPE_SYSTEM
	//命名hook,检测同名,存在只做更新
	if hook.Name != "" {
		var currentHook WebHook
		err := svc.db.Where("hook_type=?  AND name=?",
			HOOK_TYPE_SYSTEM, hook.Name).First(&currentHook).Error
		if err != nil {
			err := svc.db.Create(hook).Error
			return hook, err
		} else {
			currentHook.Url = hook.Url
			currentHook.PushEvents = hook.PushEvents
			currentHook.IsActive = hook.IsActive
			err := svc.db.Save(&currentHook).Error
			return &currentHook, err
		}
	} else {
		//匿名hook直接添加
		err := svc.db.Create(hook).Error
		return hook, err
	}
}
