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

package db

import (
	"fmt"
	"strings"
	"time"

	"github.com/xormplus/builder"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"
	"sigs.k8s.io/yaml"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pexpr"
	"github.com/erda-project/erda/pkg/strutil"
)

type PipelineAction struct {
	ID            string    `json:"id" xorm:"pk"`
	TimeCreated   time.Time `json:"timeCreated,omitempty" xorm:"created_at created"`
	TimeUpdated   time.Time `json:"timeUpdated,omitempty" xorm:"updated_at updated"`
	Name          string    `json:"name,omitempty" xorm:"name"`
	Category      string    `json:"category,omitempty" xorm:"category"`
	DisplayName   string    `json:"displayName,omitempty" xorm:"display_name"`
	LogoUrl       string    `json:"logoUrl,omitempty" xorm:"logo_url"`
	Desc          string    `json:"desc,omitempty" xorm:"desc"`
	Readme        string    `json:"readme,omitempty" xorm:"readme"`
	Dice          string    `json:"dice,omitempty" xorm:"dice"`
	Spec          string    `json:"spec,omitempty" xorm:"spec"`
	VersionInfo   string    `json:"versionInfo,omitempty" xorm:"version_info"`
	Location      string    `json:"location,omitempty" xorm:"location"`
	IsPublic      bool      `json:"isPublic,omitempty" xorm:"is_public"`
	IsDefault     bool      `json:"isDefault,omitempty" xorm:"is_default"`
	SoftDeletedAt int64     `json:"softDeletedAt,omitempty" xorm:"soft_deleted_at"`
}

const localeSpecEntry = "locale"
const displayNameKey = "displayName"
const descKey = "desc"

func (action *PipelineAction) Convert(yamlFormat bool) (*pb.Action, error) {
	actionDto := &pb.Action{
		ID:          action.ID,
		TimeCreated: timestamppb.New(action.TimeCreated),
		TimeUpdated: timestamppb.New(action.TimeUpdated),
		Name:        action.Name,
		Category:    action.Category,
		DisplayName: action.DisplayName,
		LogoUrl:     action.LogoUrl,
		Desc:        action.Desc,
		Readme:      action.Readme,
		Version:     action.VersionInfo,
		Location:    action.Location,
		IsPublic:    action.IsPublic,
		IsDefault:   action.IsDefault,
		IsDelete: func() bool {
			if action.SoftDeletedAt > 0 {
				return true
			}
			return false
		}(),
		SoftDeletedAt: func() *timestamppb.Timestamp {
			if action.SoftDeletedAt == 0 {
				return nil
			}
			return timestamppb.New(time.Unix(action.SoftDeletedAt/1e3, 0))
		}(),
	}

	var specInterface = map[string]interface{}{}
	withLocaleInfo, specInfo := SpecI18nReplace(action.Spec)
	if !withLocaleInfo {
		err := yaml.Unmarshal([]byte(action.Spec), &specInterface)
		if err != nil {
			return nil, err
		}
	} else {
		v, ok := specInfo.(map[string]interface{})
		if !ok {
			return nil, fmt.Errorf("spec was not map struct")
		}
		specInterface = v
	}

	displayName, ok := specInterface[displayNameKey].(string)
	if ok {
		actionDto.DisplayName = displayName
	}

	desc, ok := specInterface[descKey].(string)
	if ok {
		actionDto.Desc = desc
	}

	if yamlFormat {
		specYmlInfo, err := yaml.Marshal(specInterface)
		if err != nil {
			return nil, err
		}
		specPbValue, err := structpb.NewValue(string(specYmlInfo))
		if err != nil {
			return nil, err
		}
		actionDto.Spec = specPbValue
		actionDto.Dice = structpb.NewStringValue(action.Dice)
	} else {
		specPbValue, err := structpb.NewValue(specInterface)
		if err != nil {
			return nil, err
		}
		actionDto.Spec = specPbValue

		var dice = map[string]interface{}{}
		err = yaml.Unmarshal([]byte(action.Dice), &dice)
		if err != nil {
			return nil, err
		}
		dicePbValue, err := structpb.NewValue(dice)
		if err != nil {
			return nil, err
		}
		actionDto.Dice = dicePbValue
	}

	return actionDto, nil
}

