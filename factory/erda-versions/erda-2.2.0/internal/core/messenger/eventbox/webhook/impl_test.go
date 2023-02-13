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
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRemoveEvents(t *testing.T) {
	assert.Equal(t, []string{"a"}, removeEvents([]string{"a", "b", "c", "c"}, []string{"c", "b"}))
	assert.Equal(t, []string{"a", "b", "c", "c"}, removeEvents([]string{"a", "b", "c", "c"}, []string{"u"}))
}

func TestAddEvents(t *testing.T) {
	assert.Equal(t, []string{"a", "c", "d"}, addEvents([]string{"a", "c"}, []string{"c", "c", "d"}))
}

// func TestCreateHook(t *testing.T) {
// 	impl, _ := NewWebHookImpl()
// 	r, err := impl.CreateHook("6", CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org:         "6",
// 			Project:     "66",
// 			Application: "666",
// 		},
// 		Name:   "test-hook",
// 		Events: []string{"e1", "e2"},
// 		URL:    "http://xx",
// 		Active: true,
// 	})
// 	defer del(string(r))
// 	assert.Nil(t, err)
// 	assert.NotEqual(t, "", r)
// }

// func TestDeleteHook(t *testing.T) {
// 	org, id := create(CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org: "3", Project: "33", Application: "333",
// 		},
// 		Name: "666"})
// 	defer del(id)
// 	impl, _ := NewWebHookImpl()
// 	assert.Nil(t, impl.DeleteHook(org, id))
// }

// func TestEditHook(t *testing.T) {
// 	org, id := create(CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org:         "3",
// 			Project:     "33",
// 			Application: "333",
// 		},
// 		Name: "777"})
// 	defer del(id)
// 	impl, _ := NewWebHookImpl()
// 	_, err := impl.EditHook(org, id, EditHookRequest{
// 		Events: []string{"e1", "e2"},
// 	})
// 	assert.Nil(t, err)
// 	h1 := get(org, id)
// 	assert.Equal(t, []string{"e1", "e2"}, h1.Events)
// 	_, err = impl.EditHook(org, id, EditHookRequest{
// 		RemoveEvents: []string{"e1"},
// 	})
// 	assert.Nil(t, err)
// 	h2 := get(org, id)
// 	assert.Equal(t, []string{"e2"}, h2.Events)

// 	_, err = impl.EditHook(org, id, EditHookRequest{
// 		AddEvents: []string{"e2", "e3"},
// 	})
// 	assert.Nil(t, err)
// 	h3 := get(org, id)
// 	assert.Equal(t, []string{"e2", "e3"}, h3.Events)

// 	_, err = impl.EditHook(org, id, EditHookRequest{
// 		Active: true,
// 	})
// 	assert.Nil(t, err)

// 	h4 := get(org, id)
// 	assert.Equal(t, true, h4.Active)

// }

// func TestPingHook(t *testing.T) {
// 	kill, err := RunAHTTPServer(23456, "ok")
// 	assert.Nil(t, err)
// 	defer kill()
// 	org, id := create(CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org: "3", Project: "33", Application: "333",
// 		},
// 		Name: "ping-test", URL: "http://127.0.0.1:23456/1/q"})
// 	defer del(id)
// 	impl, _ := NewWebHookImpl()
// 	assert.Nil(t, impl.PingHook(org, id))
// }

// func TestListHooks(t *testing.T) {
// 	org, id1 := create(CreateHookRequest{
// 		HookLocation: HookLocation{Org: "3", Project: "33", Application: "333"},
// 		Name:         "222"})
// 	_, id2 := create(CreateHookRequest{
// 		HookLocation: HookLocation{Org: "3", Project: "33", Application: "333"},
// 		Name:         "222"})
// 	_, id3 := create(CreateHookRequest{
// 		HookLocation: HookLocation{Org: "3", Project: "33", Application: "333"},
// 		Name:         "333"})
// 	_, id4 := create(CreateHookRequest{
// 		HookLocation: HookLocation{Org: "3", Project: "33", Application: "333"},
// 		Name:         "444"})
// 	defer del(id1)
// 	defer del(id2)
// 	defer del(id3)
// 	defer del(id4)

// 	impl, _ := NewWebHookImpl()
// 	r, err := impl.ListHooks(HookLocation{
// 		Org: org,
// 	})
// 	assert.Nil(t, err)
// 	assert.Equal(t, 4, len(r))

// }

// func TestListHooksWithEnv(t *testing.T) {
// 	org, id1 := create(CreateHookRequest{
// 		HookLocation: HookLocation{Org: "3", Project: "33", Application: "333", Env: []string{"test", "dev"}},
// 		Name:         "222"})
// 	_, id2 := create(CreateHookRequest{
// 		HookLocation: HookLocation{Org: "3", Project: "33", Application: "333", Env: []string{"prod"}},
// 		Name:         "222"})
// 	_, id3 := create(CreateHookRequest{
// 		HookLocation: HookLocation{Org: "3", Project: "33", Application: "333"},
// 		Name:         "333"})
// 	defer del(id1)
// 	defer del(id2)
// 	defer del(id3)

