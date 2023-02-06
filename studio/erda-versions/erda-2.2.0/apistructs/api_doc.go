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

package apistructs

import (
	"encoding/json"
	"fmt"
	"time"
)

type NodeType string

const (
	NodeTypeDir  NodeType = "d"
	NodeTypeFile NodeType = "f"
)

type APIDocLockModel struct {
	ID            uint64    `json:"id"`
	CreatedAt     time.Time `json:"createAt"`
	UpdatedAt     time.Time `json:"updatedAt"`
	SessionID     string    `json:"sessionID"`
	IsLocked      bool      `json:"isLocked"`
	ExpiredAt     time.Time `json:"expiredAt"`
	ApplicationID uint64    `json:"applicationID"`
	BranchName    string    `json:"branchName"`
	DocName       string    `json:"docName"`
	CreatorID     string    `json:"creatorID"`
	UpdaterID     string    `json:"updaterID"`
}

func (m APIDocLockModel) TableName() string {
	return "dice_api_doc_lock"
}

type APIDocTmpContentModel struct {
	ID            uint64    `json:"id"`
	CreatedAt     time.Time `json:"createAt"`
	UpdatedAt     time.Time `json:"updatedAt"`
	ApplicationID uint64    `json:"applicationID"`
	BranchName    string    `json:"branchName"`
	DocName       string    `json:"docName"`
	Content       string    `json:"content"`
	CreatorID     string    `json:"creatorID"`
	UpdaterID     string    `json:"updaterID"`
}

func (m APIDocTmpContentModel) TableName() string {
	return "dice_api_doc_tmp_content"
}

type APIDocCreateNodeReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *FileTreeDetailURI
	Body      *APIDocCreateUpdateNodeBody
}

type APIDocCreateUpdateNodeBody struct {
	Type   NodeType        `json:"type"`
	Pinode string          `json:"pinode"`
	Name   string          `json:"name"`
	Meta   json.RawMessage `json:"meta,omitempty"`
}

type APIDocDeleteNodeReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *FileTreeDetailURI
}

type APIDocUpdateNodeReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *FileTreeDetailURI
	Body      *RenameAPIDocBody
}

type FileTreeNodeRspData struct {
	Type      NodeType        `json:"type"`
	Inode     string          `json:"inode"`
	Pinode    string          `json:"pinode"`
	Scope     string          `json:"scope"`
	ScopeID   string          `json:"scopeID"`
	Name      string          `json:"name"`
	CreatorID string          `json:"creatorID"`
	UpdaterID string          `json:"updaterID"`
	Meta      json.RawMessage `json:"meta,omitempty"`
}

type APIDocMvCpNodeReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *FileTreeActionURI
	Body      *APIDocMvCpNodeReqBody
}

type APIDocMvCpNodeReqBody struct {
	Pinode string `json:"pinode"`
}

// 子节点列表
type APIDocListChildrenReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	URIParams   *FileTreeDetailURI
	QueryParams *FileTreeQueryParameters
}

type FileTreeQueryParameters struct {
	Pinode  string `json:"pinode"`
	Scope   string `json:"scope"`
	ScopeID string `json:"scopeID"`
}

type APIDocNodeDetailReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *FileTreeDetailURI
}

type FileTreeDetailURI struct {
	TreeName string
	Inode    string
}

type FileTreeActionURI struct {
	TreeName string
	Inode    string
	Action   string
}

type CreateAPIDocMeta struct {
	Content string `json:"content"`
}

type RenameAPIDocBody struct {
	Name string `json:"name"`
}

type APIDocMeta struct {
	Lock     *APIDocMetaLock      `json:"lock,omitempty"`
	Asset    *APIDocMetaAssetInfo `json:"asset"`
	Tree     *GittarTreeRspData   `json:"tree,omitempty"`
	Blob     *GittarBlobRspData   `json:"blob,omitempty"`
	ReadOnly bool                 `json:"readOnly"`
	Valid    bool                 `json:"valid"`
	Error    string               `json:"error,omitempty"`
}

type APIDocMetaLock struct {
	Locked   bool   `json:"locked"`
	UserID   string `json:"userID"`
	NickName string `json:"nickName"`
}

type APIDocMetaAssetInfo struct {
	AssetName string `json:"assetName"`
	AssetID   string `json:"assetID"`
	Major     uint64 `json:"major"`
	Minor     uint64 `json:"minor"`
	Patch     uint64 `json:"patch"`
}

type BaseResponse struct {
	Success bool             `json:"success"`
	Err     *BaseResponseErr `json:"err,omitempty"`
	Data    json.RawMessage  `json:"data"`
}

type BaseResponseErr struct {
	Code string `json:"code"`
	Msg  string `json:"msg"`
}

func (e *BaseResponseErr) Error() string {
	if e == nil {
		return ""
	}
	return fmt.Sprintf("code: %s, msg: %s", e.Code, e.Msg)
}

type GittarTreeRspData struct {
	Binary     bool                     `json:"binary"`
	Commit     *GittarTreeRspDataCommit `json:"commit"`
	Entries    []interface{}            `json:"entries"`
	Path       string                   `json:"path"`
	ReadmeFile string                   `json:"readmeFile"`
	RefName    string                   `json:"refName"`
	TreeID     string                   `json:"treeId"`
	Type       string                   `json:"type"`
}

type GittarTreeRspDataCommit struct {
	ID            string            `json:"id"`
	Author        map[string]string `json:"author"`
	Committer     map[string]string `json:"committer"`
	CommitMessage string            `json:"commitMessage"`
	Parent        interface{}       `json:"parent"`
}

type GittarBlobRspData struct {
	Binary  bool   `json:"binary"`
	Content string `json:"content"`
	RefName string `json:"refName"`
	Path    string `json:"path"`
	Size    uint64 `json:"size"`
}

type PumpType string

const (
	PumpTypeHeartBeat              = "heart_beat"
	PumpTypeRequestAPIDocContent   = "request_api_doc_content"
	PumpTypeResponseAPIDocContent  = "response_api_doc_content"
	PumpTypeRequestAutoSaveAPIDoc  = "request_auth_save_api_doc"
	PumpTypeResponseAutoSaveAPIDoc = "response_auto_save_api_doc"
	PumpTypeRequestCommitAPIDoc    = "request_commit_api_doc"
	PumpTypeResponseCommitAPIDoc   = "response_commit_api_doc"
)

type WebsocketRequest struct {
	SessionID string          `json:"sessionID"`
	MessageID uint64          `json:"messageID"`
	Type      string          `json:"type"`
	CreatedAt time.Time       `json:"createdAt,omitempty"`
	Data      json.RawMessage `json:"data,omitempty"`
}

type WsAPIDocHeartBeatReqData struct {
	Pinode string `json:"pinode"`
	Inode  string `json:"inode"`
}

type WsAPIDocAutoSaveReqData struct {
	Pinode  string `json:"pinode"`
	Inode   string `json:"inode"`
	Content string `json:"content"`
}

type WsAPIDocHandShakeReq struct {
	OrgID     uint64
	Identity  *IdentityInfo
	URIParams *FileTreeDetailURI
}

type ListSchemasReq struct {
	OrgID       uint64
	Identity    *IdentityInfo
	QueryParams *ListSchemasQueryParams
}

type ListSchemasQueryParams struct {
	// branch 节点的 inode,
	// 在实现中, 如果传了 branch 以下节点的 inode, 也会被处理成 branch 节点的 inode
	Inode string `json:"inode"`

	// 如果不传 inode, 就必须传 appID 和 branch
	AppID  string `json:"appID"`
	Branch string `json:"branch"`
}
