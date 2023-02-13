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

package models

import (
	"net/http"
	"time"

	"github.com/parnurzeal/gorequest"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
)

const (
	HOOK_EVENT_PUSH string = "push_events"
)

const (
	HOOK_TYPE_PROJECT string = "project"
	HOOK_TYPE_SYSTEM  string = "system"
)

type WebHook struct {
	BaseModel
	HookType   string `json:"hook_type" gorm:"size:150;index:idx_hook_type"`
	Name       string `json:"name" gorm:"size:150;index:idx_hook_name"`
	RepoID     int64  `json:"repo" gorm:"size:150;index:idx_repo_id"`
	Token      string `json:"token"`
	Url        string `json:"url"`
	IsActive   bool   `json:"is_active"`
	PushEvents bool   `json:"push_events"`
}

// WebHookTask represents a hook task.
// todo 请求头 响应头
type WebHookTask struct {
	BaseModel
	HookId          int64 `gorm:"index:idx_hook_id"`
	Url             string
	Event           string
	IsDelivered     bool
	IsSucceed       bool
	RequestContent  string `gorm:"type:text"`
	ResponseContent string `gorm:"type:text"`
	ResponseStatus  string
}

var HookTaskQueue = make(chan *WebHookTask, 300)

func (svc *Service) GetProjectHooks(repository *gitmodule.Repository) ([]WebHook, error) {
	var hooks []WebHook
	err := svc.db.Where("hook_type=? AND repo_id = ?",
		HOOK_TYPE_PROJECT,
		repository.ID).Find(&hooks).Error

	if err != nil {
		return nil, err
	}
	return hooks, nil
}

func (svc *Service) GetProjectHooksByEvent(repository *gitmodule.Repository, event string, active bool) ([]WebHook, error) {
	var hooks []WebHook
	err := svc.db.Where("hook_type=? AND repo_id = ? AND "+event+"=1 AND is_active=?",
		HOOK_TYPE_PROJECT,
		repository.ID, active).Find(&hooks).Error

	if err != nil {
		return nil, err
	}
	return hooks, nil
}

func (svc *Service) AddProjectHook(hook *WebHook) (*WebHook, error) {
	hook.HookType = HOOK_TYPE_PROJECT

	//命名hook,检测同名,存在只做更新
	if hook.Name != "" {
		var currentHook WebHook
		if svc.db.First(&currentHook, "hook_type=? AND name = ? AND repo_id = ?",
			HOOK_TYPE_PROJECT, hook.Name, hook.RepoID).RecordNotFound() {
			err := svc.db.Create(hook).Error
			return hook, err
		} else {
			currentHook.Url = hook.Url
			currentHook.PushEvents = hook.PushEvents
			currentHook.IsActive = hook.IsActive
			err := svc.db.Save(&currentHook).Error
			return &currentHook, err
		}
	} else {
		//匿名hook直接添加
		err := svc.db.Create(hook).Error
		return hook, err
	}
}

func (svc *Service) RemoveProjectHooks(repository *Repo) error {
	svc.db.Where("hook_type=? AND repo_id = ?",
		HOOK_TYPE_PROJECT,
		repository.ID).Delete(&WebHook{})
	return nil
}

func (svc *Service) CreateHookTask(task *WebHookTask) error {
	return svc.db.Create(task).Error

}

func Init(db *DBClient) {
	for i := 0; i < 5; i += 1 {
		go StartHookTaskConsumer(db)
	}
}

func AddToHookTaskQueue(task *WebHookTask) {
	HookTaskQueue <- task
}

func StartHookTaskConsumer(db *DBClient) {
	for {
		hookTask := <-HookTaskQueue
		func() {
			logrus.Info("hookTask start:" + hookTask.RequestContent)
			request := gorequest.New()
			resp, body, errors := request.Post(hookTask.Url).
				Set("X-GITTAR-EVENT", hookTask.Event).
				Retry(2, 5*time.Second, http.StatusBadRequest, http.StatusInternalServerError).
				Send(hookTask.RequestContent).
				End()

			hookTask.IsSucceed = false
			if errors != nil {
				logrus.Errorf("hookTask callback error %s %v", hookTask.Url, errors)
			} else {
				status := resp.StatusCode
				hookTask.ResponseContent = body
				hookTask.ResponseStatus = resp.Status
				if status == 200 {
					hookTask.IsSucceed = true
					logrus.Infof("hookTask callback success %s %v", hookTask.Url, body)
				} else {
					logrus.Errorf("hookTask callback error %s %v", hookTask.Url, body)
				}
			}

			hookTask.IsDelivered = true
			db.Save(hookTask)
		}()
	}
}
