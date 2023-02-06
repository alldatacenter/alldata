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

// func TestCreateBuiltinHooks(t *testing.T) {
// 	impl, err := NewWebHookImpl()
// 	assert.Nil(t, err)
// 	assert.Nil(t, createIfNotExist(impl, &CreateHookRequest{
// 		Name:   "test-builtin-hook",
// 		Events: []string{"test-event"},
// 		URL:    "http://xxx/a",
// 		Active: true,
// 		HookLocation: HookLocation{
// 			Org:         "-1",
// 			Project:     "-1",
// 			Application: "-1",
// 		},
// 	}))
// }

// func TestMakeSureBuiltinHooks(t *testing.T) {
// 	impl, err := NewWebHookImpl()
// 	assert.Nil(t, err)
// 	assert.Nil(t, MakeSureBuiltinHooks(impl))
// }
