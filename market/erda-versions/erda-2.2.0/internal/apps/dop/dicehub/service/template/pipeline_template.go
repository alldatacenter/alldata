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

package template

import (
	"errors"
	"fmt"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type PipelineTemplate struct {
	db  *dbclient.DBClient
	bdl *bundle.Bundle
}

// Option 定义 Extension 对象的配置选项
type Option func(*PipelineTemplate)

// New 新建 Extension 实例，操作 Extension 资源
func New(options ...Option) *PipelineTemplate {
	app := &PipelineTemplate{}
	for _, op := range options {
		op(app)
	}
	return app
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(a *PipelineTemplate) {
		a.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(a *PipelineTemplate) {
		a.bdl = bdl
	}
}

func (p *PipelineTemplate) Apply(request *apistructs.PipelineTemplateApplyRequest) (template *apistructs.PipelineTemplate, err error) {
	var templateSpec apistructs.PipelineTemplateSpec
	if err := yaml.Unmarshal([]byte(request.Spec), &templateSpec); err != nil {
		return nil, apierrors.ErrCreateExtension.InternalError(err)
	}

	if err := templateSpec.Check(); err != nil {
		return nil, apierrors.ErrCreateExtension.InternalError(err)
	}

	createRequest := apistructs.PipelineTemplateCreateRequest{
		Name:           templateSpec.Name,
		ScopeType:      request.ScopeType,
		ScopeID:        request.ScopeID,
		Spec:           request.Spec,
		Desc:           templateSpec.Desc,
		LogoUrl:        templateSpec.LogoUrl,
		Version:        templateSpec.Version,
		DefaultVersion: templateSpec.DefaultVersion,
	}

	return p.Create(&createRequest)
}

func (p *PipelineTemplate) Create(request *apistructs.PipelineTemplateCreateRequest) (*apistructs.PipelineTemplate, error) {
	if request.Name == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("name")
	}

	if request.ScopeID == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("scopeID")
	}

	if request.ScopeType == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("scopeType")
	}

	if request.Spec == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("spec")
	}

	if request.Version == "" {
		request.Version = "latest"
	}

	if request.DefaultVersion == "" {
		request.DefaultVersion = request.Version
	}

	dbPipelineTemplate, err := p.db.GetPipelineTemplate(request.Name, request.ScopeType, request.ScopeID)
	if err != nil {
		return nil, err
	}

	tx := p.db.Begin()
	//已经存在
	var saveTemplate *dbclient.DicePipelineTemplate
	if dbPipelineTemplate != nil {
		saveTemplate = dbPipelineTemplate
		saveTemplate.Desc = request.Desc
		saveTemplate.LogoUrl = request.LogoUrl
		saveTemplate.DefaultVersion = request.DefaultVersion
		if err := p.db.UpdatePipelineTemplate(saveTemplate); err != nil {
			tx.Rollback()
			return nil, errors.New(fmt.Sprintf("update pipeline template error. saveTemplate %v", saveTemplate))
		}
	} else {
		saveTemplate = &dbclient.DicePipelineTemplate{
			ScopeId:        request.ScopeID,
			ScopeType:      request.ScopeType,
			Name:           request.Name,
			Desc:           request.Desc,
			LogoUrl:        request.LogoUrl,
			DefaultVersion: request.DefaultVersion,
		}
		if err := p.db.CreatePipelineTemplate(saveTemplate); err != nil {
			tx.Rollback()
			return nil, errors.New(fmt.Sprintf("create pipeline template error. saveTemplate %v", saveTemplate))
		}
	}

	dbPipelineTemplateVersion, err := p.db.GetPipelineTemplateVersion(request.Version, saveTemplate.ID)
	if err != nil {
		tx.Rollback()
		return nil, err
	}

	if dbPipelineTemplateVersion != nil {
		dbPipelineTemplateVersion.Spec = request.Spec
		dbPipelineTemplateVersion.Readme = request.Readme
		if err := p.db.UpdatePipelineTemplateVersion(dbPipelineTemplateVersion); err != nil {
			tx.Rollback()
			return nil, errors.New(fmt.Sprintf("update pipeline template version error. saveTemplateVersion %v", dbPipelineTemplateVersion))
		}
	} else {
		dbPipelineTemplateVersion = &dbclient.DicePipelineTemplateVersion{
			Name:       saveTemplate.Name,
			Version:    request.Version,
			Spec:       request.Spec,
			Readme:     request.Readme,
			TemplateId: saveTemplate.ID,
		}
		if err := p.db.CreatePipelineTemplateVersion(dbPipelineTemplateVersion); err != nil {
			tx.Rollback()
			return nil, errors.New(fmt.Sprintf("create pipeline template version error. saveTemplateVersion %v", dbPipelineTemplateVersion))
		}
	}
	data := saveTemplate.ToApiData()
	data.Version = request.Version
	tx.Commit()
	return data, err
}

