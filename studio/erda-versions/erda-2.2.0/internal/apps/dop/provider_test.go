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

package dop

import (
	"context"
	"embed"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/protobuf/proto-go/cp/pb"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/autotest/testplan"
	"github.com/erda-project/erda/internal/apps/dop/providers/cms"
	"github.com/erda-project/erda/internal/apps/dop/providers/guide"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/sync"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline"
	"github.com/erda-project/erda/internal/apps/dop/providers/taskerror"
	"github.com/erda-project/erda/internal/apps/dop/services/cdp"
)

type MockCP struct {
	Tran i18n.I18n
}

func (m *MockCP) Render(context.Context, *pb.RenderRequest) (*pb.RenderResponse, error) {
	return nil, nil
}
func (m *MockCP) SetI18nTran(tran i18n.I18n)              { m.Tran = tran }
func (m *MockCP) WithContextValue(key, value interface{}) {}

type MockI18n struct{}

func (m *MockI18n) Get(namespace string, lang i18n.LanguageCodes, key, def string) string { return "" }
func (m *MockI18n) Text(namespace string, lang i18n.LanguageCodes, key string) string     { return "" }
func (m *MockI18n) Sprintf(namespace string, lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return ""
}
func (m MockI18n) Translator(namespace string) i18n.Translator                 { return &i18n.NopTranslator{} }
func (m *MockI18n) RegisterFilesFromFS(fsPrefix string, rootFS embed.FS) error { return nil }

func Test_provider_Init(t *testing.T) {

	mockCP := &MockCP{}
	nopTran := &MockI18n{}
	p := &provider{Log: logrusx.New(), Protocol: mockCP, CPTran: nopTran}
	monkey.PatchInstanceMethod(reflect.TypeOf(p), "Initialize",
		func(p *provider, ctx servicehub.Context) error {
			return nil
		})
	defer monkey.UnpatchAll()

	err := p.Init(nil)
	assert.NoError(t, err)
	assert.Equal(t, mockCP.Tran, nopTran)
}

type MockResourceTran struct{}

func (m *MockResourceTran) Get(lang i18n.LanguageCodes, key string, def string) string { return "" }
func (m *MockResourceTran) Text(lang i18n.LanguageCodes, key string) string            { return "" }
func (m *MockResourceTran) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return ""
}
func (m MockResourceTran) Translator(namespace string) i18n.Translator { return &i18n.NopTranslator{} }

func TestNewCDP(t *testing.T) {
	bdl := bundle.New()
	nopTran := &MockResourceTran{}
	c := cdp.New(cdp.WithBundle(bdl), cdp.WithResourceTranslator(nopTran))
	assert.NotNil(t, c)
}

func Test_initEndpoints(t *testing.T) {
	p := &provider{
		Log:                   logrusx.New(),
		TestPlanSvc:           &testplan.TestPlanService{},
		TaskErrorSvc:          &taskerror.TaskErrorService{},
		CommentIssueStreamSvc: &stream.CommentIssueStreamService{},
		IssueSyncSvc:          &sync.IssueSyncService{},
		ProjectPipelineSvc:    &projectpipeline.ProjectPipelineService{},
		GuideSvc:              &guide.GuideService{},
		CICDCmsSvc:            &cms.CICDCmsService{},
		IssueCoreSvc:          &core.IssueService{},
	}

	_, err := p.initEndpoints(nil)
	assert.NoError(t, err)
}
