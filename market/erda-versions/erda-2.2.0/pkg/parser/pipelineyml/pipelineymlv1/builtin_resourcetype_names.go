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

// DO NOT EDIT!!!
// See go:generate at github.com/erda-project/erda/internal/tools/pipeline/pipelineyml/builtin_resourcetype.go

package pipelineymlv1

var BuiltinResTypeNames = []string{
	string(RES_TYPE_GIT),
	string(RES_TYPE_BUILDPACK),
	string(RES_TYPE_BP_COMPILE),
	string(RES_TYPE_BP_IMAGE),
	string(RES_TYPE_DICE),
	string(RES_TYPE_ABILITY),
	string(RES_TYPE_ADDON_REGISTRY),
	string(RES_TYPE_DICEHUB),
	string(RES_TYPE_IT),
	string(RES_TYPE_SONAR),
	string(RES_TYPE_FLINK),
	string(RES_TYPE_SPARK),
	string(RES_TYPE_UT),
}
