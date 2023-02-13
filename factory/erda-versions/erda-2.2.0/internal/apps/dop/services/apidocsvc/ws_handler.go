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
	"io"
	"path/filepath"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/uc"
	"github.com/erda-project/erda/internal/apps/dop/services/websocket"
)

const (
	lockDuration = time.Minute
)

const (
	heartBeatRequest  = "heart_beat_request"
	heartBeatResponse = "heart_beat_response"
	autoSaveRequest   = "auto_save_request"
	autoSaveResponse  = "auto_save_response"
	commitRequest     = "commit_request"
	commitResponse    = "commit_response"
)

// 消息处理函数, 处理具体的 request *WebsocketRequest
// 如果返回 err != nil, 服务端则立即断开连接, 如果不是致命错误, 不要返回.
type handler func(tx *dbclient.TX, writer websocket.ResponseWriter, request *apistructs.WebsocketRequest) error

type APIDocWSHandler struct {
	orgID        uint64
	userID       string
	appID        uint64
	branch       string
	filename     string // base filename
	sessionID    string
	hasLock      bool   // true: 持有锁, false: 不持有锁
	lockUserID   string // 持有锁的用户 ID
	lockUserNick string // 持有锁的用户昵称
	req          *apistructs.WsAPIDocHandShakeReq
	svc          *Service
	ft           *bundle.GittarFileTree
	trans        i18n.Translator
	codes        i18n.LanguageCodes
}

// 每个消息的通用处理过程: 确认是否持有锁; 如未持有锁则尝试抢占; 锁定文档锁所在的行; 更新过期时效
func (h *APIDocWSHandler) wrap(handler handler) websocket.Handler {
	return func(w websocket.ResponseWriter, r *apistructs.WebsocketRequest) error {
		if r != nil && r.SessionID != h.sessionID {
			return apierrors.WsUpgrade.InvalidParameter("invalid sessionID")
		}

		tx := dbclient.Tx()
		defer tx.RollbackUnlessCommitted()
		tx.Exec("set innodb_lock_wait_timeout=5")

		var (
			lock    apistructs.APIDocLockModel
			timeNow = time.Now()
			where   = map[string]interface{}{
				"application_id": h.ft.ApplicationID(),
				"branch_name":    h.ft.BranchName(),
				"doc_name":       filepath.Base(h.ft.TreePath()),
			}
		)

		// 检查锁的状态, 如未持有锁则尝试抢占
		switch record := tx.Where(where).First(&lock); {
		case record.RowsAffected == 0:
			// 不存在文档锁记录, 则抢占
			create := tx.Create(&apistructs.APIDocLockModel{
				ID:            0,
				CreatedAt:     timeNow,
				UpdatedAt:     timeNow,
				SessionID:     h.sessionID,
				IsLocked:      true,
				ExpiredAt:     timeNow.Add(lockDuration),
				ApplicationID: h.appID,
				BranchName:    h.branch,
				DocName:       h.filename,
				CreatorID:     h.req.Identity.UserID,
				UpdaterID:     h.req.Identity.UserID,
			})

			// 如果抢占失败, 则向客户端发送错误信息和文档锁信息, 提前结束业务
			if create.Error != nil {
				h.hasLock = false
				h.responseError(w, create.Error)
				h.responseDocLockStatus(w)
				return nil
			}
		case lock.SessionID == h.sessionID:
			// 该用户是该文档锁的持有人

		default:
			// 尝试抢占锁
			updates := tx.Model(&lock).Where(where).Where("is_locked = false or expired_at < ?", timeNow).
				Updates(map[string]interface{}{
					"session_id": h.sessionID,
					"is_locked":  true,
					"expired_at": timeNow.Add(lockDuration),
					"updated_at": timeNow,
					"updater_id": h.req.Identity.UserID,
				})
			// 如果抢占失败, 则向客户端发送错误信息和文档锁状态, 提前结束业务
			if updates.Error != nil || updates.RowsAffected == 0 {
				logrus.Errorf("failed to Updates, err: %v, RowsAffected: %v", updates.Error, updates.RowsAffected)
				h.hasLock = false
				h.setLockedUser(lock.CreatorID)
				h.responseError(w, errors.New(h.text("FailedToGetDocLock")))
				h.responseDocLockStatus(w)
				return nil
			}
		}

		h.hasLock = true

		// 行锁
		tx.Model(&lock).Where(where).Updates(map[string]interface{}{"session_id": h.sessionID})

		// 执行具体业务
		switch err := handler(tx, w, r); err.(type) {
		case nil:
		case websocket.ExitWithDoingNothing, *websocket.ExitWithDoingNothing:
			tx.Commit()
			return err
		default:
			return err
		}

		// 更新锁过期时间
		tx.Model(&lock).Where(where).Updates(map[string]interface{}{"expired_at": time.Now().Add(lockDuration)})

		tx.Commit()

		return nil
	}
}

