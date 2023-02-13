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

package auth

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"path"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/jinzhu/gorm"
	"github.com/patrickmn/go-cache"
	"github.com/sirupsen/logrus"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/conf"
	"github.com/erda-project/erda/internal/tools/gittar/models"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/internal/tools/gittar/uc"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
	"github.com/erda-project/erda/pkg/ucauth"
)

var (
	authCache = cache.New(3*time.Minute, 1*time.Minute)
)

var (
	NO_AUTH_ERROR = errors.New("no auth info")
)

func renderTemplate(template string, params map[string]string) string {
	paramPattern, _ := regexp.Compile("{{.+?}}")
	result := paramPattern.ReplaceAllStringFunc(template, func(s string) string {
		key := s[2:(len(s) - 2)]
		value, ok := params[key]
		if ok {
			return value
		} else {
			return s
		}
	})
	return result
}

func Authenticate(c *webcontext.Context) {
	// Repository content
	orgProject := c.Param("org")
	appName := strings.TrimSuffix(c.Param("repo"), ".git")

	repoPath := filepath.Join(orgProject, appName)

	echoReqPath := c.EchoContext.Path()
	repo, err := c.Service.GetRepoByPath(repoPath)
	if err != nil {
		c.AbortWithStatus(http.StatusNotFound, errors.New("repo not found"))
		return
	}

	//没有子path尝试重定向到UI
	if c.EchoContext.Request().Method == "GET" && (echoReqPath == "/:org/:repo" || echoReqPath == "/:org/:repo/*") && c.EchoContext.QueryString() == "" {
		params := map[string]string{
			"projectId": strconv.FormatInt(repo.ProjectID, 10),
			"appId":     strconv.FormatInt(repo.AppID, 10),
			"orgId":     strconv.FormatInt(repo.OrgID, 10),
		}
		orgDto, err := c.Bundle.GetOrg(repo.OrgID)
		if err != nil {
			c.AbortWithStatus(http.StatusNotFound, errors.New("org not found"))
			return
		}
		redirectUrlPrefix := "http://" + orgDto.Domain
		c.EchoContext.Redirect(301, redirectUrlPrefix+renderTemplate(conf.RepoPathTemplate(), params))
		return
	}

	doAuth(c, repo, repoPath)
}

func AuthenticateByApp(c *webcontext.Context) {
	appIDStr := c.Param("appId")
	appID, err := strconv.ParseInt(appIDStr, 10, 64)
	if err != nil {
		c.AbortWithStatus(http.StatusNotFound, errors.New("invalid appId"))
		return
	}
	repo, err := c.Service.GetRepoByApp(appID)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			c.AbortWithStatus(http.StatusNotFound, errors.New("repo not found"))
			return
		}
		c.EchoContext.String(500, err.Error())
		return
	}

	repoName := path.Join(repo.OrgName, repo.ProjectName, repo.AppName)
	doAuth(c, repo, repoName)
}

func AuthenticateV2(c *webcontext.Context) {
	host := c.EchoContext.Request().Host
	// 优先域名 第二优先读取ORG-ID头
	dto, err := c.Bundle.GetDopOrgByDomain(host, "x")
	var orgID int64
	if err == nil {
		orgID = int64(dto.ID)
	} else {
		orgIdStr := c.HttpRequest().Header.Get("Org-ID")
		orgID, err = strconv.ParseInt(orgIdStr, 10, 64)
		if err != nil {
			c.EchoContext.String(404, "org not found")
			return
		}
	}
	project := c.Param("project")
	app := strings.TrimSuffix(c.Param("app"), ".git")
	repo, err := c.Service.GetRepoByNames(orgID, project, app)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			c.AbortWithStatus(http.StatusNotFound, errors.New("repo not found"))
			return
		}
		c.EchoContext.String(500, err.Error())
		return
	}

	//todo delete redirect
	echoReqPath := c.EchoContext.Path()
	//没有子path尝试重定向到UI
	if c.EchoContext.Request().Method == "GET" &&
		(echoReqPath == "/wb/:project/:app" || echoReqPath == "/wb/:project/:app/*") && c.EchoContext.QueryString() == "" {
		params := map[string]string{
			"projectId": strconv.FormatInt(repo.ProjectID, 10),
			"appId":     strconv.FormatInt(repo.AppID, 10),
			"orgId":     strconv.FormatInt(repo.OrgID, 10),
		}
		redirectUrlPrefix := c.EchoContext.Scheme() + "://" + c.Host()
		c.EchoContext.Redirect(301, redirectUrlPrefix+renderTemplate(conf.RepoPathTemplate(), params))
		return
	}

	repoName := path.Join(strconv.FormatInt(orgID, 10), project, app)
	doAuth(c, repo, repoName)
}

