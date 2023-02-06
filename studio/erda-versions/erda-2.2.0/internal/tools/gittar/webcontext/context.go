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

package webcontext

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"strconv"

	"github.com/coreos/etcd/clientv3"
	"github.com/labstack/echo"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/gittar/models"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/errorx"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

type Context struct {
	EchoContext  echo.Context
	Repository   *gitmodule.Repository
	User         *models.User
	Service      *models.Service
	DBClient     *models.DBClient
	Bundle       *bundle.Bundle
	UCAuth       *ucauth.UCUserAuth
	next         bool
	EtcdClient   *clientv3.Client
	TokenService tokenpb.TokenServiceServer
}

type ContextHandlerFunc func(*Context)

var dbClientInstance *models.DBClient
var diceBundleInstance *bundle.Bundle
var ucAuthInstance *ucauth.UCUserAuth
var etcdClientInstance *clientv3.Client
var tokenServiceInstance *tokenpb.TokenServiceServer

func WithDB(db *models.DBClient) {
	dbClientInstance = db
}

func WithBundle(diceBundle *bundle.Bundle) {
	diceBundleInstance = diceBundle
}

func WithUCAuth(ucAuth *ucauth.UCUserAuth) {
	ucAuthInstance = ucAuth
}

func WithEtcdClient(client *clientv3.Client) {
	etcdClientInstance = client
}

func WithTokenService(tokenService *tokenpb.TokenServiceServer) {
	tokenServiceInstance = tokenService
}

func WrapHandler(handlerFunc ContextHandlerFunc) echo.HandlerFunc {
	return func(c echo.Context) error {
		ctx := NewEchoContext(c, dbClientInstance)
		handlerFunc(ctx)
		return nil
	}
}

func WrapHandlerWithRepoCheck(handlerFunc ContextHandlerFunc) echo.HandlerFunc {
	return func(c echo.Context) error {
		ctx := NewEchoContext(c, dbClientInstance)
		repoPath := ctx.Repository.DiskPath()
		if _, err := os.Stat(repoPath); os.IsNotExist(err) {
			ctx.AbortWithStatus(http.StatusNotFound, errors.New("repo not exist"))
			return nil
		} else {
			rawRepo, err := ctx.Repository.GetRawRepo()
			if err != nil {
				ctx.Abort(err)
				return nil
			}
			isEmpty, err := rawRepo.IsEmpty()
			if err != nil {
				ctx.Abort(err)
				return nil
			}
			if isEmpty {
				ctx.AbortWithStatus(404, errors.New("repo is empty"))
				return nil
			}
		}
		handlerFunc(ctx)
		return nil
	}
}

func WrapMiddlewareHandler(handlerFunc ContextHandlerFunc) echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c echo.Context) error {
			ctx := NewEchoContext(c, dbClientInstance)
			ctx.next = false
			handlerFunc(ctx)
			if ctx.next {
				next(c)
				return nil
			} else {
				return errors.New("abort")
			}
		}
	}
}

func NewEchoContext(c echo.Context, db *models.DBClient) *Context {
	repository := c.Get("repository")
	if repository == nil {
		repository = &gitmodule.Repository{}
	}
	repo := repository.(*gitmodule.Repository)
	repo.Bundle = diceBundleInstance
	var user *models.User
	userValue := c.Get("user")
	if userValue != nil {
		user = userValue.(*models.User)
	}
	return &Context{
		Repository:   repository.(*gitmodule.Repository),
		EchoContext:  c,
		User:         user,
		Service:      models.NewService(db, diceBundleInstance),
		DBClient:     db,
		UCAuth:       ucAuthInstance,
		Bundle:       diceBundleInstance,
		EtcdClient:   etcdClientInstance,
		TokenService: *tokenServiceInstance,
	}
}

func (c *Context) GetQueryInt32(key string, defaults ...int) int {
	value := c.EchoContext.QueryParam(key)
	if value == "" {
		return defaults[0]
	} else {
		i, err := strconv.ParseInt(value, 10, 32)
		if err != nil {
			return defaults[0]
		} else {
			return int(i)
		}
	}
}