// 建立连接时的处理方法
// 建立连接时立即下发 sessionID 和 lock 状态
func (h *APIDocWSHandler) AfterConnected(w websocket.ResponseWriter) {
	_ = h.wrap(func(_ *dbclient.TX, writer websocket.ResponseWriter, _ *apistructs.WebsocketRequest) error {
		h.responseDocLockStatus(writer)
		return nil
	})(w, nil)
}

// 连接关闭前的处理方法
// 暂存的文档提交到 gittar, 释放 session 持有的文档锁, 将错误信息透给客户端
func (h *APIDocWSHandler) BeforeClose(w websocket.ResponseWriter, err error) {
	h.responseError(w, err)

	_ = h.wrap(h.beforeClose)(w, nil)
}

func (h *APIDocWSHandler) beforeClose(tx *dbclient.TX, writer websocket.ResponseWriter, _ *apistructs.WebsocketRequest) error {
	if !h.hasLock {
		return nil
	}

	// 暂存的文档提交到 gittar
	if err := h.commitTmpToGittar(h.text("AutoCommitDoc")); err != nil {
		h.responseError(writer, err)
	}

	// 释放持有的文档锁
	h.releaseLock(tx)
	return nil
}

// 释放持有的文档锁
func (h *APIDocWSHandler) releaseLock(tx *dbclient.TX) {
	var where = map[string]interface{}{
		"application_id": h.appID,
		"branch_name":    h.branch,
		"doc_name":       h.filename,
	}
	tx.Delete(new(apistructs.APIDocLockModel), where)
	tx.Delete(new(apistructs.APIDocTmpContentModel), where)
}

func (h *APIDocWSHandler) handleHeartBeat(_ *dbclient.TX, w websocket.ResponseWriter, _ *apistructs.WebsocketRequest) error {
	h.responseDocLockStatus(w)
	return nil
}

// 暂存文档
func (h *APIDocWSHandler) handleAutoSave(tx *dbclient.TX, w websocket.ResponseWriter, r *apistructs.WebsocketRequest) error {
	var data apistructs.WsAPIDocAutoSaveReqData
	if err := json.Unmarshal(r.Data, &data); err != nil {
		h.responseError(w, err)
		return nil
	}
	if data.Inode != h.ft.Inode() {
		h.responseError(w, errors.New("inode error"))
		return nil
	}

	var (
		tmpDoc apistructs.APIDocTmpContentModel
		where  = map[string]interface{}{
			"application_id": h.appID,
			"branch_name":    h.branch,
			"doc_name":       h.filename,
		}
		timeNow = time.Now()
	)

	switch record := tx.Where(where).First(&tmpDoc); {
	case record.RowsAffected == 0:
		// 如果没有暂存的文档记录, 则插入
		tmpDoc = apistructs.APIDocTmpContentModel{
			ID:            0,
			CreatedAt:     timeNow,
			UpdatedAt:     timeNow,
			ApplicationID: h.appID,
			BranchName:    h.branch,
			DocName:       h.filename,
			Content:       data.Content,
			CreatorID:     h.req.Identity.UserID,
			UpdaterID:     h.req.Identity.UserID,
		}
		if create := tx.Create(&tmpDoc); create.Error != nil {
			h.responseError(w, errors.Wrap(create.Error, h.text("FailedToSaveDoc")))
			return nil
		}

	case record.Error == nil:
		// 如果查找到了暂存的文档记录, 则更新
		updates := tx.Model(new(apistructs.APIDocTmpContentModel)).
			Where(where).Updates(map[string]interface{}{
			"updater_id": h.req.Identity.UserID,
			"updated_at": timeNow,
			"content":    data.Content,
		})
		if updates.Error != nil {
			h.responseError(w, errors.Wrap(updates.Error, h.text("FailedToUpdateDoc")))
			return nil
		}
	default:
		// 一般不会出现此分支的情况, 如果出现就抛出错误
		logrus.Errorf("failed to First tmpDoc, err: %v", record.Error)
		return record.Error
	}

	// response to client
	h.responseAutoSaveCommit(w, r.MessageID, autoSaveResponse)

	return nil
}

// 提交文档
func (h *APIDocWSHandler) handleCommit(tx *dbclient.TX, w websocket.ResponseWriter, r *apistructs.WebsocketRequest) error {
	var data apistructs.WsAPIDocAutoSaveReqData
	if err := json.Unmarshal(r.Data, &data); err != nil {
		h.responseError(w, err)
		return nil
	}

	repo := strings.TrimPrefix(h.ft.RepoPath(), "/")
	if err := CommitAPIDocModifies(h.orgID, h.userID, repo, h.text("UpdateDocFromDesignCenter"),
		h.filename, data.Content, h.ft.BranchName()); err != nil {
		h.responseError(w, err)
		return nil
	}

	h.responseAutoSaveCommit(w, r.MessageID, commitResponse)

	h.releaseLock(tx)

	return websocket.ExitWithDoingNothing{}
}

