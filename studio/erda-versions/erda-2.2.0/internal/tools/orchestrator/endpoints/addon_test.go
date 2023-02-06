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

package endpoints

import (
	"math/rand"
	"reflect"
	"sync"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

const count = 20

func TestConcurrentWriteAddonInfos(t *testing.T) {
	keys := []string{"a", "b", "c", "d", "e"}

	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryExtensions",
		func(*bundle.Bundle, apistructs.ExtensionQueryRequest) ([]apistructs.Extension, error) {
			var ext []apistructs.Extension
			for i, v := range keys {
				ext = append(ext, apistructs.Extension{ID: uint64(i), Name: v})
			}
			return ext, nil
		},
	)
	defer monkey.UnpatchAll()
	var (
		wg sync.WaitGroup
	)
	wg.Add(count)
	for i := 0; i != count; i++ {
		go func() {
			e := Endpoints{}
			_, err := e.SyncAddons()
			if err != nil {
				t.Fatal(err)
			}
			wg.Done()
		}()
	}
	wg.Wait()
	for _, v := range keys {
		_, ok := addon.AddonInfos.Load(v)
		assert.Equal(t, true, ok)
	}
}

func TestConcurrentReadWriteAddonInfos(t *testing.T) {
	keys := []string{"a", "b", "c", "d", "e"}

	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryExtensions",
		func(*bundle.Bundle, apistructs.ExtensionQueryRequest) ([]apistructs.Extension, error) {
			var ext []apistructs.Extension
			for i, v := range keys {
				ext = append(ext, apistructs.Extension{ID: uint64(i), Name: v})
			}
			return ext, nil
		},
	)
	defer monkey.UnpatchAll()
	var (
		wg sync.WaitGroup
	)
	wg.Add(count * 2)
	for i := 0; i != count; i++ {
		go func() {
			e := Endpoints{}
			_, err := e.SyncAddons()
			if err != nil {
				t.Fatal(err)
			}
			wg.Done()
		}()
	}
	for i := 0; i != count; i++ {
		go func() {
			addon.AddonInfos.Load(rand.Intn(len(keys)))
			wg.Done()
		}()
	}

	wg.Wait()
	for _, v := range keys {
		_, ok := addon.AddonInfos.Load(v)
		assert.Equal(t, true, ok)
	}
}

func TestConcurrentWriteProjectInfos(t *testing.T) {
	keys := []string{"1", "2", "3", "4", "5"}

	var db *dbclient.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetDistinctProjectInfo",
		func(*dbclient.DBClient) (*[]string, error) {
			return &keys, nil
		},
	)
	defer monkey.UnpatchAll()

	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProject",
		func(_ *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{
				ID: id,
			}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProjectWithSetter",
		func(bdl *bundle.Bundle, id uint64, requestSetter ...httpclient.RequestSetter) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{
				ID: id,
			}, nil
		},
	)

	var (
		wg sync.WaitGroup
	)
	wg.Add(count)
	for i := 0; i != count; i++ {
		go func() {
			e := Endpoints{}
			_, err := e.SyncProjects()
			if err != nil {
				t.Fatal(err)
			}
			wg.Done()
		}()
	}
	wg.Wait()
	for _, v := range keys {
		_, ok := addon.ProjectInfos.Load(v)
		assert.Equal(t, true, ok)
	}
}

func TestConcurrentReadWriteProjectInfos(t *testing.T) {
	keys := []string{"1", "2", "3", "4", "5"}

	var db *dbclient.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetDistinctProjectInfo",
		func(*dbclient.DBClient) (*[]string, error) {
			return &keys, nil
		},
	)
	defer monkey.UnpatchAll()

	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProject",
		func(_ *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{
				ID: id,
			}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProjectWithSetter",
		func(bdl *bundle.Bundle, id uint64, requestSetter ...httpclient.RequestSetter) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{
				ID: id,
			}, nil
		},
	)

	var (
		wg sync.WaitGroup
	)
	wg.Add(count * 2)
	for i := 0; i != count; i++ {
		go func() {
			e := Endpoints{}
			_, err := e.SyncProjects()
			if err != nil {
				t.Fatal(err)
			}
			wg.Done()
		}()
	}
	for i := 0; i != count; i++ {
		go func() {
			addon.ProjectInfos.Load(rand.Intn(len(keys)))
			wg.Done()
		}()
	}
	wg.Wait()
	for _, v := range keys {
		_, ok := addon.ProjectInfos.Load(v)
		assert.Equal(t, true, ok)
	}
}
