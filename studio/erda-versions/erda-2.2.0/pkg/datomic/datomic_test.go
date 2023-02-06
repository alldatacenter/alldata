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

package datomic

//import (
//	"fmt"
//	"sync"
//	"testing"
//
//	"github.com/stretchr/testify/assert"
//)
//
//func TestAdd(t *testing.T) {
//	var new0, new1, new2, new3, new4, new5, new6, new7, new8, new9 uint64
//	var old0, old1, old2, old3, old4, old5, old6, old7, old8, old9 uint64
//	var g sync.WaitGroup
//	g.Add(10)
//	dint, err := New("key_add")
//	assert.Nil(t, err)
//	defer dint.Clear()
//	f := func(newv, oldv *uint64) {
//		dint, err := New("key_add")
//		assert.Nil(t, err)
//		old, new, err := dint.Add(5)
//		assert.Nil(t, err)
//		assert.True(t, new-old == 5)
//		*newv = new
//		*oldv = old
//		g.Done()
//	}
//	go f(&new0, &old0)
//	go f(&new1, &old1)
//	go f(&new2, &old2)
//	go f(&new3, &old3)
//	go f(&new4, &old4)
//	go f(&new5, &old5)
//	go f(&new6, &old6)
//	go f(&new7, &old7)
//	go f(&new8, &old8)
//	go f(&new9, &old9)
//	g.Wait()
//	assert.True(t, (old0+old1+old2+old3+old4+old5+old6+old7+old8+old9+50) == (new0+new1+new2+new3+new4+new5+new6+new7+new8+new9))
//}
//
//func TestStore(t *testing.T) {
//	dint, err := New("key_store")
//	assert.Nil(t, err)
//	defer dint.Clear()
//
//	succSum := 0
//	var g sync.WaitGroup
//	g.Add(5)
//
//	f := func() {
//		dint, _ := New("key_store")
//		succ, err := dint.Store(func(old uint64) bool {
//			fmt.Printf("%+v\n", old) // debug print
//
//			return old == 0
//		}, 1)
//		assert.Nil(t, err)
//		if succ {
//			succSum += 1
//		}
//		g.Done()
//	}
//
//	go f()
//	go f()
//	go f()
//	go f()
//	go f()
//	g.Wait()
//	assert.Equal(t, 1, succSum)
//}
