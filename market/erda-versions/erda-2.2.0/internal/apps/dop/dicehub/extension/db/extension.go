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
	"encoding/json"
	"errors"
	"strings"

	"github.com/ghodss/yaml"
	"github.com/jinzhu/gorm"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/dicehub/extension/pb"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pexpr"
	"github.com/erda-project/erda/pkg/strutil"
)

const localeSpecEntry = "locale"

// ExtensionConfig .
type ExtensionConfigDB struct {
	*gorm.DB
}

func (ext *ExtensionVersion) ToApiData(typ string, yamlFormat bool) (*pb.ExtensionVersion, error) {
	if yamlFormat {
		return &pb.ExtensionVersion{
			Name:      ext.Name,
			Type:      typ,
			Version:   ext.Version,
			Dice:      structpb.NewStringValue(ext.Dice),
			Spec:      structpb.NewStringValue(ext.Spec),
			Swagger:   structpb.NewStringValue(ext.Swagger),
			Readme:    ext.Readme,
			CreatedAt: timestamppb.New(ext.CreatedAt),
			UpdatedAt: timestamppb.New(ext.UpdatedAt),
			IsDefault: ext.IsDefault,
			Public:    ext.Public,
		}, nil
	} else {
		diceData, err := yaml.YAMLToJSON([]byte(ext.Dice))
		if err != nil {
			return nil, err
		}
		swaggerData, err := yaml.YAMLToJSON([]byte(ext.Swagger))
		if err != nil {
			return nil, err
		}
		specData, err := yaml.YAMLToJSON([]byte(ext.Spec))
		if err != nil {
			return nil, err
		}

		var diceValue interface{}
		var swaggerValue interface{}
		var specValue interface{}

		err = json.Unmarshal(diceData, &diceValue)
		if err != nil {
			return nil, err
		}
		err = json.Unmarshal(swaggerData, &swaggerValue)
		if err != nil {
			return nil, err
		}
		err = json.Unmarshal(specData, &specValue)
		if err != nil {
			return nil, err
		}

		dice, err := structpb.NewValue(diceValue)
		if err != nil {
			return nil, err
		}
		swagger, err := structpb.NewValue(swaggerValue)
		if err != nil {
			return nil, err
		}
		spec, err := structpb.NewValue(specValue)
		if err != nil {
			return nil, err
		}

		withLocaleInfo, replaceSpecValue := ext.SpecI18nReplace()
		if withLocaleInfo {
			spec, err = structpb.NewValue(replaceSpecValue)
			if err != nil {
				return nil, err
			}
		}

		return &pb.ExtensionVersion{
			Name:      ext.Name,
			Type:      typ,
			Version:   ext.Version,
			Dice:      dice,
			Spec:      spec,
			Swagger:   swagger,
			Readme:    ext.Readme,
			CreatedAt: timestamppb.New(ext.CreatedAt),
			UpdatedAt: timestamppb.New(ext.UpdatedAt),
			IsDefault: ext.IsDefault,
			Public:    ext.Public,
		}, nil
	}
}

