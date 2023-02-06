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

package extension

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/erda-project/erda-proto-go/core/dicehub/extension/pb"
	"github.com/erda-project/erda/pkg/i18n"
)

const DicehubExtensionsMenu = "dicehub.extensions.menu"

var CategoryTypes = map[string][]string{
	"action": {
		"source_code_management",
		"build_management",
		"deploy_management",
		"version_management",
		"test_management",
		"data_management",
		"custom_task",
	},
	"addon": {
		"database",
		"distributed_cooperation",
		"search",
		"message",
		"content_management",
		"security",
		"traffic_load",
		"monitoring&logging",
		"content",
		"image_processing",
		"document_processing",
		"sound_processing",
		"custom",
		"general_ability",
		"new_retail",
		"srm",
		"solution",
	},
}

type MenuItem struct {
	Name string `json:"name"`
}

func menuExtWithLocale(extensions []*pb.Extension, locale *i18n.LocaleResource) map[string][]pb.ExtensionMenu {
	var result = map[string][]pb.ExtensionMenu{}

	extMap := extMap(extensions)
	//Traverse the big category
	for categoryType, typeItems := range CategoryTypes {
		//Each category belongs to a map
		menuList := result[categoryType]
		//Traverse the subcategories in the big category
		for _, v := range typeItems {
			//Obtain the object data of this subcategory Extension
			extensionListWithKeyName, ok := extMap[v]
			if !ok {
				continue
			}
			//Whether this subcategory is internationalized, if not, it is the name of the word category
			var displayName string
			if locale != nil {
				displayNameTemplate := locale.GetTemplate(DicehubExtensionsMenu + "." + categoryType + "." + v)
				if displayNameTemplate != nil {
					displayName = displayNameTemplate.Content()
				}
			}

			if displayName == "" {
				displayName = v
			}
			// Assign these word categories to the array
			menuList = append(menuList, pb.ExtensionMenu{
				Name:        v,
				DisplayName: displayName,
				Items:       extensionListWithKeyName,
			})
		}
		// set array to map
		result[categoryType] = menuList
	}
	return result
}

func menuExt(extensions []*pb.Extension, confMenu map[string][]string) interface{} {
	extMap := extMap(extensions)
	menuMap := &MenuMap{}
	for subMenuName, subMenuValues := range confMenu {
		subMenu := &MenuMap{}
		for _, v := range subMenuValues {
			params := strings.Split(v, ":")
			keyName := params[0]
			displayName := params[1]
			subMenus := getMapValue(extMap, keyName)
			if len(subMenus) > 0 {
				subMenu.Put(displayName, subMenus)
			}
		}
		menuMap.Put(subMenuName, subMenu)
	}
	return menuMap
}

func getMapValue(extMap map[string][]*pb.Extension, key string) []*pb.Extension {
	extList, _ := extMap[key]
	return extList
}

func extMap(extensions []*pb.Extension) map[string][]*pb.Extension {
	extMap := map[string][]*pb.Extension{}
	for _, v := range extensions {
		extList, exist := extMap[v.Category]
		if exist {
			extList = append(extList, v)
		} else {
			extList = []*pb.Extension{v}
		}
		extMap[v.Category] = extList
	}
	return extMap
}

type MenuMap []*SortMapNode

type SortMapNode struct {
	Key string
	Val interface{}
}

func (m *MenuMap) Put(key string, val interface{}) {
	index, _, ok := m.get(key)
	if ok {
		(*m)[index].Val = val
	} else {
		node := &SortMapNode{Key: key, Val: val}
		*m = append(*m, node)
	}
}

func (m *MenuMap) Get(key string) (interface{}, bool) {
	_, val, ok := m.get(key)
	return val, ok
}

func (m *MenuMap) get(key string) (int, interface{}, bool) {
	for index, node := range *m {
		if node.Key == key {
			return index, node.Val, true
		}
	}
	return -1, nil, false
}
func (m *MenuMap) MarshalJSON() ([]byte, error) {
	mapJson := m.ToSortedMapJson(m)
	return []byte(mapJson), nil
}

func (m *MenuMap) ToSortedMapJson(smap *MenuMap) string {
	s := "{"
	for _, node := range *smap {
		v := node.Val
		isSamp := false
		str := ""
		switch v.(type) {
		case *MenuMap:
			isSamp = true
			str = smap.ToSortedMapJson(v.(*MenuMap))
		}

		if !isSamp {
			b, _ := json.Marshal(node.Val)
			str = string(b)
		}

		s = fmt.Sprintf("%s\"%s\":%s,", s, node.Key, str)
	}
	s = strings.TrimRight(s, ",")
	s = fmt.Sprintf("%s}", s)
	return s
}