func (p *PipelineTemplate) QueryPipelineTemplates(request *apistructs.PipelineTemplateQueryRequest) ([]*apistructs.PipelineTemplate, int, error) {
	queryDicePipelineTemplate := dbclient.DicePipelineTemplate{
		Name:      request.Name,
		ScopeId:   request.ScopeID,
		ScopeType: request.ScopeType,
	}
	dbTemplates, total, err := p.db.QueryByPipelineTemplates(&queryDicePipelineTemplate, request.PageSize, request.PageNo)
	if err != nil {
		return nil, 0, err
	}

	var result []*apistructs.PipelineTemplate

	if dbTemplates == nil {
		return result, 0, nil
	}

	for _, v := range dbTemplates {
		result = append(result, v.ToApiData())
	}

	return result, total, nil
}

func (p *PipelineTemplate) GetPipelineTemplateVersion(request *apistructs.PipelineTemplateVersionGetRequest) (*apistructs.PipelineTemplateVersion, error) {
	if request.Name == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("name")
	}
	if request.ScopeID == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("scopeID")
	}
	if request.ScopeType == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("scopeType")
	}
	if request.Version == "" {
		return nil, apierrors.ErrCreateExtension.InvalidParameter("Version")
	}

	dbTemplate, err := p.db.GetPipelineTemplate(request.Name, request.ScopeType, request.ScopeID)
	if err != nil {
		return nil, apierrors.ErrCreateExtension.InternalError(err)
	}

	if dbTemplate == nil {
		return nil, nil
	}

	dbVersion, err := p.db.GetPipelineTemplateVersion(request.Version, dbTemplate.ID)
	if err != nil {
		return nil, apierrors.ErrCreateExtension.InternalError(err)
	}

	if dbVersion == nil {
		return nil, nil
	}

	return dbVersion.ToApiData(), nil
}

func (p *PipelineTemplate) QueryPipelineTemplateVersions(request apistructs.PipelineTemplateVersionQueryRequest) ([]*apistructs.PipelineTemplateVersion, error) {

	if request.Name == "" {
		return nil, apierrors.ErrQueryPipelineTemplateVersion.InvalidParameter("name")
	}
	if request.ScopeID == "" {
		return nil, apierrors.ErrQueryPipelineTemplateVersion.InvalidParameter("scopeID")
	}
	if request.ScopeType == "" {
		return nil, apierrors.ErrQueryPipelineTemplateVersion.InvalidParameter("scopeType")
	}

	dbTemplate, err := p.db.GetPipelineTemplate(request.Name, request.ScopeType, request.ScopeID)
	if err != nil || dbTemplate == nil {
		return nil, apierrors.ErrQueryPipelineTemplateVersion.InternalError(err)
	}

	version := dbclient.DicePipelineTemplateVersion{
		TemplateId: dbTemplate.ID,
	}

	dbVersions, err := p.db.QueryPipelineTemplateVersions(&version)
	if err != nil {
		return nil, apierrors.ErrQueryPipelineTemplateVersion.InternalError(err)
	}

	var result []*apistructs.PipelineTemplateVersion
	for _, v := range dbVersions {
		result = append(result, v.ToApiData())
	}
	return result, nil
}