func AuthenticateV3(c *webcontext.Context) {
	org := c.Param("org")
	project := c.Param("project")
	app := strings.TrimSuffix(c.Param("app"), ".git")

	orgID, err := getOrgIDV3(c, org)
	if err != nil {
		c.EchoContext.String(404, "org not found")
		return
	}
	repo, err := c.Service.GetRepoByNames(orgID, project, app)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			c.AbortWithStatus(http.StatusNotFound, errors.New("repo not found"))
			return
		}
		c.EchoContext.String(500, err.Error())
		return
	}

	repoName := path.Join(strconv.FormatInt(orgID, 10), project, app)
	doAuth(c, repo, repoName)
}

func doAuth(c *webcontext.Context, repo *models.Repo, repoName string) {
	// Git Protocol version v2
	version := c.GetHeader("Git-Protocol")
	c.Set("gitProtocol", version)

	var gitRepository = &gitmodule.Repository{}
	var err error
	var userInfo *ucauth.UserInfo

	userIdStr := c.GetHeader(httputil.UserHeader)
	if userIdStr != "" {
		logrus.Infof("userIdStr: %s", userIdStr)
		gitRepository, err = openRepository(c, repo)
		if err != nil {
			c.AbortWithStatus(500, err)
			return
		}
		c.Set("repository", gitRepository)
		userInfoDto, err := uc.FindUserById(userIdStr)
		if err != nil {
			c.AbortWithStatus(500, err)
			return
		}
		logrus.Infof("repo: %s userId: %v, username: %s", repoName, userIdStr, userInfoDto.Username)

		// if success, caches the results for 5 minutes
		_, validateError := ValidaUserRepoWithCache(c, userIdStr, repo)
		if validateError != nil {
			logrus.Infof("openapi auth fail repo:%s user:%s", repoName, userInfoDto.Username)
			c.AbortWithStatus(403, validateError)
			return
		}

		c.Set("user", &models.User{
			Name:     userInfoDto.Username,
			NickName: userInfoDto.NickName,
			Email:    userInfoDto.Email,
			Id:       userIdStr,
		})
		c.Next()
		return
	}
	logrus.Infof("basic auth,url: %s", c.HttpRequest().URL.String())
	userInfo, err = GetUserInfoByTokenOrBasicAuth(c, repo.ProjectID)
	if err == nil {
		_, validateError := ValidaUserRepo(c, string(userInfo.ID), repo)
		if validateError != nil {
			c.AbortWithString(403, validateError.Error()+" 403")
		} else {
			gitRepository, err = openRepository(c, repo)
			if err != nil {
				c.AbortWithStatus(500, err)
				return
			}
			c.Set("repository", gitRepository)
			c.Set("user", &models.User{
				Name:     userInfo.UserName,
				NickName: userInfo.NickName,
				Email:    userInfo.Email,
				Id:       string(userInfo.ID)})
			c.Next()
		}
	} else {
		c.Header("WWW-Authenticate", "Basic realm=Restricted")
		if err == NO_AUTH_ERROR {
			c.AbortWithStatus(401)
		} else {
			c.AbortWithString(401, err.Error()+" 401")
		}
	}
}

type AuthResp struct {
	Permission *apistructs.ScopeRole
	Repo       *models.Repo
}

type ErrorData struct {
	Code string `json:"code"`
	Msg  string `json:"msg"`
}

func GetUserInfoByTokenOrBasicAuth(c *webcontext.Context, projectID int64) (*ucauth.UserInfo, error) {
	var userInfo = &ucauth.UserInfo{}
	var err error

	org := c.Param("org")
	repo := strings.TrimSuffix(c.Param("repo"), ".git")
	repoName := filepath.Join(org, repo)

	username, password, ok := c.BasicAuth()
	if ok && username != "" && password != "" {
		// 触发token校验
		if username == conf.GitTokenUserName() {
			res, err := c.TokenService.QueryTokens(context.Background(), &tokenpb.QueryTokensRequest{
				Access: password,
				Type:   mysqltokenstore.PAT.String(),
			})
			if err == nil && res.Total > 0 {
				userID := res.Data[0].CreatorId
				userInfo, err := uc.FindUserById(userID)
				if err == nil {
					return &ucauth.UserInfo{
						ID:        ucauth.USERID(userID),
						Email:     userInfo.Email,
						Phone:     userInfo.Phone,
						AvatarUrl: userInfo.AvatarURL,
						UserName:  userInfo.Username,
						NickName:  userInfo.NickName,
					}, nil
				}
			} else {
				logrus.Errorf("get PAT err:%s", err)
				return nil, errors.New("invalid token")
			}
		} else {
			userInfo, err = GetUserByBasicAuth(c, username, password)
			if err != nil {
				logrus.Debugf("basic auth failed repo:%s login_name:%s err:%v", repoName, username, err)
			} else {
				logrus.Debugf("basic auth success repo:%s login_name:%s user_name:%s", repoName, username, userInfo.UserName)
			}
		}
		return userInfo, err
	} else {
		logrus.Debugf("no auth info repo:%s", repoName)
		return nil, NO_AUTH_ERROR
	}

}
func GetUserByBasicAuth(c *webcontext.Context, username string, passwd string) (*ucauth.UserInfo, error) {
	token, err := c.UCAuth.PwdAuth(username, passwd)
	if err != nil {
		return nil, err
	}
	logrus.Debugf("login success username: %s", username)
	userInfo, err := c.UCAuth.GetUserInfo(token)
	if err != nil {
		return nil, err
	}
	return &userInfo, nil
}