func (c *Context) GetQueryBool(key string, defaults ...bool) bool {
	value := c.EchoContext.QueryParam(key)
	if value == "" {
		return defaults[0]
	} else {
		bvalue, err := strconv.ParseBool(value)
		if err != nil {
			return defaults[0]
		} else {
			return bvalue
		}
	}
}

func (c *Context) MustGet(key string) interface{} {
	return c.EchoContext.Get(key)
}

func (c *Context) Set(key string, value interface{}) {
	c.EchoContext.Set(key, value)
}
func (c *Context) Param(key string) string {
	return c.EchoContext.Param(key)
}

func (c *Context) ParamInt32(key string, defaults ...int) int {
	value := c.EchoContext.Param(key)
	if value == "" {
		return defaults[0]
	} else {
		i, err := strconv.ParseInt(value, 10, 32)
		if err != nil {
			return defaults[0]
		} else {
			return int(i)
		}
	}
}

func (c *Context) Query(key string) string {
	return c.EchoContext.QueryParam(key)
}

func (c *Context) BindJSON(obj interface{}) error {
	return c.EchoContext.Bind(obj)
}

func (c *Context) Success(data interface{}, userIDs ...[]string) {
	result := &ApiData{
		Success: true,
		Data:    data,
	}
	if len(userIDs) > 0 {
		result.UserIDs = strutil.DedupSlice(userIDs[0], true)
	}
	c.EchoContext.JSON(200, result)
}

func (c *Context) Header(key string, value string) {
	c.EchoContext.Response().Header().Set(key, value)
}

func (c *Context) File(path string) {
	c.EchoContext.File(path)
}
func (c *Context) GetHeader(key string) string {
	return c.EchoContext.Request().Header.Get(key)
}

func (c *Context) GetWriter() io.Writer {
	return c.EchoContext.Response()
}
func (c *Context) GetRequestBody() io.ReadCloser {
	return c.EchoContext.Request().Body
}

func (c *Context) AbortWithStatus(code int, err ...error) {
	errMsg := ""
	if len(err) > 0 {
		errMsg = err[0].Error()
	}
	result := &ApiData{
		Success: false,
		Err: apistructs.ErrorResponse{
			Code: strconv.FormatInt(int64(code), 10),
			Msg:  errMsg,
			Ctx:  "",
		},
	}

	c.EchoContext.JSON(code, result)
}

func (c *Context) Status(status int) {
	c.EchoContext.Response().Status = status
	c.EchoContext.Response().Flush()
}

func (c *Context) AbortWithData(code int, data interface{}) {
	c.EchoContext.JSON(code, data)
}

func (c *Context) AbortWithString(code int, msg string) {
	c.EchoContext.String(code, msg)
}

func (c *Context) Abort(err error) {
	logrus.Error(errorx.NewTracedError(err).Error())
	c.AbortWithStatus(500, err)
}

func (c *Context) Next() {
	c.next = true
}

func (c *Context) Host() string {
	return c.EchoContext.Request().Host
}

func (c *Context) HttpRequest() *http.Request {
	return c.EchoContext.Request()
}

func (c *Context) Data(statusCode int, contentType string, buf []byte) {
	c.EchoContext.Response().Status = statusCode
	c.EchoContext.Response().Header().Add("Content-Type", contentType)
	c.EchoContext.Response().Write(buf)
}

func (c *Context) BasicAuth() (username, password string, ok bool) {
	return c.EchoContext.Request().BasicAuth()
}

func (c *Context) CheckPermission(permission models.Permission) error {
	return c.Service.CheckPermission(c.Repository, c.User, permission, nil)
}
func (c *Context) CheckBranchOperatePermission(user *models.User, branch string) error {
	if user.IsInnerUser() {
		return nil
	}
	resource := apistructs.NormalBranchResource
	if c.Repository.IsProtectBranch(branch) {
		resource = apistructs.ProtectedBranchResource
	}

	checkPermission, err := c.Bundle.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   user.Id,
		Scope:    "app",
		ScopeID:  uint64(c.Repository.ApplicationId),
		Resource: resource,
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return err
	}
	if !checkPermission.Access {
		return fmt.Errorf("no permission to operate  branch:%s", branch)
	}

	return nil
}

func (c *Context) CheckPermissionWithRole(permission models.Permission, resourceRoleList []string) error {
	return c.Service.CheckPermission(c.Repository, c.User, permission, resourceRoleList)
}
