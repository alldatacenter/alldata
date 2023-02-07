package services

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/PuerkitoBio/goquery"
	entity2 "github.com/crawlab-team/crawlab-core/entity"
	"github.com/crawlab-team/crawlab-core/env/deps/constants"
	"github.com/crawlab-team/crawlab-core/env/deps/entity"
	"github.com/crawlab-team/crawlab-core/env/deps/models"
	"github.com/crawlab-team/go-trace"
	"github.com/imroc/req"
	"go.mongodb.org/mongo-driver/bson"
	mongo2 "go.mongodb.org/mongo-driver/mongo"
	"net/url"
	"os/exec"
	"path"
	"strconv"
	"strings"
	"time"
)

type PythonService struct {
	*BaseLangService
}

func (svc *PythonService) Init() {
}

func (svc *PythonService) GetRepoList(query string, pagination *entity2.Pagination) (deps []models.Dependency, total int, err error) {
	// request session
	reqSession := req.New()

	// set timeout
	reqSession.SetTimeout(15 * time.Second)

	// user agent
	ua := req.Header{"user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36"}

	// request url
	requestUrl := fmt.Sprintf("https://pypi.org/search?page=%d&q=%s", pagination.Page, url.QueryEscape(query))

	// perform request
	res, err := reqSession.Get(requestUrl, ua)
	if err != nil {
		if res != nil {
			err = errors.New(res.String())
			return nil, 0, trace.TraceError(err)
		}
		return nil, 0, trace.TraceError(err)
	}

	// response bytes
	data, err := res.ToBytes()
	if err != nil {
		return nil, 0, trace.TraceError(err)
	}
	buf := bytes.NewBuffer(data)

	// parse html
	doc, err := goquery.NewDocumentFromReader(buf)
	if err != nil {
		return nil, 0, trace.TraceError(err)
	}

	// dependencies
	var depNames []string
	doc.Find(".left-layout__main > form ul > li").Each(func(i int, s *goquery.Selection) {
		d := models.Dependency{
			Name:          s.Find(".package-snippet__name").Text(),
			LatestVersion: s.Find(".package-snippet__version").Text(),
		}
		deps = append(deps, d)
		depNames = append(depNames, d.Name)
	})

	// total
	totalStr := doc.Find(".left-layout__main .split-layout p > strong").Text()
	escapeStr := ",+"
	for _, c := range strings.Split(escapeStr, "") {
		totalStr = strings.ReplaceAll(totalStr, c, "")
	}
	total, _ = strconv.Atoi(totalStr)

	// empty results
	if total == 0 {
		return nil, 0, nil
	}

	// dependencies in db
	var depsResults []entity.DependencyResult
	pipelines := mongo2.Pipeline{
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
	if err := svc.parent.colD.Aggregate(pipelines, nil).All(&depsResults); err != nil {
		return nil, 0, trace.TraceError(err)
	}

	// dependencies map
	depsResultsMap := map[string]entity.DependencyResult{}
	for _, dr := range depsResults {
		depsResultsMap[dr.Name] = dr
	}

	// iterate dependencies
	for i, d := range deps {
		dr, ok := depsResultsMap[d.Name]
		if ok {
			deps[i].Result = dr
		}
	}

	return deps, total, nil
}

func (svc *PythonService) GetDependencies(params entity.UpdateParams) (deps []models.Dependency, err error) {
	cmd := exec.Command(params.Cmd, "list", "--format", "json")
	data, err := cmd.Output()
	if err != nil {
		return nil, err
	}
	var _deps []models.Dependency
	if err := json.Unmarshal(data, &_deps); err != nil {
		return nil, err
	}
	for _, d := range _deps {
		if strings.HasPrefix(d.Name, "-") {
			continue
		}
		d.Type = constants.DependencyTypePython
		deps = append(deps, d)
	}
	return deps, nil
}

func (svc *PythonService) InstallDependencies(params entity.InstallParams) (err error) {
	// arguments
	var args []string

	// install
	args = append(args, "install")

	// proxy
	if params.Proxy != "" {
		args = append(args, "-i")
		args = append(args, params.Proxy)
	}

	if params.UseConfig {
		// workspace path
		workspacePath, err := svc._getInstallWorkspacePath(params)
		if err != nil {
			return err
		}

		// config path
		configPath := path.Join(workspacePath, constants.DependencyConfigRequirementsTxt)

		// use config
		args = append(args, "-r")
		args = append(args, configPath)
	} else {
		// upgrade
		if params.Upgrade {
			args = append(args, "-U")
		}

		// dependency names
		for _, depName := range params.Names {
			args = append(args, depName)
		}
	}

	// command
	cmd := exec.Command(params.Cmd, args...)

	// logging
	svc.parent._configureLogging(params.TaskId, cmd)

	// start
	if err := cmd.Start(); err != nil {
		return trace.TraceError(err)
	}

	// wait
	if err := cmd.Wait(); err != nil {
		return trace.TraceError(err)
	}

	return nil
}

func (svc *PythonService) UninstallDependencies(params entity.UninstallParams) (err error) {
	// arguments
	var args []string

	// uninstall
	args = append(args, "uninstall")
	args = append(args, "-y")

	// dependency names
	for _, depName := range params.Names {
		args = append(args, depName)
	}

	// command
	cmd := exec.Command(params.Cmd, args...)

	// logging
	svc.parent._configureLogging(params.TaskId, cmd)

	// start
	if err := cmd.Start(); err != nil {
		return trace.TraceError(err)
	}

	// wait
	if err := cmd.Wait(); err != nil {
		return trace.TraceError(err)
	}

	return nil
}

func (svc *PythonService) GetLatestVersion(dep models.Dependency) (v string, err error) {
	// not exists in cache, request from pypi
	reqSession := req.New()

	// set timeout
	reqSession.SetTimeout(60 * time.Second)

	// user agent
	ua := req.Header{"user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36"}

	// request url
	requestUrl := fmt.Sprintf("https://pypi.org/project/%s/", dep.Name)

	// perform request
	res, err := reqSession.Get(requestUrl, ua)
	if err != nil {
		return "", trace.TraceError(err)
	}

	// response bytes
	data, err := res.ToBytes()
	if err != nil {
		return "", trace.TraceError(err)
	}
	buf := bytes.NewBuffer(data)

	// parse html
	doc, err := goquery.NewDocumentFromReader(buf)
	if err != nil {
		return "", trace.TraceError(err)
	}

	// latest version
	v = doc.Find(".release-timeline .release--current .release__version").Text()
	v = strings.TrimSpace(v)

	return v, nil
}

func NewPythonService(parent *Service) (svc *PythonService) {
	svc = &PythonService{}
	baseSvc := newBaseService(
		svc,
		parent,
		constants.DependencyTypePython,
		entity.MessageCodes{
			Update:    constants.MessageCodePythonUpdate,
			Save:      constants.MessageCodePythonSave,
			Install:   constants.MessageCodePythonInstall,
			Uninstall: constants.MessageCodePythonUninstall,
		},
	)
	svc.BaseLangService = baseSvc
	return svc
}
