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

package gitflowutil

import (
	"strings"

	"github.com/pkg/errors"
)

const (
	MASTER                = "master"
	HOTFIX                = "hotfix/"
	HOTFIX_WITHOUT_SLASH  = "hotfix"
	SUPPORT               = "support/"
	SUPPORT_WITHOUT_SLASH = "support"
	RELEASE               = "release/"
	RELEASE_WITHOUT_SLASH = "release"
	DEVELOP               = "develop"
	FEATURE               = "feature/"
	FEATURE_WITHOUT_SLASH = "feature"

	DEFAULT = "default"
)

// DiceWorkspace dice 部署环境：DEV、TEST、STAGING、PROD
const (
	DefaultWorkspace string = "DEFAULT"
	// DevWorkspace 开发环境
	DevWorkspace string = "DEV"
	// TestWorkspace 测试环境
	TestWorkspace string = "TEST"
	// StagingWorkspace 预发环境
	StagingWorkspace string = "STAGING"
	// ProdWorkspace 生产环境
	ProdWorkspace string = "PROD"
)

func ErrorNotSupportedReference(reference string) error {
	return errors.Errorf("not supported reference [%s], must be one of "+
		"[PROD: master, support/*], "+
		"[STAGING: release/*, hotfix/*, {semver}, v{semver}], "+
		"[TEST: develop], "+
		"[DEV: feature/*]", reference)
}

func IsMaster(branch string) bool {
	return branch == MASTER
}

func IsHotfix(branch string) bool {
	return isXXXSlash(branch, HOTFIX)
}

func IsSupport(branch string) bool {
	return isXXXSlash(branch, SUPPORT)
}

func IsRelease(branch string) bool {
	return isXXXSlash(branch, RELEASE)
}

func IsDevelop(branch string) bool {
	return branch == DEVELOP
}

func IsFeature(branch string) bool {
	return isXXXSlash(branch, FEATURE)
}

func IsValid(reference string) bool {
	return IsMaster(reference) ||
		IsHotfix(reference) ||
		IsSupport(reference) ||
		IsRelease(reference) || IsReleaseTag(reference) ||
		IsDevelop(reference) ||
		IsFeature(reference)
}

func GetReferencePrefix(reference string) (string, error) {
	if !IsValid(reference) {
		return "", ErrorNotSupportedReference(reference)
	}
	if IsMaster(reference) {
		return MASTER, nil
	}
	switch true {
	case IsMaster(reference):
		return MASTER, nil
	case IsHotfix(reference):
		return HOTFIX_WITHOUT_SLASH, nil
	case IsSupport(reference):
		return SUPPORT_WITHOUT_SLASH, nil
	case IsRelease(reference), IsReleaseTag(reference):
		return RELEASE_WITHOUT_SLASH, nil
	case IsDevelop(reference):
		return DEVELOP, nil
	case IsFeature(reference):
		return FEATURE_WITHOUT_SLASH, nil
	default:
		return "", ErrorNotSupportedReference(reference)
	}
}

// map:
//   key: prefix with slash
//   value: prefix with mock
type PrefixAndBranch struct {
	Workspace string
	Branch    string
}

func ListAllBranchPrefix() []PrefixAndBranch {
	return []PrefixAndBranch{
		{ProdWorkspace, MASTER},
		{ProdWorkspace, mockBranchPrefix(HOTFIX)},
		{ProdWorkspace, mockBranchPrefix(SUPPORT)},
		{StagingWorkspace, mockBranchPrefix(RELEASE)},
		{TestWorkspace, DEVELOP},
		{DevWorkspace, mockBranchPrefix(FEATURE)},
	}
}

func mockBranchPrefix(prefix string) string {
	return prefix + "mock"
}

func isXXXSlash(branch string, target string) bool {
	return strings.HasPrefix(branch, target) && len(branch) > len(target)
}
