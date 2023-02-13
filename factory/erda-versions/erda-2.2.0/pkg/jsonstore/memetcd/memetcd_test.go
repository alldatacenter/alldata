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

package memetcd

//import (
//	"context"
//	"fmt"
//	"strconv"
//	"testing"
//	"time"
//
//	"github.com/erda-project/erda/pkg/jsonstore/etcd"
//	"github.com/erda-project/erda/pkg/jsonstore/mem"
//	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
//
//	"github.com/stretchr/testify/assert"
//)
//
//func TestSync(t *testing.T) {
//	ctx, cancel := context.WithCancel(context.Background())
//	defer cancel()
//	s, err := New(ctx, "/memetcd_test", nil)
//	assert.Nil(t, err)
//	e, err := etcd.New()
//	assert.Nil(t, err)
//	e.Put(ctx, "/memetcd_test/1", "1")
//	e.Put(ctx, "/memetcd_test/1", "2")
//	e.Put(ctx, "/memetcd_test/2", "3")
//	e.Put(ctx, "/memetcd_test/2", "4")
//	e.Put(ctx, "/memetcd_test/2", "3")
//	time.Sleep(100 * time.Millisecond)
//	kv, err := s.Get(ctx, "/memetcd_test/1")
//	assert.Nil(t, err)
//	assert.Equal(t, "2", string(kv.Value))
//	kv, err = s.Get(ctx, "/memetcd_test/2")
//	assert.Nil(t, err)
//	assert.Equal(t, "3", string(kv.Value))
//}
//
//func TestPut(t *testing.T) {
//	ctx, cancel := context.WithCancel(context.Background())
//	defer cancel()
//	s, err := New(ctx, "/memetcd_test", nil)
//	assert.Nil(t, err)
//	s.PrefixRemove(ctx, "/memetcd_test")
//	kvs, err := s.PrefixGet(ctx, "/memetcd_test")
//	assert.Nil(t, err)
//	assert.Equal(t, 0, len(kvs))
//	assert.Nil(t, s.Put(ctx, "/memetcd_test/1", "1"))
//	kv, err := s.Get(ctx, "/memetcd_test/1")
//	assert.Nil(t, err)
//	assert.Equal(t, "1", string(kv.Value))
//}
//
//func TestWatch(t *testing.T) {
//	ctx, cancel := context.WithCancel(context.Background())
//	s, err := New(ctx, "/memetcd_test", nil)
//	assert.Nil(t, err)
//	ch, err := s.Watch(ctx, "/memetcd_test", true, false)
//	assert.Nil(t, err)
//	e, err := etcd.New()
//	assert.Nil(t, err)
//	assert.Nil(t, e.Put(ctx, "/memetcd_test/1", "1"))
//	count := 0
//	c := make(chan struct{})
//	go func() {
//		for range ch {
//			count++
//		}
//		c <- struct{}{}
//	}()
//	time.Sleep(1 * time.Second)
//	assert.Equal(t, 1, count)
//	cancel()
//	<-c
//}
//
//func TestWatchAndDel(t *testing.T) {
//	ctx, cancel := context.WithCancel(context.Background())
//	s, err := New(ctx, "/memetcd_test", nil)
//	assert.Nil(t, err)
//	ch, err := s.Watch(ctx, "/memetcd_test", true, false)
//	assert.Nil(t, err)
//	e, err := etcd.New()
//	assert.Nil(t, err)
//	assert.Nil(t, e.Put(ctx, "/memetcd_test/1", "3"))
//	resp := <-ch
//
//	_, err = e.Remove(ctx, "/memetcd_test/1")
//	assert.Nil(t, err)
//	resp = <-ch
//	assert.Equal(t, 1, len(resp.Kvs))
//	assert.Equal(t, storetypes.Del, resp.Kvs[0].T)
//	assert.Equal(t, "/memetcd_test/1", string(resp.Kvs[0].Key))
//	fmt.Printf("%+v", resp)
//	assert.Equal(t, "3", string(resp.Kvs[0].Value))
//
//	assert.Nil(t, e.Put(ctx, "/memetcd_test/2", ""))
//	assert.Nil(t, err)
//	resp = <-ch
//	_, err = e.Remove(ctx, "/memetcd_test/2")
//	assert.Nil(t, err)
//	resp = <-ch
//	assert.Equal(t, 1, len(resp.Kvs))
//	assert.Equal(t, storetypes.Del, resp.Kvs[0].T)
//	assert.Equal(t, "/memetcd_test/2", string(resp.Kvs[0].Key))
//	assert.Equal(t, "", string(resp.Kvs[0].Value))
//
//	cancel()
//}
//
//func TestDiff(t *testing.T) {
//	old, _ := mem.New()
//	new_, _ := mem.New()
//	r, err := diff(old, new_, "/")
//	assert.Nil(t, err)
//	assert.Equal(t, 0, len(r[0]))
//	assert.Equal(t, 0, len(r[1]))
//	assert.Equal(t, 0, len(r[2]))
//
//	old.Put(context.Background(), "/k1", "v1")
//	r, err = diff(old, new_, "/")
//	assert.Nil(t, err)
//	assert.Equal(t, 1, len(r[1]))
//
//	new_.Put(context.Background(), "/k2", "v2")
//	r, err = diff(old, new_, "/")
//	assert.Nil(t, err)
//	assert.Equal(t, 1, len(r[0]))
//
//	new_.Put(context.Background(), "/k1", "v3")
//	r, err = diff(old, new_, "/")
//	assert.Nil(t, err)
//	assert.Equal(t, 1, len(r[2]))
//}
//
//// 测试s.mem.get 会miss，s.get不会
//func TestPutAndImmediatelyGet(t *testing.T) {
//	ctx, cancel := context.WithCancel(context.Background())
//	defer cancel()
//
//	s1, err := New(ctx, "/", nil)
//	assert.Nil(t, err)
//
//	s2, err := New(ctx, "/", nil)
//	assert.Nil(t, err)
//
//	cnt := 10
//	miss := 0
//	var missedKey string
//
//	for miss < 3 {
//		k := "/retrytest/" + strconv.Itoa(cnt)
//		s1.Put(ctx, k, "11")
//
//		_, err := s2.mem.Get(ctx, k)
//		if err != nil {
//			miss++
//			missedKey = k
//		}
//		_, err = s2.Get(ctx, k)
//		assert.Nil(t, err)
//		cnt++
//	}
//
//	println("direct s.mem missed " + strconv.Itoa(miss) + " keys, last one is " + missedKey + " and s.Get get it well")
//}
//
//// try decreasing the loadPeriod of memetcd to improve the possibility
//func TestPutAndLaterGet(t *testing.T) {
//	ctx, cancel := context.WithCancel(context.Background())
//	defer cancel()
//
//	s, err := New(ctx, "/", nil)
//	assert.Nil(t, err)
//
//	cnt := 0
//	for cnt < 1000 {
//		k := "/latertest/" + strconv.Itoa(cnt)
//		s.Put(ctx, k, "1")
//		cnt++
//	}
//
//	for i := 0; i < 1000; i++ {
//		cnt = 0
//		for cnt < 1000 {
//			k := "/latertest/" + strconv.Itoa(cnt)
//
//			_, err := s.Get(ctx, k)
//			assert.Nil(t, err)
//
//			cnt++
//		}
//	}
//
//}