func (p *PipelineTemplate) RenderPipelineTemplate(request apistructs.PipelineTemplateRenderRequest) (*apistructs.PipelineTemplateRender, error) {
	if request.Name == "" {
		return nil, apierrors.ErrRenderPipelineTemplate.InvalidParameter("name")
	}
	if request.ScopeID == "" {
		return nil, apierrors.ErrRenderPipelineTemplate.InvalidParameter("scopeID")
	}
	if request.ScopeType == "" {
		return nil, apierrors.ErrRenderPipelineTemplate.InvalidParameter("scopeType")
	}
	if request.Version == "" {
		return nil, apierrors.ErrRenderPipelineTemplate.InvalidParameter("Version")
	}

	if request.TemplateVersion == apistructs.TemplateVersionV2 {
		if request.Alias == "" {
			return nil, apierrors.ErrRenderPipelineTemplate.InvalidParameter("alias")
		}
	}

	getPipelineTemplateRequest := apistructs.PipelineTemplateVersionGetRequest{
		Name:      request.Name,
		ScopeType: request.ScopeType,
		ScopeID:   request.ScopeID,
		Version:   request.Version,
	}

	templateVersion, err := p.GetPipelineTemplateVersion(&getPipelineTemplateRequest)
	if err != nil {
		return nil, apierrors.ErrRenderPipelineTemplate.InternalError(err)
	}

	if templateVersion == nil {
		return nil, apierrors.ErrRenderPipelineTemplate.InternalError(errors.New(fmt.Sprintf(" not find template version. request %v", request)))
	}

	specYaml := templateVersion.Spec

	pipelineYaml, outputs, err := RenderTemplate(specYaml, request)
	if err != nil {
		return nil, apierrors.ErrRenderPipelineTemplate.InternalError(err)
	}

	render := apistructs.PipelineTemplateRender{
		PipelineYaml: pipelineYaml,
		Version:      templateVersion,
		Outputs:      outputs,
	}

	return &render, nil
}

func (p *PipelineTemplate) RenderPipelineTemplateBySpec(request *apistructs.PipelineTemplateRenderSpecRequest) (*apistructs.PipelineTemplateRender, error) {

	if request.Spec == nil {
		return nil, apierrors.ErrRenderPipelineTemplate.InvalidParameter("spec")
	}
	if request.Alias == "" {
		return nil, apierrors.ErrRenderPipelineTemplate.InvalidParameter("alias")
	}

	params := request.Spec.Params
	if params != nil {
		for _, v := range params {
			if err := v.Check(); err != nil {
				return nil, apierrors.ErrRenderPipelineTemplate.InvalidParameter(fmt.Sprintf("snippet %s definition params name can not empty", request.Spec.Name))
			}
		}
	}

	pipelineYaml, outputs, err := pipelineyml.DoRenderTemplateWithFormat(request.Params, request.Spec, request.Alias, request.TemplateVersion)
	if err != nil {
		return nil, apierrors.ErrRenderPipelineTemplate.InternalError(err)
	}

	render := apistructs.PipelineTemplateRender{
		PipelineYaml: pipelineYaml,
		Version:      nil,
		Outputs:      outputs,
	}
	return &render, nil

}

func RenderTemplate(specYaml string, request apistructs.PipelineTemplateRenderRequest) (string, []apistructs.SnippetFormatOutputs, error) {
	params := request.Params
	if params == nil {
		params = map[string]interface{}{}
	}

	alias := request.Alias

	var templateAction apistructs.PipelineTemplateSpec
	if err := yaml.Unmarshal([]byte(specYaml), &templateAction); err != nil {
		logrus.Errorf("Unmarshal specYaml error: %v, yaml: %s", err, specYaml)
		return "", nil, err
	}

	return pipelineyml.DoRenderTemplateWithFormat(params, &templateAction, alias, request.TemplateVersion)
}
