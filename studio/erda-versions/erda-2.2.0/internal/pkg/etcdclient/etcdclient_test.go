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

package etcdclient

import (
	"os"
	"testing"

	"bou.ke/monkey"
)

//import (
//	"sync"
//	"testing"
//
//	"github.com/coreos/etcd/clientv3"
//)
//
//func TestSingleInstance(t *testing.T) {
//	var instance1 *clientv3.Client
//	var instance2 *clientv3.Client
//	waitGroup := sync.WaitGroup{}
//	waitGroup.Add(2)
//	go func() {
//		for i := 0; i < 100; i++ {
//			instance1, _ = NewEtcdClientSingleInstance()
//		}
//		waitGroup.Done()
//	}()
//	go func() {
//		for i := 0; i < 100; i++ {
//			instance2, _ = NewEtcdClientSingleInstance()
//		}
//		waitGroup.Done()
//	}()
//	waitGroup.Wait()
//	if instance1 != instance2 {
//		t.Failed()
//	}
//
//}

func Test_getEnvOrDefault(t *testing.T) {
	nonExistsKey := "none_exists_key"
	defaultValue := "default_value"
	existsKey := "exists_key"
	existsValue := "exists_value"

	defer monkey.Unpatch(os.Getenv)
	monkey.Patch(os.Getenv, func(key string) string {
		if key == nonExistsKey {
			return ""
		}

		return existsValue
	})

	val := getEnvOrDefault(nonExistsKey, defaultValue)
	if val != defaultValue {
		t.Errorf("with %s should return %s, but got: %s", nonExistsKey, defaultValue, val)
	}

	val = getEnvOrDefault(existsKey, defaultValue)
	if val != existsValue {
		t.Errorf("with %s should return %s, but got: %s", existsKey, existsValue, val)
	}
}

func Test_NewEtcdClient_ReadCertFromEnv(t *testing.T) {

	defer monkey.Unpatch(os.Getenv)
	monkey.Patch(os.Getenv, func(key string) string {
		return "mock"
	})

	_, err := NewEtcdClient()
	if err != nil {
		t.Errorf("should not throw error")
	}
}
