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

package apidocsvc

import (
	"encoding/json"
	"fmt"
	"path"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/cache/org"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/swagger"
	"github.com/erda-project/erda/pkg/swagger/oas3"
	"github.com/erda-project/erda/pkg/swagger/oasconv"
)

const oas3Text = `{
  "openapi": "3.0.0",
  "info": {
    "title": "%s",
    "description": "# API 设计中心创建的 API 文档\n\n请在『API 概况』中填写 API 文档的基本信息；在『API列表』新增接口描述；在『数据类型』中定义要引用的数据结构。\n",
    "version": "default"
  },
  "paths": {
    "/new-resource": {}
  },
  "tags": [
  	{
   		"name": "其他"
  	}
 ]
}
`
const oas3TextEn = `{
  "openapi": "3.0.0",
  "info": {
    "title": "%s",
    "description": "# The API Doc Creating From Desigin Center\n\nPlease write the base information in API Overview；add URI in API List；define data structure in Data Types.\n",
    "version": "default"
  },
  "paths": {
    "/new-resource": {}
  },
  "tags": [
  	{
   		"name": "other"
  	}
 ]
}
`

func FetchAPIDocContent(orgID uint64, userID, inode string, specProtocol oasconv.Protocol, branchRuleSvc *branchrule.BranchRule) (*apistructs.FileTreeNodeRspData,
	*errorresp.APIError) {
	// 解析路径
	ft, err := bundle.NewGittarFileTree(inode)
	if err != nil {
		return nil, apierrors.GetNodeDetail.InternalError(err)
	}
	appID, err := strconv.ParseUint(ft.ApplicationID(), 10, 64)
	if err != nil {
		return nil, apierrors.GetNodeDetail.InvalidParameter(err)
	}

	blob, err := bdl.Bdl.GetGittarBlobNodeInfo(ft.BlobPath(), strconv.FormatUint(orgID, 10), userID)
	if err != nil {
		return nil, apierrors.GetNodeDetail.InternalError(err)
	}

	// gittar 仓库的文件是 .yaml, 根据参数要转换格式
	switch specProtocol {
	case oasconv.OAS3YAML:
	case oasconv.OAS3JSON:
		data, err := oasconv.YAMLToJSON([]byte(blob.Content))
		if err != nil {
			return nil, apierrors.GetNodeDetail.InternalError(errors.New("failed to convert doc to oas3-json"))
		}
		blob.Content = string(data)
		blob.Path = MustSuffix(blob.Path, ".json")
	case oasconv.OAS2YAML:
		v3, err := swagger.LoadFromData([]byte(blob.Content))
		if err == nil {
			return nil, apierrors.GetNodeDetail.InternalError(errors.New("failed to load oas3 from doc, content is invalid oas3 doc"))
		}
		v2, err := oasconv.OAS3ConvTo2(v3)
		if err != nil {
			return nil, apierrors.GetNodeDetail.InternalError(errors.Wrap(err, "failed to convert doc to oas2-yaml"))
		}
		data, err := json.Marshal(v2)
		if err != nil {
			return nil, apierrors.GetNodeDetail.InternalError(errors.Wrap(err, "failed to convert doc to oas2-yaml"))
		}
		data, err = oasconv.JSONToYAML(data)
		if err != nil {
			return nil, apierrors.GetNodeDetail.InternalError(errors.Wrap(err, "failed to convert doc to oas2-yaml"))
		}
		blob.Content = string(data)
		blob.Path = MustSuffix(blob.Path, ".yaml")
	case oasconv.OAS2JSON:
		v3, err := swagger.LoadFromData([]byte(blob.Content))
		if err == nil {
			return nil, apierrors.GetNodeDetail.InternalError(errors.Wrap(err, "failed to load oas3 from doc, content is invalid oas3 doc"))
		}
		v2, err := oasconv.OAS3ConvTo2(v3)
		if err != nil {
			return nil, apierrors.GetNodeDetail.InternalError(errors.Wrap(err, "failed to load oas3 from doc, content is invalid oas3 doc"))
		}
		data, err := json.Marshal(v2)
		if err != nil {
			return nil, apierrors.GetNodeDetail.InternalError(errors.New("failed to convert doc to oas2-json"))
		}
		blob.Content = string(data)
		blob.Path = MustSuffix(blob.Path, ".json")
	default:
		return nil, apierrors.GetNodeDetail.InvalidParameter(errors.Errorf("invalid specProtocol: %s", specProtocol))
	}

	isManager, _ := bdl.IsManager(userID, apistructs.AppScope, appID)
	readOnly := !isManager && isBranchProtected(ft.ApplicationID(), ft.BranchName(), branchRuleSvc)

	meta := apistructs.APIDocMeta{
		Lock:     nil,
		Tree:     nil,
		Blob:     blob,
		ReadOnly: readOnly,
	}
	metaData, _ := json.Marshal(meta)

	var data = apistructs.FileTreeNodeRspData{
		Type:      "f",
		Inode:     inode,
		Pinode:    ft.Clone().DeletePathFromRepoRoot().Inode(),
		Scope:     "application",
		ScopeID:   ft.ApplicationID(),
		Name:      MustSuffix(path.Base(ft.PathFromRepoRoot()), ""),
		CreatorID: "",
		UpdaterID: "",
		Meta:      json.RawMessage(metaData),
	}

	return &data, nil
}

