package controllers

import (
	"errors"
	"fmt"
	"github.com/blang/semver/v4"
	"github.com/crawlab-team/crawlab-core/env/deps/constants"
	"github.com/crawlab-team/crawlab-core/env/deps/entity"
	envDepsModels "github.com/crawlab-team/crawlab-core/env/deps/models"
	envDepsServices "github.com/crawlab-team/crawlab-core/env/deps/services"
	"github.com/crawlab-team/crawlab-core/spider/fs"
	"github.com/crawlab-team/crawlab-core/utils"
	mongo2 "github.com/crawlab-team/crawlab-db/mongo"
	"github.com/crawlab-team/go-trace"
	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"io/ioutil"
	"net/http"
	"path"
	"regexp"
	"strconv"
	"strings"
)

var EnvDepsController ActionController

func getEnvDepsActions() []Action {
	ctx := newEnvDepsContext()
	return []Action{
		{
			Method:      http.MethodGet,
			Path:        "/settings",
			HandlerFunc: ctx.GetSettingList,
		},
		{
			Method:      http.MethodGet,
			Path:        "/settings/:id",
			HandlerFunc: ctx.GetSetting,
		},
		{
			Method:      http.MethodPost,
			Path:        "/settings",
			HandlerFunc: ctx.PostSetting,
		},
		{
			Method:      http.MethodPut,
			Path:        "/settings/:id",
			HandlerFunc: ctx.PutSetting,
		},
		{
			Method:      http.MethodDelete,
			Path:        "/settings/:id",
			HandlerFunc: ctx.DeleteSetting,
		},
		{
			Method:      http.MethodPost,
			Path:        "/settings/:id/enable",
			HandlerFunc: ctx.EnableSetting,
		},
		{
			Method:      http.MethodPost,
			Path:        "/settings/:id/disable",
			HandlerFunc: ctx.DisableSetting,
		},
		{
			Method:      http.MethodGet,
			Path:        "/tasks",
			HandlerFunc: ctx.GetTaskList,
		},
		{
			Method:      http.MethodGet,
			Path:        "/tasks/:id/logs",
			HandlerFunc: ctx.GetTaskLogs,
		},
		{
			Method:      http.MethodGet,
			Path:        "/spiders/:id",
			HandlerFunc: ctx.SpiderGet,
		},
		{
			Method:      http.MethodPost,
			Path:        "/spiders/:id/install",
			HandlerFunc: ctx.SpiderInstall,
		},
		{
			Method:      http.MethodPost,
			Path:        "/spiders/:id/uninstall",
			HandlerFunc: ctx.SpiderUninstall,
		},
		{
			Method:      http.MethodGet,
			Path:        "/lang/:lang",
			HandlerFunc: ctx.LangGetList,
		},
		{
			Method:      http.MethodPost,
			Path:        "/lang/:lang/update",
			HandlerFunc: ctx.LangUpdate,
		},
		{
			Method:      http.MethodPost,
			Path:        "/lang/:lang/install",
			HandlerFunc: ctx.LangInstall,
		},
		{
			Method:      http.MethodPost,
			Path:        "/lang/:lang/uninstall",
			HandlerFunc: ctx.LangUninstall,
		},
	}
}

type envDepsContext struct {
	svc *envDepsServices.Service
}

func (ctx *envDepsContext) Svc() *envDepsServices.Service {
	return ctx.svc
}

func (ctx *envDepsContext) GetSettingList(c *gin.Context) {
	svc := ctx.Svc().SettingSvc()

	// params
	pagination := MustGetPagination(c)
	query := MustGetFilterQuery(c)
	sort := MustGetSortOption(c)

	// get list
	var list []envDepsModels.Setting
	if err := svc.Col().Find(query, &mongo2.FindOptions{
		Sort:  sort,
		Skip:  pagination.Size * (pagination.Page - 1),
		Limit: pagination.Size,
	}).All(&list); err != nil {
		if err.Error() == mongo.ErrNoDocuments.Error() {
			HandleSuccessWithListData(c, nil, 0)
		} else {
			HandleErrorInternalServerError(c, err)
		}
		return
	}

	// total count
	total, err := svc.Col().Count(query)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccessWithListData(c, list, total)
}

