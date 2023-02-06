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

package pipelineyml

type Source map[string]interface{}

type Params map[string]interface{}

type Version map[string]string

type Metadata []MetadataField

type MetadataField struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}

const (
	RES_TYPE_BUILDPACK = "buildpack"
	RES_TYPE_BP_IMAGE  = "bp-image"
	RES_TYPE_SONAR     = "sonar"
	RES_TYPE_UT        = "ut"

	RES_TYPE_GIT = "git"
)