func CommitAPIDocCreation(orgID uint64, userID, repo, commitMessage, serviceName, content, branch string) error {
	return commitAPIDocContent(orgID, userID, repo, commitMessage, serviceName, content, branch, "add")
}

func CommitAPIDocModifies(orgID uint64, userID, repo, commitMessage, serviceName, content, branch string) error {
	return commitAPIDocContent(orgID, userID, repo, commitMessage, serviceName, content, branch, "update")
}

// 提交 API 文档内容到 gittar
// 如果文档内容为空, 则填充默认内容;
// 前端提交的文档格式为 json, 转换为 yaml 后再提交.
func commitAPIDocContent(orgID uint64, userID, repo, commitMessage, serviceName, content, branch, action string) error {
	// if the content is empty, set default
	if content == "" {
		content = defaultOAS3Content(strconv.FormatUint(orgID, 10), serviceName)
	}

	// load structural Openapi 3
	v3, err := oas3.LoadFromData([]byte(content))
	if err != nil {
		return errors.Wrap(err, "failed to load Openapi 3 structural from content")
	}

	// trans to yaml
	data, err := oas3.MarshalYaml(v3)
	if err != nil {
		return errors.Wrap(err, "failed to MarshalYaml")
	}
	content = string(data)

	// change filename suffix to .yaml
	serviceName = MustSuffix(serviceName, suffixYaml)
	filenameFromRepoRoot := filepath.Join(apiDocsPathFromRepoRoot, serviceName)

	// make commit: .dice/apidocs/{docName}
	var commit = apistructs.GittarCreateCommitRequest{
		Message: commitMessage,
		Actions: []apistructs.EditActionItem{{
			Action:   action,
			Content:  content,
			Path:     filenameFromRepoRoot,
			PathType: "blob",
		}},
		Branch: branch,
	}

	resp, err := bdl.Bdl.CreateGittarCommitV3(orgID, userID, repo, &commit)
	if err != nil {
		return errors.Wrap(err, "failed to CreateGittarCommitV3")
	}
	if !resp.Success {
		return errors.Errorf("not success to CreateGittarCommitV3, err: %+v", resp.Error)
	}
	return nil
}

func MustSuffix(filename, suffix string) string {
	return strings.TrimSuffix(filename, filepath.Ext(filename)) + suffix
}

func MatchSuffix(filename string, suffix ...string) bool {
	ext := strings.ToLower(filepath.Ext(filename))
	for _, s := range suffix {
		if s == ext {
			return true
		}
	}
	return false
}

func defaultOAS3Content(orgID string, title string) string {
	if orgDTO, ok := org.GetOrgByOrgID(orgID); ok && strings.Contains(orgDTO.Locale, "en") {
		return fmt.Sprintf(oas3TextEn, title)
	}
	return fmt.Sprintf(oas3Text, title)
}

func isBranchProtected(applicationID, branchName string, branchRuleSvc *branchrule.BranchRule) bool {
	rules, err := getRules(applicationID, branchRuleSvc)
	if err != nil || len(rules) == 0 {
		return false
	}

	for _, rule := range rules {
		if !rule.IsProtect {
			continue
		}
		branches := strings.Split(rule.Rule, ",")
		for _, pat := range branches {
			if match, _ := filepath.Match(pat, branchName); match {
				return true
			}
		}
	}

	return false
}

func getRules(applicationID string, branchRuleSvc *branchrule.BranchRule) ([]*apistructs.BranchRule, error) {
	// 查询文档是否只读
	appID, err := strconv.ParseUint(applicationID, 10, 64)
	if err != nil {
		return nil, errors.New("invalid inode: can not parse app id to uint")
	}

	rules, err := branchRuleSvc.Query(apistructs.AppScope, int64(appID))
	if err != nil {
		return nil, err
	}
	return rules, nil
}

// 该分支下是否有 API 文档
func branchHasAPIDoc(orgID uint64, branchInode string) bool {
	ft, err := bundle.NewGittarFileTree(branchInode)
	if err != nil {
		return false
	}
	ft.SetPathFromRepoRoot(apiDocsPathFromRepoRoot)
	orgIDStr := strconv.FormatUint(orgID, 10)
	treeData, err := bdl.Bdl.GetGittarTreeNode(ft.TreePath(), orgIDStr, true, "")
	if err != nil {
		return false
	}
	for _, node := range treeData.Entries {
		// 如果 .dice/apidocs 目录下存在 .yaml 或 .yml 的文件 则认为该分支下存在文档
		if node.Type == "blob" && MatchSuffix(node.Name, suffixYaml, suffixYaml) {
			return true
		}
	}
	return false
}
