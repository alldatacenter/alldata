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

//go:build !codeanalysis
// +build !codeanalysis

package helper

import (
	"encoding/json"
	"strconv"
	"strings"

	git "github.com/libgit2/git2go/v30"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/event"
	"github.com/erda-project/erda/internal/tools/gittar/models"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
)

// protect branch
func preReceiveHook(pushEvents []*models.PayloadPushEvent, c *webcontext.Context) bool {
	for _, pushEvent := range pushEvents {
		var err error
		if pushEvent.IsTag {
			//tag校验
			if pushEvent.IsDelete {
				err = c.CheckPermission(models.PermissionDeleteTAG)
			} else {
				err = c.CheckPermission(models.PermissionCreateTAG)
			}
		} else {
			//分支校验
			if pushEvent.IsDelete {
				err = c.CheckPermission(models.PermissionDeleteBranch)
			} else {
				err = c.CheckPermission(models.PermissionPush)
			}
		}
		if err != nil {
			c.Status(200)
			c.GetWriter().Write(NewReportStatus(
				"unpack ok",
				"ng "+pushEvent.Ref,
				err.Error()))
			return false
		}

		if !pushEvent.IsTag {
			branch := strings.TrimPrefix(pushEvent.Ref, gitmodule.BRANCH_PREFIX)
			//保护分支权限
			if c.Repository.IsProtectBranch(branch) {
				//是否可以推送保护分支
				if err := c.CheckPermission(models.PermissionPushProtectBranch); err != nil {
					c.Status(200)
					c.GetWriter().Write(NewReportStatus(
						"unpack ok",
						"ng "+pushEvent.Ref,
						err.Error()))
					return false
				}

				//是否可以覆盖推送保护分支
				isForcePush := false
				//有可能是未push上来的新commit,不做异常判断，只根据是否为空判断
				beforeCommit, _ := c.Repository.GetCommit(pushEvent.Before)
				lastCommit, _ := c.Repository.GetCommit(pushEvent.After)
				if beforeCommit != nil && lastCommit != nil {
					baseCommit, err := c.Repository.GetMergeBase(beforeCommit, lastCommit)
					if err == nil {
						if baseCommit != nil && baseCommit.ID == lastCommit.ID {
							isForcePush = true
						}
					}
				} else {
					//TODO 有一些情况遗漏
					//全新推送
					if pushEvent.Before == gitmodule.INIT_COMMIT_ID {
						_, err := c.Repository.GetBranchCommit(branch)
						//如果master已经有commit，判断全新覆盖force push
						if err == nil {
							isForcePush = true
						}
					}
				}

				if isForcePush {
					if err := c.CheckPermission(models.PermissionPushProtectBranchForce); err != nil {
						c.Status(200)
						c.GetWriter().Write(NewReportStatus(
							"unpack ok",
							"ng "+pushEvent.Ref,
							err.Error()))
						return false
					}
				}
			}
		}

	}
	return true
}

// trigger event
func PostReceiveHook(pushEvents []*models.PayloadPushEvent, c *webcontext.Context) {
	pusher := c.User
	repository := c.Repository

	size, err := repository.CalcRepoSize()
	if err == nil {
		c.Service.UpdateRepoSizeCache(repository.ID, size)
	}

	repo, err := git.OpenRepository(repository.DiskPath())
	if err != nil {
		logrus.Errorf("repo error %s %s", repository.DiskPath(), err)
	}
	logrus.Debugf("[Pusher] Name: %s Email: %s", pusher.Name, pusher.Email)

	repoFullName := repository.Path
	for _, pushEvent := range pushEvents {

		//删除暂时有问题,先不触发
		if pushEvent.IsDelete {
			continue
		}
		walker, _ := repo.Walk()
		walker.PushRef(pushEvent.Ref)
		beforeCommit, _ := git.NewOid(pushEvent.Before)
		pushEvent.Commits = []models.PayloadCommit{}

		statusCountToAdd := 20
		walker.Iterate(func(commit *git.Commit) bool {
			if commit.Id().Equal(beforeCommit) {
				return false
			}
			statusCountToAdd--
			pushEvent.TotalCommitsCount++
			if statusCountToAdd >= 0 {
				commitData := models.PayloadCommit{}
				commitData.Author = &models.User{
					Name:  commit.Author().Name,
					Email: commit.Author().Email,
				}
				commitData.Id = commit.Id().String()
				commitData.Message = commit.Message()
				commitData.Timestamp = commit.Author().When
				pushEvent.Commits = append(pushEvent.Commits, commitData)
			}

			return true
		})

		pushEvent.Repository = repository
		logrus.Infof("%v", pushEvent)

		//trigger eventbox event
		err := c.Bundle.CreateEvent(&apistructs.EventCreateRequest{
			EventHeader: apistructs.EventHeader{
				ApplicationID: strconv.FormatInt(repository.ApplicationId, 10),
				ProjectID:     strconv.FormatInt(repository.ProjectId, 10),
				OrgID:         strconv.FormatInt(repository.OrgId, 10),
				Event:         event.GitPushEvent,
				Action:        event.GitPushEvent,
				UserID:        pushEvent.Pusher.Id,
			},
			Sender:  "gittar",
			Content: pushEvent,
		})
		if err != nil {
			logrus.Errorf("failed to create eventbox message: %v", err)
		}

		//project system hook
		projectHooks, err := c.Service.GetProjectHooksByEvent(repository, models.HOOK_EVENT_PUSH, true)
		if err != nil {
			logrus.Error("error get project hooks")
			continue
		}

		systemHooks, err := c.Service.GetSystemHooksByEvent(models.HOOK_EVENT_PUSH, true)
		if err != nil {
			logrus.Error("error get system hooks")
			continue
		}

		hooks := append(projectHooks, systemHooks...)

		flag := true
		if len(hooks) == 0 {
			logrus.Infof("%v pushEvent", repoFullName)
			logrus.Infof("%v no hooks skip", repoFullName)
			flag = false
		} else {
			jsonData, _ := json.Marshal(pushEvent)
			for _, hook := range hooks {

				task := &models.WebHookTask{
					HookId:         hook.ID,
					RequestContent: string(jsonData),
					Url:            hook.Url,
					Event:          models.HOOK_EVENT_PUSH,
				}
				err := c.Service.CreateHookTask(task)
				if err != nil {
					logrus.Errorf("create hookTask error %v %v", err, task)
					continue
				}
				models.AddToHookTaskQueue(task)
			}
		}

		//更新mr表
		if !pushEvent.IsTag {
			branch := strings.TrimPrefix(pushEvent.Ref, gitmodule.BRANCH_PREFIX)
			err := c.Service.SyncMergeRequest(repository, branch, pushEvent.After, pusher.Id, flag)
			if err != nil {
				logrus.Errorf("error sync merge request repo:%s ref:%s err:%s",
					repository.Path, pushEvent.Ref, err)
			}
		}
	}
}