func ValidaUserRepoWithCache(c *webcontext.Context, userId string, repo *models.Repo) (*AuthResp, error) {
	key := userId + "-" + repo.Path
	authResultCache, found := authCache.Get(key)
	if found {
		autuResp := authResultCache.(*AuthResp)
		// 只有缓存存在并且是有权限的才使用缓存
		if autuResp != nil && autuResp.Permission.Access {
			return authResultCache.(*AuthResp), nil
		}
	}
	result, err := ValidaUserRepo(c, userId, repo)
	if err != nil {
		return nil, err
	}
	authCache.Set(key, result, cache.DefaultExpiration)
	return result, err
}

func ValidaUserRepo(c *webcontext.Context, userId string, repo *models.Repo) (*AuthResp, error) {
	permission, err := c.Bundle.ScopeRoleAccess(userId, &apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: apistructs.AppScope,
			ID:   strconv.FormatInt(repo.AppID, 10),
		},
	})
	if err != nil {
		return nil, err
	}
	if !permission.Access {
		return nil, errors.New("no permission to access")
	}
	return &AuthResp{
		Repo:       repo,
		Permission: permission,
	}, nil
}

func openRepository(ctx *webcontext.Context, repo *models.Repo) (*gitmodule.Repository, error) {
	gitRepository, err := gitmodule.OpenRepositoryWithInit(conf.RepoRoot(), repo.Path)
	if err != nil {
		return nil, err
	}
	gitRepository.ID = repo.ID
	gitRepository.ProjectId = repo.ProjectID
	gitRepository.ProjectName = repo.ProjectName
	gitRepository.ApplicationId = repo.AppID
	gitRepository.ApplicationName = repo.AppName
	gitRepository.OrgId = repo.OrgID
	gitRepository.OrgName = repo.OrgName
	gitRepository.Size = repo.Size
	gitRepository.Url = conf.GittarUrl() + "/" + repo.Path
	gitRepository.IsExternal = repo.IsExternal
	if repo.IsExternal {
		// check the key is exist or not
		key := fmt.Sprintf("/gittar/repo/%d", repo.ID)
		resp, err := ctx.EtcdClient.Get(context.Background(), key)
		if err != nil {
			return nil, err
		}
		// if key exist,and the request url's suffix is "/commits" will return err
		// else return without SyncExternalRepository
		if len(resp.Kvs) > 0 {
			if strings.HasSuffix(ctx.EchoContext.Request().URL.String(), "/commits") {
				return nil, errors.New("the repo is locked, please wait for a moment")
			}
			return gitRepository, nil
		}

		// minimum lease TTL is 5-second
		grantResp, err := ctx.EtcdClient.Grant(context.Background(), 5)
		if err != nil {
			return nil, err
		}

		// put key with lease
		_, err = ctx.EtcdClient.Put(context.Background(), key, "lock", clientv3.WithLease(grantResp.ID))
		if err != nil {
			return nil, err
		}

		// keep alive
		_, err = ctx.EtcdClient.KeepAlive(context.Background(), grantResp.ID)
		if err != nil {
			return nil, err
		}

		defer func() {
			_, err = ctx.EtcdClient.Revoke(context.Background(), grantResp.ID)
			if err != nil {
				logrus.Errorf("failed to revoke etcd, err: %v ", err)
			}
		}()

		err = gitmodule.SyncExternalRepository(path.Join(conf.RepoRoot(), repo.Path))
		if err != nil {
			return nil, err
		}
	}

	return gitRepository, nil
}

// getOrgIDV3 get orgID v3
func getOrgIDV3(c *webcontext.Context, orgName string) (int64, error) {
	orgDto, err := c.Bundle.GetOrg(orgName)
	if err == nil {
		return int64(orgDto.ID), nil
	}
	orgIdStr := c.HttpRequest().Header.Get("Org-ID")
	orgID, err := strconv.ParseInt(orgIdStr, 10, 64)
	if err != nil {
		return 0, err
	}
	return orgID, nil
}