func SpecI18nReplace(specYaml string) (withLocaleInfo bool, spec interface{}) {
	localeName := i18n.GetGoroutineBindLang()
	if localeName == "" {
		localeName = i18n.ZH
	}
	var specData map[string]interface{}
	if err := yaml.Unmarshal([]byte(specYaml), &specData); err != nil {
		return
	}
	localeEntry, ok := specData[localeSpecEntry]
	if !ok {
		return
	}
	l, ok := localeEntry.(map[string]interface{})
	if !ok {
		return
	}
	locale, ok := l[localeName]
	if !ok {
		return
	}
	localeMap, ok := locale.(map[string]interface{})
	if !ok {
		return
	}
	withLocaleInfo = true
	spec = dfs(specData, localeMap)
	return
}

func dfs(obj interface{}, locale map[string]interface{}) interface{} {
	switch obj.(type) {
	case string:
		return strutil.ReplaceAllStringSubmatchFunc(pexpr.PhRe, obj.(string), func(v []string) string {
			if len(v) == 2 && strings.HasPrefix(v[1], expression.I18n+".") {
				key := strings.TrimPrefix(v[1], expression.I18n+".")
				if len(key) > 0 {
					if r, ok := locale[key]; ok {
						return r.(string)
					}
					return v[0]
				}
			}
			return v[0]
		})
	case map[string]interface{}:
		m := obj.(map[string]interface{})
		for i, v := range m {
			if i == localeSpecEntry {
				continue
			}
			m[i] = dfs(v, locale)
		}
		return m
	case []interface{}:
		l := obj.([]interface{})
		for i, v := range l {
			l[i] = dfs(v, locale)
		}
		return l
	default:
		return obj
	}
}

func (PipelineAction) TableName() string {
	return "erda_pipeline_action"
}

func (client *Client) ListPipelineAction(req *pb.PipelineActionListRequest, ops ...mysqlxorm.SessionOption) ([]PipelineAction, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var (
		pipelineActions []PipelineAction
		err             error
	)

	engine := session.Table(PipelineAction{}).Where("soft_deleted_at = 0")
	if req.Locations != nil {
		engine = engine.In("location", req.Locations)
	}
	if req.Categories != nil {
		engine = engine.In("category", req.Categories)
	}
	if req.IsPublic {
		engine = engine.Where("is_public = ?", req.IsPublic)
	}
	if req.ActionNameWithVersionQuery != nil {
		cond := builder.NewCond()
		for _, query := range req.ActionNameWithVersionQuery {
			if query.Name == "" {
				continue
			}

			var queryMap = map[string]interface{}{}
			queryMap["name"] = query.Name

			if query.Version != "" {
				queryMap["version_info"] = query.Version
			}
			if query.IsDefault {
				queryMap["is_default"] = true
			}
			if query.LocationFilter != "" {
				queryMap["location"] = query.LocationFilter
			}

			cond = cond.Or(builder.Eq(queryMap))
		}
		sqlBuild, args, _ := builder.ToSQL(cond)
		engine = engine.Where(sqlBuild, args...)
	}

	engine = engine.OrderBy("version_info desc")
	err = engine.Find(&pipelineActions)
	if err != nil {
		return nil, err
	}

	return pipelineActions, nil
}

func (client *Client) InsertPipelineAction(action *PipelineAction, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.InsertOne(action)

	return err
}

func (client *Client) UpdatePipelineAction(id string, action *PipelineAction, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).AllCols().Update(action)
	return err
}

func (client *Client) DeletePipelineAction(id string, action *PipelineAction, ops ...mysqlxorm.SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()
	_, err := session.ID(id).Cols("soft_deleted_at").Update(action)
	return err
}
