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

package exdto

type PackageInfoDto struct {
	Id       string `json:"id"`
	CreateAt string `json:"createAt"`
	PackageDto
}

type SortBySceneList []PackageInfoDto

func (list SortBySceneList) Len() int { return len(list) }

func (list SortBySceneList) Swap(i, j int) { list[i], list[j] = list[j], list[i] }

func (list SortBySceneList) Less(i, j int) bool {
	if list[i].Scene == "unity" {
		return true
	}
	if list[j].Scene == "unity" {
		return false
	}
	return list[i].CreateAt > list[j].CreateAt
}