func (ctx *envDepsContext) GetSetting(c *gin.Context) {
	svc := ctx.Svc().SettingSvc()

	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	var s envDepsModels.Setting
	if err := svc.Col().FindId(id).One(&s); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccessWithData(c, s)
}

func (ctx *envDepsContext) PostSetting(c *gin.Context) {
	svc := ctx.Svc().SettingSvc()

	var s envDepsModels.Setting
	if err := c.ShouldBindJSON(&s); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	s.Id = primitive.NewObjectID()
	if _, err := svc.Col().Insert(s); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccessWithData(c, s)
}

func (ctx *envDepsContext) PutSetting(c *gin.Context) {
	svc := ctx.Svc().SettingSvc()

	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	var s envDepsModels.Setting
	if err := svc.Col().FindId(id).One(&s); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	if err := c.ShouldBindJSON(&s); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}
	s.Id = id

	if err := svc.Col().ReplaceId(id, s); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccessWithData(c, s)
}

func (ctx *envDepsContext) DeleteSetting(c *gin.Context) {
	svc := ctx.Svc().SettingSvc()

	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	if err := svc.Col().DeleteId(id); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccess(c)
}

func (ctx *envDepsContext) EnableSetting(c *gin.Context) {
	ctx._toggleSettingFunc(true)(c)
}

func (ctx *envDepsContext) DisableSetting(c *gin.Context) {
	ctx._toggleSettingFunc(false)(c)
}

func (ctx *envDepsContext) _toggleSettingFunc(value bool) func(c *gin.Context) {
	svc := ctx.Svc().SettingSvc()

	return func(c *gin.Context) {
		id, err := primitive.ObjectIDFromHex(c.Param("id"))
		if err != nil {
			HandleErrorBadRequest(c, err)
			return
		}
		var s envDepsModels.Setting
		if err := svc.Col().FindId(id).One(&s); err != nil {
			HandleErrorInternalServerError(c, err)
			return
		}
		s.Enabled = value
		if err := svc.Col().ReplaceId(id, s); err != nil {
			HandleErrorInternalServerError(c, err)
			return
		}
		HandleSuccess(c)
	}
}

func (ctx *envDepsContext) GetTaskList(c *gin.Context) {
	// filter
	query, _ := GetFilterQuery(c)

	// all
	all, _ := strconv.ParseBool(c.Query("all"))

	// pagination
	pagination := MustGetPagination(c)

	// options
	opts := &mongo2.FindOptions{
		Sort: bson.D{{"_id", -1}},
	}
	if !all {
		opts.Skip = (pagination.Page - 1) * pagination.Size
		opts.Limit = pagination.Size
	}

	// tasks
	var tasks []envDepsModels.Task
	if err := ctx.svc.ColT().Find(query, opts).All(&tasks); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// total
	total, err := ctx.svc.ColT().Count(query)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccessWithListData(c, tasks, total)
}

func (ctx *envDepsContext) GetTaskLogs(c *gin.Context) {
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	var logList []envDepsModels.Log
	if err := ctx.Svc().ColL().Find(bson.M{"task_id": id}, nil).All(&logList); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccessWithData(c, logList)
}

func (ctx *envDepsContext) SpiderInstall(c *gin.Context) {
	// spider id
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	// payload
	var payload entity.InstallPayload
	if err := c.ShouldBindJSON(&payload); err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	// spider fs service
	fsSvc, err := fs.NewSpiderFsService(id)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// sync to workspace
	if err := fsSvc.GetFsService().SyncToWorkspace(); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// workspace path
	workspacePath := fsSvc.GetWorkspacePath()

	// dependency type
	dependencyType := ctx._getDependencyType(workspacePath)

	// lang service
	var langSvc envDepsServices.IBaseLangService
	switch dependencyType {
	case constants.DependencyConfigRequirementsTxt:
		langSvc = ctx.Svc().PythonSvc()
	case constants.DependencyConfigPackageJson:
		langSvc = ctx.Svc().NodeSvc()
	default:
		HandleErrorInternalServerError(c, errors.New(fmt.Sprintf("invalid dependency type: %s", dependencyType)))
		return
	}

	// install
	if err := langSvc.Install(payload); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccess(c)
}

