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

package gitmodule

import (
	"bufio"
	"errors"
	"fmt"
	"strings"
)

type SubModuleData struct {
	UrlMap map[string]string
	Exist  bool
}

func (repo *Repository) GetSubmodules(commitID string) (map[string]string, error) {
	key := fmt.Sprintf("%v-%v", repo.ID, commitID)
	var submoduleData SubModuleData
	err := Setting.RepoSubmoduleCache.Get(key, &submoduleData)
	//有缓存但是为空,直接返回
	if err == nil && submoduleData.Exist == false {
		if submoduleData.Exist {
			return submoduleData.UrlMap, nil
		} else {
			return nil, errors.New("submodule not found")
		}
	}
	treeEntry, err := repo.GetTreeEntryByPath(commitID, ".gitmodules")
	if err == ERROR_PATH_NOT_FOUND {
		Setting.RepoSubmoduleCache.Set(key, SubModuleData{
			UrlMap: nil,
			Exist:  false,
		})
		return nil, err
	}
	if err != nil {
		return nil, err
	}
	rd, err := treeEntry.Blob().Data()
	if err != nil {
		return nil, err
	}

	submoduleMap := map[string]string{}
	scanner := bufio.NewScanner(rd)
	var ismodule bool
	var path string
	for scanner.Scan() {
		if strings.HasPrefix(scanner.Text(), "[submodule") {
			ismodule = true
			continue
		}
		if ismodule {
			fields := strings.Split(scanner.Text(), "=")
			k := strings.TrimSpace(fields[0])
			if k == "path" {
				path = strings.TrimSpace(fields[1])
			} else if k == "url" {
				submoduleMap[path] = strings.TrimSpace(fields[1])
				ismodule = false
			}
		}
	}
	Setting.RepoSubmoduleCache.Set(key, SubModuleData{
		UrlMap: submoduleMap,
		Exist:  true,
	})
	return submoduleMap, nil
}

func (repo *Repository) FillSubmoduleInfo(commitID string, entries ...*TreeEntry) {
	submodules, err := repo.GetSubmodules(commitID)
	if err != nil {
		return
	}
	for _, entry := range entries {
		if entry.Type == OBJECT_COMMIT {
			if err == nil {
				url, ok := submodules[entry.Name]
				if ok {
					entry.Url = url
				}
			}
		}
	}
}
