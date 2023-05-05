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

package api

import (
	"errors"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/gittar/models"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
)

type HookRequest struct {
	Id         int64  `json:"id"`
	Url        string `json:"url"`
	Name       string `json:"name"`
	PushEvents bool   `json:"push_events"`
}

// GetRepoStats function
func GetHooks(context *webcontext.Context) {
	repository := context.Repository

	hooks, err := context.Service.GetProjectHooks(repository)
	if err != nil {
		context.Abort(ERROR_DB)
		return
	}
	context.Success(hooks)
}

func GetHookDetail(context *webcontext.Context) {
	repository := context.Repository
	id, err := strconv.ParseInt(context.Param("id"), 10, 32)
	if err != nil {
		context.AbortWithStatus(400, ERROR_ARG_ID)
		return
	}
	hook := models.WebHook{}
	if context.DBClient.First(&hook, "id= ? AND repo_id = ?",
		id, repository.ID).RecordNotFound() {
		context.AbortWithStatus(404, ERROR_HOOK_NOT_FOUND)
		return
	}

	context.Success(hook)
}

func AddHook(context *webcontext.Context) {
	repository := context.Repository
	request := HookRequest{}
	err := context.BindJSON(&request)
	if err != nil {
		context.AbortWithStatus(400, errors.New("request body parse failed"))
		return
	}

	hook := models.WebHook{
		Url:        request.Url,
		RepoID:     repository.ID,
		Name:       request.Name,
		IsActive:   true,
		PushEvents: request.PushEvents,
	}

	currentHook, err := context.Service.AddProjectHook(&hook)
	if err != nil {
		context.AbortWithStatus(500, errors.New("db error"))
		logrus.Errorf("create project hook error %v", err)
	}

	context.Success(currentHook)
}

func UpdateHook(context *webcontext.Context) {
	repository := context.Repository
	id, err := strconv.ParseInt(context.Param("id"), 10, 32)
	if err != nil {
		context.AbortWithStatus(400, errors.New("id parse failed"))
		return
	}

	request := HookRequest{}
	err = context.BindJSON(&request)
	if err != nil {
		context.AbortWithStatus(400, errors.New("request body parse failed"))
		return
	}

	hook := models.WebHook{}

	if context.DBClient.First(&hook, "id= ? AND repo_id = ?",
		id, repository.ID).RecordNotFound() {
		context.AbortWithStatus(404, errors.New("hook not found"))
		return
	}

	hook.PushEvents = request.PushEvents
	hook.Url = request.Url

	context.DBClient.Save(&hook)
	context.Success(hook)

}

func DeleteHook(context *webcontext.Context) {
	repository := context.Repository
	id, err := strconv.ParseInt(context.Param("id"), 10, 32)
	if err != nil {
		context.AbortWithStatus(400, errors.New("id parse failed"))
		return
	}
	hook := models.WebHook{}

	if context.DBClient.First(&hook, "id= ? AND repo_id = ?",
		id, repository.ID).RecordNotFound() {
		context.AbortWithStatus(404, errors.New("hook not found"))
		return
	}

	context.DBClient.Delete(hook)
	context.Success(nil)
}

func RequeueTask(context *webcontext.Context) {
	hookTask := models.WebHookTask{}
	id, err := strconv.ParseInt(context.Param("id"), 10, 32)
	if err != nil {
		context.AbortWithStatus(400)
		return
	}
	if context.DBClient.First(&hookTask, "id= ? ", id).RecordNotFound() {
		context.AbortWithStatus(404, errors.New("task not found"))
		return
	}
	models.AddToHookTaskQueue(&hookTask)
	context.Success(nil)
}

func AddSystemHook(context *webcontext.Context) {
	request := HookRequest{}
	err := context.BindJSON(&request)
	if err != nil {
		context.AbortWithStatus(400, errors.New("request body parse failed"))
		return
	}

	hook := models.WebHook{
		Url:        request.Url,
		Name:       request.Name,
		IsActive:   true,
		PushEvents: request.PushEvents,
	}
	result, err := context.Service.AddSystemHook(&hook)
	if err != nil {
		context.AbortWithStatus(500, errors.New("db error"))
		logrus.Errorf("create system hook error %v", err)
		return
	}
	context.Success(result)
}