func (h *APIDocWSHandler) setLockedUser(userID string) {
	h.lockUserID = userID
	if users, _ := uc.GetUsers([]string{userID}); len(users) > 0 {
		if user := users[userID]; user != nil {
			h.lockUserNick = user.Nick
		}
	}
}

// 向客户端发送锁的状态
func (h *APIDocWSHandler) responseDocLockStatus(w websocket.ResponseWriter) {
	var (
		lock = apistructs.APIDocMetaLock{
			Locked:   !h.hasLock,
			UserID:   h.lockUserID,
			NickName: h.lockUserNick,
		}
		meta = apistructs.APIDocMeta{
			Lock: &lock,
			Tree: nil,
			Blob: nil,
		}
	)

	metaData, _ := json.Marshal(meta)

	var data = apistructs.FileTreeNodeRspData{
		Type:      "f",
		Inode:     h.req.URIParams.Inode,
		Pinode:    h.ft.Clone().DeletePathFromRepoRoot().Inode(),
		Scope:     "application",
		ScopeID:   h.ft.ApplicationID(),
		Name:      h.filename,
		CreatorID: "",
		UpdaterID: "",
		Meta:      json.RawMessage(metaData),
	}
	dataRaw, err := json.Marshal(data)
	if err != nil {
		logrus.Fatalf("json.Marshal, err: %v", err)
	}
	var response = apistructs.WebsocketRequest{
		SessionID: h.sessionID,
		MessageID: 0,
		Type:      heartBeatResponse,
		CreatedAt: time.Now(),
		Data:      dataRaw,
	}
	dataRaw, err = json.Marshal(response)
	if err != nil {
		logrus.Fatalf("json.Marshal, err: %v", err)
	}
	_, _ = w.Write(dataRaw)
}

func (h *APIDocWSHandler) responseAutoSaveCommit(w io.Writer, messageID uint64, type_ string) {
	var rspData = apistructs.FileTreeNodeRspData{
		Type:      "f",
		Inode:     h.ft.Inode(),
		Pinode:    h.ft.Clone().DeletePathFromRepoRoot().Inode(),
		Scope:     "application",
		ScopeID:   h.ft.ApplicationID(),
		Name:      h.filename,
		CreatorID: "",
		UpdaterID: "",
		Meta:      nil,
	}
	dataRaw, _ := json.Marshal(rspData)
	var response = apistructs.WebsocketRequest{
		SessionID: h.sessionID,
		MessageID: messageID,
		Type:      type_,
		CreatedAt: time.Now(),
		Data:      dataRaw,
	}
	dataRaw, _ = json.Marshal(response)
	_, _ = w.Write(dataRaw)
}

// 向客户端发送错误消息
func (h *APIDocWSHandler) responseError(w websocket.ResponseWriter, err error) {
	var data = map[string]string{"err": err.Error()}
	dataRaw, _ := json.Marshal(data)
	var response = apistructs.WebsocketRequest{
		SessionID: h.sessionID,
		MessageID: 0,
		Type:      "error_response",
		CreatedAt: time.Now(),
		Data:      dataRaw,
	}
	dataRaw, _ = json.Marshal(response)
	_, _ = w.Write(dataRaw)
}

// 将暂存的文档提交到 gittar
func (h *APIDocWSHandler) commitTmpToGittar(commitMessage string) error {
	if commitMessage == "" {
		commitMessage = "update API doc from API Design Center"
	}
	var (
		tmp   apistructs.APIDocTmpContentModel
		where = map[string]interface{}{
			"application_id": h.appID,
			"branch_name":    h.branch,
			"doc_name":       h.filename,
		}
	)
	record := dbclient.Sq().Where(where).First(&tmp)
	if record.Error != nil {
		return record.Error
	}

	repo := strings.TrimPrefix(h.ft.RepoPath(), "/")
	if err := CommitAPIDocModifies(h.orgID, h.userID, repo, commitMessage, h.filename, tmp.Content, h.ft.BranchName()); err != nil {
		return errors.Wrap(err, "failed to CommitAPIDocContent")
	}

	return nil
}

func (h APIDocWSHandler) text(key string, a ...interface{}) string {
	if len(a) == 0 {
		return h.trans.Text(h.codes, key)
	}
	return fmt.Sprintf(h.trans.Text(h.codes, key), a...)
}
