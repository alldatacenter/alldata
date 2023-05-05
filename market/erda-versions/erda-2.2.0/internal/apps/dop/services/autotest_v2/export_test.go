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

package autotestv2

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/i18n"
)

func TestSetSingleSceneSet(t *testing.T) {
	autotestSvc := New()
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(autotestSvc), "GetSceneSet", func(svc *Service, setID uint64) (*apistructs.SceneSet, error) {
		return &apistructs.SceneSet{ID: setID}, nil
	})
	defer pm.Unpatch()
	a := &AutoTestSpaceDB{
		Data: &AutoTestSpaceData{
			svc: autotestSvc,
		},
	}
	err := a.SetSingleSceneSet(3)
	assert.NoError(t, err)
}

func TestExportSceneSet(t *testing.T) {
	autotestSvc := New()
	autotestSvc.CreateFileRecord = func(req apistructs.TestFileRecordRequest) (uint64, error) {
		return 1, nil
	}

	bdl := bundle.New(bundle.WithI18nLoader(&i18n.LocaleResourceLoader{}))
	m := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetLocale",
		func(bdl *bundle.Bundle, local ...string) *i18n.LocaleResource {
			return &i18n.LocaleResource{}
		})
	defer m.Unpatch()

	autotestSvc.bdl = bdl
	_, err := autotestSvc.ExportSceneSet(apistructs.AutoTestSceneSetExportRequest{
		ID:       1,
		FileType: apistructs.TestSceneSetFileTypeExcel,
	})
	assert.NoError(t, err)
}