func (ctx *envDepsContext) SpiderUninstall(c *gin.Context) {
	// spider id
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	// payload
	var payload entity.UninstallPayload
	if err := c.ShouldBindJSON(&payload); err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	// spider fs service
	fsSvc, err := fs.NewSpiderFsService(id)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// sync to workspace
	if err := fsSvc.GetFsService().SyncToWorkspace(); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// workspace path
	workspacePath := fsSvc.GetWorkspacePath()

	// dependency type
	dependencyType := ctx._getDependencyType(workspacePath)

	// lang service
	var langSvc envDepsServices.IBaseLangService
	switch dependencyType {
	case constants.DependencyConfigRequirementsTxt:
		langSvc = ctx.Svc().PythonSvc()
	case constants.DependencyConfigPackageJson:
		langSvc = ctx.Svc().NodeSvc()
	default:
		HandleErrorInternalServerError(c, errors.New(fmt.Sprintf("invalid dependency type: %s", dependencyType)))
		return
	}

	// uninstall
	if err := langSvc.Uninstall(payload); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccess(c)
}

func (ctx *envDepsContext) SpiderGet(c *gin.Context) {
	// spider id
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		HandleErrorBadRequest(c, err)
		return
	}

	// spider fs service
	fsSvc, err := fs.NewSpiderFsService(id)
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// sync to workspace
	if err := fsSvc.GetFsService().SyncToWorkspace(); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// workspace path
	workspacePath := fsSvc.GetWorkspacePath()

	// dependency type
	dependencyType := ctx._getDependencyType(workspacePath)

	// spider info
	info := bson.M{}
	info["dependency_type"] = dependencyType

	// dependencies
	var dependencies []envDepsModels.Dependency
	switch dependencyType {
	case constants.DependencyConfigRequirementsTxt:
		dependencies, err = ctx._getDependenciesRequirementsTxt(workspacePath)
	case constants.DependencyConfigPackageJson:
		// TODO: implement
	}
	if err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}
	info["dependencies"] = dependencies

	HandleSuccessWithData(c, info)
}

func (ctx *envDepsContext) _getDependencyType(workspacePath string) (t string) {
	if utils.Exists(path.Join(workspacePath, constants.DependencyConfigRequirementsTxt)) {
		return constants.DependencyConfigRequirementsTxt
	} else if utils.Exists(path.Join(workspacePath, constants.DependencyConfigPackageJson)) {
		return constants.DependencyConfigPackageJson
	}
	return ""
}

func (ctx *envDepsContext) _getDependenciesRequirementsTxt(workspacePath string) (deps []envDepsModels.Dependency, err error) {
	// file path
	filePath := path.Join(workspacePath, constants.DependencyConfigRequirementsTxt)

	// file content
	data, err := ioutil.ReadFile(filePath)
	if err != nil {
		return nil, trace.TraceError(err)
	}
	content := string(data)

	// regex pattern
	// python distribution names rules according to https://peps.python.org/pep-0508/#names
	pattern, err := regexp.Compile("(?i)([A-Z0-9][A-Z0-9._-]*[A-Z0-9])([=<>]=.*)?")

	if err != nil {
		return nil, trace.TraceError(err)
	}

	// dependency names
	var depNames []string

	// iterate content lines
	for _, line := range strings.Split(content, "\n") {
		// trim space
		line = strings.TrimSpace(line)

		// validate regex match result
		if !pattern.MatchString(line) {
			return nil, trace.TraceError(errors.New(fmt.Sprintf("invalid %s", constants.DependencyConfigRequirementsTxt)))
		}

		// sub matches
		matches := pattern.FindStringSubmatch(line)
		if len(matches) < 2 {
			return nil, trace.TraceError(errors.New(fmt.Sprintf("invalid %s", constants.DependencyConfigRequirementsTxt)))
		}

		// dependency result
		d := envDepsModels.Dependency{
			Name: matches[1],
		}

		// required version
		if len(matches) > 2 {
			d.Version = matches[2]
		}

		// add to dependency names
		depNames = append(depNames, d.Name)

		// add to dependencies
		deps = append(deps, d)
	}

	// dependencies in db
	var depsResults []entity.DependencyResult
	pipelines := mongo.Pipeline{
		{{
			"$match",
			bson.M{
				"type": constants.DependencyTypePython,
				"name": bson.M{
					"$in": depNames,
				},
			},
		}},
		{{
			"$group",
			bson.M{
				"_id": "$name",
				"node_ids": bson.M{
					"$push": "$node_id",
				},
				"versions": bson.M{
					"$addToSet": "$version",
				},
			},
		}},
		{{
			"$project",
			bson.M{
				"name":     "$_id",
				"node_ids": "$node_ids",
				"versions": "$versions",
			},
		}},
	}
	if err := ctx.Svc().ColD().Aggregate(pipelines, nil).All(&depsResults); err != nil {
		return nil, err
	}

	// dependencies map
	depsResultsMap := map[string]entity.DependencyResult{}
	for _, dr := range depsResults {
		depsResultsMap[dr.Name] = dr
	}

	// iterate dependencies
	for i, d := range deps {
		// operator
		var op string
		if strings.HasPrefix(d.Version, "==") {
			op = "=="
			deps[i].Version = strings.Replace(d.Version, "==", "", 1)
		} else if strings.HasPrefix(d.Version, ">=") {
			op = ">="
		} else if strings.HasPrefix(d.Version, "<=") {
			op = "<="
		}

		// dependency result
		dr, ok := depsResultsMap[d.Name]
		if !ok {
			continue
		}
		deps[i].Result = dr

		// normalized version
		nv := strings.Replace(d.Version, op, "", 1)

		// required version
		rv, err := semver.Make(nv)
		if err != nil {
			continue
		}

		// iterate installed versions
		for _, v := range dr.Versions {
			// installed version
			iv, err := semver.Make(v)
			if err != nil {
				continue
			}

			// compare with the required version
			switch op {
			case "==":
				if rv.Compare(iv) > 0 {
					deps[i].Result.Upgradable = true
				} else if rv.Compare(iv) < 0 {
					deps[i].Result.Downgradable = true
				}
			case ">=":
				if rv.Compare(iv) > 0 {
					deps[i].Result.Upgradable = true
				}
			case "<=":
				if rv.Compare(iv) < 0 {
					deps[i].Result.Downgradable = true
				}
			}
		}
	}

	return deps, nil
}