// 	impl, _ := NewWebHookImpl()
// 	r, err := impl.ListHooks(HookLocation{
// 		Org: org,
// 		Env: []string{"prod", "dev"},
// 	})
// 	assert.Nil(t, err)
// 	assert.Equal(t, 3, len(r))

// 	r, err = impl.ListHooks(HookLocation{
// 		Org: org,
// 		Env: []string{"test"},
// 	})
// 	assert.Nil(t, err)
// 	assert.Equal(t, 2, len(r))

// 	r, err = impl.ListHooks(HookLocation{
// 		Org: org,
// 		Env: []string{"staging"},
// 	})
// 	assert.Nil(t, err)
// 	assert.Equal(t, 1, len(r))

// 	r, err = impl.ListHooks(HookLocation{
// 		Org: org,
// 		Env: []string{"staging", "test"},
// 	})
// 	assert.Nil(t, err)
// 	assert.Equal(t, 2, len(r))
// }

// func TestListHooksWithOnlyIndex(t *testing.T) {
// 	///////////////////////////////////////////////////////////////////////
// 	////      etcd 中存储的webhook结构
// 	//// 1. index:
// 	////    /<prefix>/<org>/<project>/<app>/<id> -> ""
// 	//// 2. value
// 	////    /<prefix>/<id> -> <value>
// 	////
// 	//// 当只有 index 没有 value 时，忽略这个webhook
// 	///////////////////////////////////////////////////////////////////////
// 	_, id := create(CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org: "3", Project: "33", Application: "333",
// 		},
// 		Name: "222"})
// 	defer del(id)
// 	impl, _ := NewWebHookImpl()
// 	var unused interface{}
// 	hs, err := impl.ListHooks(HookLocation{
// 		Org: "3",
// 	})
// 	assert.Nil(t, err)
// 	found := false
// 	for _, h := range hs {
// 		if h.ID == id {
// 			found = true
// 		}
// 	}
// 	assert.True(t, found)
// 	impl.js.Remove(context.Background(), "/eventbox/webhook/"+id, &unused)
// 	hs, err = impl.ListHooks(HookLocation{
// 		Org: "3",
// 	})
// 	assert.Nil(t, err)
// 	found = false
// 	for _, h := range hs {

// 		if h.ID == id {
// 			found = true
// 		}
// 	}
// 	assert.False(t, found)
// }

// func TestSearchHooks(t *testing.T) {
// 	org, id := create(CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org: "3", Project: "33", Application: "333"},
// 		Name: "234", Events: []string{"eeee"}, Active: true})
// 	defer del(id)
// 	impl, _ := NewWebHookImpl()
// 	hs := impl.SearchHooks(HookLocation{org, "", "", nil}, "eeee")
// 	assert.Equal(t, 1, len(hs))
// }

// func TestSearchHooks2(t *testing.T) {
// 	org, id := create(CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org: "3", Project: "33", Application: "333"},
// 		Name: "2345", Events: []string{"eeee"}, Active: true})
// 	defer del(id)
// 	impl, _ := NewWebHookImpl()
// 	hs := impl.SearchHooks(
// 		HookLocation{org, "33", "333", nil}, "eeee")
// 	assert.Equal(t, 1, len(hs))
// }

// func TestSearchHooks3(t *testing.T) {
// 	org1, id1 := create(CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org: "3", Project: "33", Application: "333"},
// 		Name: "2345", Events: []string{"eeee"}, Active: true})
// 	_, id2 := create(CreateHookRequest{
// 		HookLocation: HookLocation{
// 			Org: "3", Project: "33", Application: "334"},
// 		Name: "2345", Events: []string{"eeee"}, Active: true})
// 	defer del(id1)
// 	defer del(id2)
// 	impl, _ := NewWebHookImpl()
// 	hs := impl.SearchHooks(HookLocation{org1, "33", "333", nil}, "eeee")
// 	assert.Equal(t, 1, len(hs))
// 	hs = impl.SearchHooks(HookLocation{org1, "33", "", nil}, "eeee")
// 	assert.Equal(t, 2, len(hs))
// }

// func create(h CreateHookRequest) (string, string) {
// 	if h.URL == "" {
// 		h.URL = "http://xxx"
// 	}
// 	impl, _ := NewWebHookImpl()
// 	r, err := impl.CreateHook(h.Org, h)
// 	if err != nil {
// 		panic(err)
// 	}
// 	return h.Org, string(r)

// }

// func del(id string) {
// 	org := "3"
// 	impl, _ := NewWebHookImpl()
// 	impl.DeleteHook(org, id)
// }

// func get(org, id string) Hook {
// 	impl, _ := NewWebHookImpl()
// 	r, err := impl.InspectHook(org, id)
// 	if err != nil {
// 		panic(err)
// 	}
// 	return Hook(r)
// }
