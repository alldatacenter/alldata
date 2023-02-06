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

package jsonstore

//import (
//	"context"
//	"encoding/json"
//	"sync"
//	"testing"
//	"time"
//
//	"github.com/erda-project/erda/pkg/jsonstore/stm"
//	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
//
//	"github.com/stretchr/testify/assert"
//)
//
//type testObject struct {
//	AAA string
//	BBB int
//	CCC bool
//}
//
//var objects = map[string]testObject{
//	"/test/jsonstore/1": {
//		AAA: "1",
//		BBB: 1,
//		CCC: true,
//	},
//	"/test/jsonstore/2": {
//		AAA: "2",
//		BBB: 2,
//		CCC: true,
//	},
//	"/test/jsonstore/3": {
//		AAA: "3",
//		BBB: 3,
//		CCC: true,
//	},
//	"/test/jsonstore/4": {
//		AAA: "4",
//		BBB: 4,
//		CCC: true,
//	},
//	"/test/jsonstore/5": {
//		AAA: "5",
//		BBB: 5,
//		CCC: true,
//	},
//}
//var jsTimeout, _ = New(UseTimeoutStore(5), UseMemStore())
//var jsLru, _ = New(UseLruStore(5), UseMemStore())
//var jsMem, _ = New(UseMemStore())
//var jsEtcd, _ = New()
//
//// var jsMemEtcd, err = New(UseMemEtcdStore(context.Background(), "/test/jsonstore", nil, nil))
//var jsCacheEtcd, err = New(UseCacheEtcdStore(context.Background(), "/test/jsonstore", 4))
//var allJs = []JsonStore{jsEtcd, jsLru, jsMem, jsCacheEtcd, jsTimeout}
//
//func TestStorePut(t *testing.T) {
//	for _, js := range allJs {
//
//		ctx := context.Background()
//
//		for k := range objects {
//			to := testObject{}
//			err := js.Remove(ctx, k, &to)
//			assert.Nil(t, err)
//			t.Logf("%v", to)
//		}
//
//		for k, e := range objects {
//			err := js.Put(ctx, k, e)
//			assert.Nil(t, err)
//		}
//	}
//}
//
//func TestStoreGet(t *testing.T) {
//	for _, js := range allJs {
//		ctx := context.Background()
//
//		for k, e := range objects {
//			var elem testObject
//			err := js.Get(ctx, k, &elem)
//			assert.Nil(t, err)
//
//			assert.Equal(t, e.AAA, elem.AAA)
//			assert.Equal(t, e.BBB, elem.BBB)
//			assert.Equal(t, e.CCC, elem.CCC)
//		}
//	}
//}
//
//func TestStoreForEach(t *testing.T) {
//	for _, js := range allJs {
//		ctx := context.Background()
//
//		prefix := "/test/jsonstore/"
//
//		err := js.ForEach(ctx, prefix, testObject{}, func(k string, o interface{}) error {
//			obj, ok := o.(*testObject)
//			assert.Equal(t, true, ok)
//
//			assert.Equal(t, objects[k].AAA, obj.AAA)
//			assert.Equal(t, objects[k].BBB, obj.BBB)
//			assert.Equal(t, objects[k].CCC, obj.CCC)
//
//			return nil
//		})
//		assert.Nil(t, err)
//	}
//}
//func TestStoreForEachRaw(t *testing.T) {
//	ctx := context.Background()
//
//	js, err := New()
//	assert.Nil(t, err)
//
//	prefix := "/test/jsonstore/"
//
//	err = js.ForEachRaw(ctx, prefix, func(k string, o []byte) error {
//		var obj testObject
//		err := json.Unmarshal(o, &obj)
//		assert.Nil(t, err)
//		assert.Equal(t, objects[k].AAA, obj.AAA)
//		assert.Equal(t, objects[k].BBB, obj.BBB)
//		assert.Equal(t, objects[k].CCC, obj.CCC)
//
//		return nil
//	})
//	assert.Nil(t, err)
//}
//
//func TestStoreForEachKeyOnly(t *testing.T) {
//	ctx := context.Background()
//
//	js, err := New()
//	assert.Nil(t, err)
//
//	prefix := "/test/jsonstore"
//	keys, err := js.ListKeys(ctx, prefix)
//	assert.Nil(t, err)
//	assert.Equal(t, len(keys), 5)
//}
//
//func TestStoreNotfound(t *testing.T) {
//	for _, js := range allJs {
//		ctx := context.Background()
//
//		key := "/test/jsonstore/1"
//
//		notfound, err := js.Notfound(ctx, key)
//		assert.Nil(t, err)
//		assert.Equal(t, false, notfound)
//
//		notfound, err = js.Notfound(ctx, "/test/jsonstore/jfdkfjakfhadkfjadkfjaksfjaf")
//		assert.Nil(t, err)
//		assert.Equal(t, true, notfound)
//	}
//}
//
//func TestStoreForPrefixRemove(t *testing.T) {
//	ctx := context.Background()
//
//	js, err := New()
//	assert.Nil(t, err)
//
//	prefix := "/test/jsonstore"
//	deleted, err := js.PrefixRemove(ctx, prefix)
//	assert.Nil(t, err)
//	assert.Equal(t, deleted, 5)
//
//	check, err := js.PrefixRemove(ctx, prefix)
//	assert.Nil(t, err)
//	assert.Equal(t, check, 0)
//}
//
//func TestStoreWatch(t *testing.T) {
//	jss := []JsonStore{jsEtcd, jsCacheEtcd}
//	for _, js_ := range jss {
//		ctx := context.Background()
//
//		js := js_.(JSONStoreWithWatch)
//		putcount := 0
//		delcount := 0
//
//		go func() {
//			err := js.Watch(ctx, "/test/jsonstore", true, false, false, testObject{}, func(k string, v interface{}, t storetypes.ChangeType) error {
//				if t == storetypes.Del {
//					delcount++
//				} else {
//					putcount++
//				}
//				return nil
//			})
//			assert.Nil(t, err)
//		}()
//		time.Sleep(500 * time.Millisecond)
//		for k, e := range objects {
//			err := js.Put(ctx, k, e)
//			assert.Nil(t, err)
//		}
//		for k := range objects {
//			to := testObject{}
//			err := js.Remove(ctx, k, &to)
//			assert.Nil(t, err)
//		}
//		time.Sleep(500 * time.Millisecond)
//		assert.Equal(t, 5, putcount)
//		assert.Equal(t, 5, delcount)
//	}
//}
//
//func TestSTM(t *testing.T) {
//	s := jsEtcd.IncludeSTM()
//	assert.NotNil(t, s)
//	var unused interface{}
//	jsEtcd.Put(context.Background(), "/teststm", 0)
//	defer jsEtcd.Remove(context.Background(), "/teststm", &unused)
//	var wg sync.WaitGroup
//	f := func() {
//		defer wg.Done()
//		defer s.NewSTM(func(s stm.JSONStoreSTMOP) error {
//			var o int
//			s.Get("/teststm", &o)
//			s.Put("/teststm", o+1)
//			return nil
//		})
//	}
//	wg.Add(50)
//	for i := 0; i < 50; i++ {
//		go f()
//	}
//	wg.Wait()
//	var v int
//	jsEtcd.Get(context.Background(), "/teststm", &v)
//	assert.Equal(t, 50, v)
//}