func (ctx *envDepsContext) LangGetList(c *gin.Context) {
	svc := ctx.getLangSvc(c)
	installed, _ := strconv.ParseBool(c.Query("installed"))
	if installed {
		searchQuery := c.Query("query")
		pagination := MustGetPagination(c)
		results, total, err := svc.GetInstalledList(searchQuery, pagination)
		if err != nil {
			HandleErrorInternalServerError(c, err)
			return
		}
		HandleSuccessWithListData(c, results, total)
	} else {
		searchQuery := c.Query("query")
		pagination := MustGetPagination(c)
		results, total, err := svc.DepSvc().GetRepoList(searchQuery, pagination)
		if err != nil {
			HandleErrorInternalServerError(c, err)
			return
		}
		HandleSuccessWithListData(c, results, total)
	}
}

func (ctx *envDepsContext) LangUpdate(c *gin.Context) {
	svc := ctx.getLangSvc(c)

	// update
	if err := svc.Update(); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccess(c)
}

func (ctx *envDepsContext) LangInstall(c *gin.Context) {
	svc := ctx.getLangSvc(c)

	// payload
	var payload entity.InstallPayload
	if err := c.ShouldBindJSON(&payload); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// install
	if err := svc.Install(payload); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccess(c)
}

func (ctx *envDepsContext) LangUninstall(c *gin.Context) {
	svc := ctx.getLangSvc(c)

	// payload
	var payload entity.UninstallPayload
	if err := c.ShouldBindJSON(&payload); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	// uninstall
	if err := svc.Uninstall(payload); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}

	HandleSuccess(c)
}

func (ctx *envDepsContext) Update(c *gin.Context) {
	svc := ctx.getLangSvc(c)
	if err := svc.Update(); err != nil {
		HandleErrorInternalServerError(c, err)
		return
	}
	HandleSuccess(c)
}

func (ctx *envDepsContext) getLangSvc(c *gin.Context) (svc envDepsServices.IBaseLangService) {
	lang := c.Param("lang")

	switch lang {
	case "python":
		return ctx.Svc().PythonSvc()
	case "node":
		return ctx.Svc().NodeSvc()
	default:
		return nil
	}
}

func newEnvDepsContext() *envDepsContext {
	return &envDepsContext{
		svc: envDepsServices.GetService(),
	}
}