func (ext *ExtensionVersion) SpecI18nReplace() (withLocaleInfo bool, spec interface{}) {
	localeName := i18n.GetGoroutineBindLang()
	if localeName == "" {
		localeName = i18n.ZH
	}
	spec = ext.Spec
	var specData map[string]interface{}
	if err := yaml.Unmarshal([]byte(ext.Spec), &specData); err != nil {
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

func (client *ExtensionConfigDB) CreateExtension(extension *Extension) error {
	var cnt int64
	client.Model(&Extension{}).Where("name = ?", extension.Name).Count(&cnt)
	if cnt == 0 {
		err := client.Create(extension).Error
		return err
	} else {
		return errors.New("name already exist")
	}
}

func (client *ExtensionConfigDB) QueryExtensions(all bool, typ string, labels string) ([]Extension, error) {
	var result []Extension
	query := client.Model(&Extension{})

	// if all != true,only return data with public = true
	if !all {
		query = query.Where("public = ?", true)
	}

	if typ != "" {
		query = query.Where("type = ?", typ)
	}

	if labels != "" {
		labelPairs := strings.Split(labels, ",")
		for _, pair := range labelPairs {
			if strings.LastIndex(pair, "^") == 0 && len(pair) > 1 {
				query = query.Where("labels not like ?", "%"+pair[1:]+"%")
			} else {
				query = query.Where("labels like ?", "%"+pair+"%")
			}

		}
	}
	err := query.Find(&result).Error
	return result, err
}

func (client *ExtensionConfigDB) GetExtension(name string) (*Extension, error) {
	var result Extension
	err := client.Model(&Extension{}).Where("name = ?", name).Find(&result).Error
	return &result, err
}

func (client *ExtensionConfigDB) DeleteExtension(name string) error {
	return client.Where("name = ?", name).Delete(&Extension{}).Error
}

func (client *ExtensionConfigDB) GetExtensionVersion(name string, version string) (*ExtensionVersion, error) {
	var result ExtensionVersion
	err := client.Model(&ExtensionVersion{}).
		Where("name = ? ", name).
		Where("version = ?", version).
		Find(&result).Error
	return &result, err
}

func (client *ExtensionConfigDB) GetExtensionDefaultVersion(name string) (*ExtensionVersion, error) {
	var result ExtensionVersion
	err := client.Model(&ExtensionVersion{}).
		Where("name = ? ", name).
		Where("is_default = ? ", true).
		Limit(1).
		Find(&result).Error
	//no default,find latest update & public = true
	if err == gorm.ErrRecordNotFound {
		err = client.Model(&ExtensionVersion{}).
			Where("name = ? ", name).
			Where("public = ? ", true).
			Order("version desc").
			Limit(1).
			Find(&result).Error
	}
	return &result, err
}

func (client *ExtensionConfigDB) SetUnDefaultVersion(name string) error {
	return client.Model(&ExtensionVersion{}).
		Where("is_default = ?", true).
		Where("name = ?", name).
		Update("is_default", false).Error
}

func (client *ExtensionConfigDB) CreateExtensionVersion(version *ExtensionVersion) error {
	return client.Create(version).Error
}

func (client *ExtensionConfigDB) DeleteExtensionVersion(name, version string) error {
	return client.Where("name = ? and version =?", name, version).Delete(&ExtensionVersion{}).Error
}

func (client *ExtensionConfigDB) QueryExtensionVersions(name string, all bool, orderByVersionDesc bool) ([]ExtensionVersion, error) {
	var result []ExtensionVersion
	query := client.Model(&ExtensionVersion{}).
		Where("name = ?", name)
	// if all != true,only return data with public = true
	if !all {
		query = query.Where("public = ?", true)
	}
	if orderByVersionDesc {
		query = query.Order("version desc")
	}
	err := query.Find(&result).Error
	return result, err
}

func (client *ExtensionConfigDB) GetExtensionVersionCount(name string) (int64, error) {
	var count int64
	err := client.Model(&ExtensionVersion{}).
		Where("name = ? ", name).
		Count(&count).Error
	return count, err
}

func (client *ExtensionConfigDB) QueryAllExtensions() ([]ExtensionVersion, error) {
	var result []ExtensionVersion
	err := client.Model(&ExtensionVersion{}).Find(&result).Error
	if err != nil {
		return nil, err
	}
	return result, nil
}

func (client *ExtensionConfigDB) ListExtensionVersions(names []string, all bool) (map[string][]ExtensionVersion, error) {
	var result []ExtensionVersion
	query := client.Model(&ExtensionVersion{}).Order("version desc")
	if !all {
		query = query.Where("public = ?", true)
	}
	err := query.Find(&result).Error
	if err != nil {
		return nil, err
	}

	namesMap := make(map[string]struct{})
	for _, name := range names {
		namesMap[name] = struct{}{}
	}

	var extensions = map[string][]ExtensionVersion{}
	for _, extVersion := range result {
		if _, ok := namesMap[extVersion.Name]; ok {
			extensions[extVersion.Name] = append(extensions[extVersion.Name], extVersion)
		}
	}

	return extensions, err
}
