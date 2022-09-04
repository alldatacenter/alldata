package appmanagerv1

import (
	"archive/zip"
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	jsonvalue "github.com/Andrew-M-C/go.jsonvalue"
	errors2 "github.com/pkg/errors"
	"github.com/rs/zerolog/log"
	"gopkg.in/yaml.v3"
)

// 最大构建任务等待查询时间
const maxBuildTaskQueryWaitTimes = 3600

// 最大部署单等待时间
const maxDeploymentWaitTimes = 600

type AppManagerServer struct {
	client   *http.Client
	endpoint string
}

// NewAppManagerServer 创建
func NewAppManagerServer(client *http.Client, endpoint string) *AppManagerServer {
	return &AppManagerServer{
		client:   client,
		endpoint: endpoint,
	}
}

// Build 根据给定 path 的 Yaml 配置请求 appmanager server 进行构建
func (s *AppManagerServer) Build(appId, filepath string, tags []string, branch string, wait bool) (*jsonvalue.V, error) {
	components, err := s.readBuildComponents(filepath, branch)
	if err != nil {
		return nil, err
	}

	postData := jsonvalue.NewObject()
	postData.SetString(appId).At("appId")
	postData.SetArray().At("tags")
	postDataTags, _ := postData.GetArray("tags")
	for _, tag := range tags {
		postDataTags.AppendString(tag).InTheEnd()
	}
	postData.Set(components).At("components")

	// 发送请求
	response, err := s.sendRequest(
		"POST", fmt.Sprintf("apps/%s/app-package-tasks", appId), nil, postData.MustMarshal(), "")
	if err != nil {
		return nil, err
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Errorf("cannot parse appmanager response: %s", response.MustMarshalString())
	}
	appPackageTaskId, err := data.GetInt64("appPackageTaskId")
	if err != nil {
		return nil, errors2.Errorf("cannot get app package task id from appmanager server response: %s",
			response.MustMarshalString())
	}
	log.Info().Msgf("app package task has submited, appPackageTaskId=%d", appPackageTaskId)

	if !wait {
		return data, nil
	}
	return s.QueryBuildTask(appId, appPackageTaskId, wait)
}

// ImportPackage 将本地的 zip 应用包导入到远端 appmanager
func (s *AppManagerServer) ImportPackage(appId string, filepath string, resetVersion bool) (*jsonvalue.V, error) {
	// 从 zip 文件中获取 package version
	packageVersion := ""
	r, err := zip.OpenReader(filepath)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot read zip file %s", filepath)
	}
	for _, f := range r.File {
		if f.Name == "meta.yaml" {
			rc, err := f.Open()
			if err != nil {
				return nil, errors2.Wrapf(err, "cannot open meta.yaml in zip file %s", filepath)
			}
			metaConfigBytes, err := ioutil.ReadAll(rc)
			if err != nil {
				return nil, errors2.Wrapf(err, "cannot read meta.yaml in zip file %s", filepath)
			}
			metaConfig := make(map[interface{}]interface{})
			err = yaml.Unmarshal(metaConfigBytes, &metaConfig)
			if err != nil {
				return nil, errors2.Wrapf(err, "cannot unmarshal launch yaml %s", filepath)
			}
			var exists bool
			var packageVersionObj interface{}
			packageVersionObj, exists = metaConfig["packageVersion"]
			if !exists {
				return nil, errors2.Errorf("cannot get package version from meta.yaml")
			}
			packageVersion = packageVersionObj.(string)
			break
		}
	}
	if len(packageVersion) == 0 {
		return nil, errors2.Errorf("cannot find meta.yaml in zip file, invalid zip file %s", filepath)
	}
	r.Close()

	// 发送 zip 文件做导入
	forceFlag := "true"
	if resetVersion {
		forceFlag = "false"
	}
	params := map[string]string{
		"appId":          appId,
		"packageVersion": packageVersion,
		"force":          forceFlag,
		"resetVersion":   strconv.FormatBool(resetVersion),
	}
	zipfileBytes, err := ioutil.ReadFile(filepath)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot read zip file %s", filepath)
	}
	response, err := s.sendRequest("POST", fmt.Sprintf("apps/%s/app-packages/import", appId), params,
		zipfileBytes, "application/octet-stream")
	if err != nil {
		return nil, errors2.Wrapf(err, "import app package to appmanager failed")
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Errorf("cannot parse appmanager response: %s", response.MustMarshalString())
	}
	return data, nil
}

// RemoteLaunch 启动远程部署
func (s *AppManagerServer) RemoteLaunch(unitId, appId string, params map[string]string,
	filepath string, arch string, cluster string, replaceImages *jsonvalue.V, wait bool) (*jsonvalue.V, error) {
	// 获取指定单元下的最新 AppPackage ID
	appPackageId, packageVersion, err := s.GetUnitAppPackageId(unitId, appId, params)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot filter unit %s app packages by params %v", unitId, params)
	}
	log.Info().Msgf("get unit app package id success|unitId=%s|appId=%s|appPackageId=%d|packageVersion=%s",
		unitId, appId, appPackageId, packageVersion)

	appPackage, err := s.GetUnitAppPackage(unitId, appId, appPackageId, packageVersion)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot get unit %s app package by id %d", unitId, appPackageId)
	}
	log.Info().
		RawJSON("appPackage", appPackage.MustMarshal()).
		Int64("appPackageId", appPackageId).
		Msgf("get latest app package from appmanager server in unit %s", unitId)

	// 读取 Yaml 文件
	fileStr, err := s.readLaunchYaml(filepath, arch, cluster)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot read launch yaml %s", filepath)
	}

	// 向 Yaml 中注入 metadata 信息
	config := make(map[interface{}]interface{})
	err = yaml.Unmarshal([]byte(fileStr), &config)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot unmarshal launch yaml %s", filepath)
	}
	config["metadata"] = map[string]interface{}{
		"name": appId,
		"annotations": map[string]interface{}{
			"appId":        appId,
			"appPackageId": appPackageId,
			"clusterId":    cluster,
			"imageTars":    replaceImages.MustMarshalString(),
		},
	}
	postData, err := yaml.Marshal(config)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot marshal launch yaml to post data")
	}

	// 发起部署请求
	launchParams := map[string]string{
		"autoEnvironment": "true",
	}
	response, err := s.sendRequest("POST",
		fmt.Sprintf("units/%s/deployments/launch", unitId), launchParams, postData, "")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot launch appmanager deployment")
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Errorf("cannot parse appmanager response: %s", response.MustMarshalString())
	}
	deployAppId, err := data.GetInt64("deployAppId")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot get deployAppId field in appmanager response: %s",
			response.MustMarshalString())
	}
	log.Info().Msgf("app package has deployed, deploy app id %d", deployAppId)

	if !wait {
		return data, nil
	}
	return s.QueryUnitDeployment(unitId, deployAppId, wait)
}

// Launch 本地启动部署
func (s *AppManagerServer) Launch(appId string, appPackageId int64,
	filepath string, arch string, cluster, namespaceId, stageId string,
	appInstanceName string, wait bool) (*jsonvalue.V, error) {
	fileStr, err := s.readLaunchYaml(filepath, arch, cluster)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot read launch yaml %s", filepath)
	}

	// 向 Yaml 中注入 metadata 信息
	config := make(map[interface{}]interface{})
	err = yaml.Unmarshal([]byte(fileStr), &config)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot unmarshal launch yaml %s", filepath)
	}
	config["metadata"] = map[string]interface{}{
		"name": appId,
		"annotations": map[string]interface{}{
			"appId":           appId,
			"appPackageId":    appPackageId,
			"clusterId":       cluster,
			"namespaceId":     namespaceId,
			"stageId":         stageId,
			"appInstanceName": appInstanceName,
		},
	}
	postData, err := yaml.Marshal(config)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot marshal launch yaml to post data")
	}

	// 发起部署请求
	params := map[string]string{
		"autoEnvironment": "true",
	}
	response, err := s.sendRequest("POST", "deployments/launch", params, postData, "")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot launch appmanager deployment")
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Errorf("cannot parse appmanager response: %s", response.MustMarshalString())
	}
	deployAppId, err := data.GetInt64("deployAppId")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot get deployAppId field in appmanager response: %s",
			response.MustMarshalString())
	}
	log.Info().Msgf("app package has deployed, deploy app id %d", deployAppId)

	if !wait {
		return data, nil
	}
	return s.GetDeployment(deployAppId, wait)
}

// QueryUnitDeployment 查询
func (s *AppManagerServer) QueryUnitDeployment(unitId string, deployAppId int64, wait bool) (*jsonvalue.V, error) {
	waitCount := 0
	for waitCount < maxDeploymentWaitTimes {
		response, err := s.sendRequest("GET", fmt.Sprintf("units/%s/deployments/%d", unitId, deployAppId), nil, nil, "")
		if err != nil {
			return nil, errors2.Wrapf(err, "query deployment failed, deployAppId=%d", deployAppId)
		}
		data, err := response.Get("data")
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
		}
		deployStatus, err := data.GetString("deployStatus")
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
		}
		if deployStatus == "WAIT_FOR_OP" || deployStatus == "FAILURE" || deployStatus == "EXCEPTION" {
			return data, errors2.Errorf("failed deployment")
		} else if deployStatus == "SUCCESS" {
			return data, nil
		} else {
			if wait {
				log.Info().Msgf("not finished yet...")
				time.Sleep(5 * time.Second)
				waitCount++
			} else {
				return data, errors2.Errorf("running deployment")
			}
		}
	}
	return nil, errors2.Errorf("wait timeout, failed deployment")
}

// QueryDeployment 查询
func (s *AppManagerServer) GetDeployment(deployAppId int64, wait bool) (*jsonvalue.V, error) {
	waitCount := 0
	for waitCount < maxDeploymentWaitTimes {
		response, err := s.sendRequest("GET", fmt.Sprintf("deployments/%d", deployAppId), nil, nil, "")
		if err != nil {
			return nil, errors2.Wrapf(err, "query deployment failed, deployAppId=%d", deployAppId)
		}
		data, err := response.Get("data")
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
		}
		deployStatus, err := data.GetString("deployStatus")
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
		}
		if deployStatus == "WAIT_FOR_OP" || deployStatus == "FAILURE" || deployStatus == "EXCEPTION" {
			return data, errors2.Errorf("failed deployment")
		} else if deployStatus == "SUCCESS" {
			return data, nil
		} else {
			if wait {
				log.Info().Msgf("not finished yet...")
				time.Sleep(5 * time.Second)
				waitCount++
			} else {
				return data, errors2.Errorf("running deployment")
			}
		}
	}
	return nil, errors2.Errorf("wait timeout, failed deployment")
}

// RemoteSync 将指定应用的最新包同步到指定 region 的服务下
func (s *AppManagerServer) RemoteSync(unitId, appId string, appPackageId int64) (*jsonvalue.V, error) {
	postData, err := json.Marshal(map[string]interface{}{
		"unitId": unitId,
	})
	response, err := s.sendRequest("POST",
		fmt.Sprintf("apps/%s/app-packages/%d/sync", appId, appPackageId), nil, postData, "")
	if err != nil {
		return nil, err
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
	}
	return data, nil
}

func (s *AppManagerServer) ListDeployment(appId string, page, pageSize int64) (*jsonvalue.V, error) {
	params := map[string]string{
		"page":     strconv.FormatInt(page, 10),
		"pageSize": strconv.FormatInt(pageSize, 10),
		"appId":    appId,
	}
	response, err := s.sendRequest("GET", "deployments", params, nil, "")
	if err != nil {
		return nil, err
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
	}
	return data, nil
}

// GetAppPackageId 获取指定条件查询到的 app package id (第一个)
func (s *AppManagerServer) GetAppPackageId(appId string, params map[string]string) (int64, error) {
	response, err := s.sendRequest("GET", fmt.Sprintf("apps/%s/app-packages", appId), params, nil, "")
	if err != nil {
		return 0, err
	}
	data, err := response.Get("data")
	if err != nil {
		return 0, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
	}
	items, err := data.GetArray("items")
	if err != nil {
		return 0, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
	}
	item, err := items.Get(0)
	if err != nil {
		return 0, errors2.Errorf("cannot find any app packages match the params %v", params)
	}
	appPackageId, err := item.GetInt64("id")
	if err != nil {
		return 0, errors2.Errorf("cannot get app package id from response: %s", response.MustMarshalString())
	}
	return appPackageId, nil
}

// GetUnit 获取指定单元的连接信息
func (s *AppManagerServer) GetUnit(unitId string) (*jsonvalue.V, error) {
	response, err := s.sendRequest("GET", fmt.Sprintf("units/%s", unitId), nil, nil, "")
	if err != nil {
		return nil, err
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
	}
	return data, nil
}

// GetAppPackage 获取指定 appPackageId 的应用包
func (s *AppManagerServer) GetAppPackage(appId string, appPackageId int64) (*jsonvalue.V, error) {
	response, err := s.sendRequest("GET",
		fmt.Sprintf("apps/%s/app-packages/%d", appId, appPackageId), nil, nil, "")
	if err != nil {
		return nil, err
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
	}
	return data, nil
}

// GetUnitAppPackageId 获取指定单元中过滤条件 params 对应的应用包 (单个)
func (s *AppManagerServer) GetUnitAppPackageId(unitId, appId string, params map[string]string) (int64, string, error) {
	response, err := s.sendRequest("GET", fmt.Sprintf("units/%s/apps/%s/app-packages", unitId, appId), params, nil, "")
	if err != nil {
		return 0, "", err
	}
	data, err := response.Get("data")
	if err != nil {
		return 0, "", errors2.Wrapf(err, "cannot parse unit appmanager server response: %s", response.MustMarshalString())
	}
	items, err := data.GetArray("items")
	if err != nil {
		return 0, "", errors2.Wrapf(err, "cannot parse unit appmanager server response: %s", response.MustMarshalString())
	}
	item, err := items.Get(0)
	if err != nil {
		return 0, "", errors2.Errorf("cannot find any app packages match the params %v", params)
	}
	appPackageId, err := item.GetInt64("id")
	if err != nil {
		return 0, "", errors2.Errorf("cannot get app package id from response: %s", response.MustMarshalString())
	}
	packageVersion, err := item.GetString("packageVersion")
	if err != nil {
		return 0, "", errors2.Errorf("cannot get app package version from response: %s", response.MustMarshalString())
	}
	return appPackageId, packageVersion, nil
}

// GetUnitAppPackage 获取指定单元中指定 appPackageId 对应的应用包
func (s *AppManagerServer) GetUnitAppPackage(unitId, appId string, appPackageId int64,
	packageVersion string) (*jsonvalue.V, error) {
	response, err := s.sendRequest("GET",
		fmt.Sprintf("units/%s/apps/%s/app-packages/%d", unitId, appId, appPackageId), nil, nil, "")
	if err != nil {
		// 兼容老版本 appmanager
		if strings.Contains(err.Error(), "Not Found") {
			response, err = s.sendRequest("GET",
				fmt.Sprintf("units/%s/apps/%s/app-packages", unitId, appId), map[string]string{
					"packageVersion": packageVersion,
				}, nil, "")
			if err != nil {
				return nil, errors2.Wrapf(err, "get unit app package by package version %s failed|unitId=%s",
					packageVersion, unitId)
			}
		} else {
			return nil, errors2.Wrapf(err, "get unit app package by id %d failed|unitId=%s", appPackageId, unitId)
		}
	}
	data, err := response.Get("data")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot parse unit appmanager server response: %s", response.MustMarshalString())
	}
	return data, nil
}

// QueryBuildTask 用于查询构建任务的详细情况
func (s *AppManagerServer) QueryBuildTask(appId string, appPackageTaskId int64, wait bool) (*jsonvalue.V, error) {
	response, actualErr := s.queryBuildTaskWithoutDetails(appId, appPackageTaskId, wait)
	// 如果没有 response，那么 err 一定存在出错
	if response == nil {
		return nil, actualErr
	}
	withBlobs := false
	if actualErr != nil {
		withBlobs = true
	}
	components, err := s.queryBuildTaskDetails(appId, appPackageTaskId, withBlobs)
	if err != nil {
		return nil, errors2.Wrapf(err, "query build task details failed")
	}
	response.Set(components).At("components")
	return response, actualErr
}

func (s *AppManagerServer) queryBuildTaskWithoutDetails(appId string, appPackageTaskId int64,
	wait bool) (*jsonvalue.V, error) {
	waitCount := 0
	for waitCount < maxBuildTaskQueryWaitTimes {
		response, err := s.sendRequest("GET",
			fmt.Sprintf("apps/%s/app-package-tasks/%d", appId, appPackageTaskId), nil, nil, "")
		if err != nil {
			return nil, err
		}
		data, err := response.Get("data")
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
		}
		taskStatus, err := data.GetString("taskStatus")
		if err != nil {
			return nil, err
		}
		if taskStatus == "FAILURE" || taskStatus == "EXCEPTION" {
			return data, errors2.Errorf("build app package failed")
		} else if taskStatus == "SUCCESS" {
			return data, nil
		} else {
			if wait {
				log.Info().Msgf("not finished yet...")
				time.Sleep(5 * time.Second)
				waitCount++
			} else {
				return data, errors2.Errorf("build task is running now...")
			}
		}
	}
	return nil, errors2.Errorf("wait timeout")
}

func (s *AppManagerServer) queryBuildTaskDetails(appId string, appPackageTaskId int64,
	withBlobs bool) (*jsonvalue.V, error) {
	params := map[string]string{
		"appPackageTaskId": strconv.FormatInt(appPackageTaskId, 10),
		"withBlobs":        strconv.FormatBool(withBlobs),
		"pageSize":         "1000",
		"page":             "1",
	}
	response, err := s.sendRequest("GET",
		fmt.Sprintf("apps/%s/component-package-tasks", appId), params, nil, "")
	if err != nil {
		return nil, err
	}
	data, err := response.Get("data", "items")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot parse appmanager server response: %s", response.MustMarshalString())
	}
	for item := range data.IterArray() {
		taskLog, err := item.V.GetString("taskLog")
		if err != nil {
			continue
		}
		taskLogArray := strings.Split(taskLog, "\n")
		item.V.SetArray().At("taskLog")
		for _, content := range taskLogArray {
			item.V.AppendString(content).InTheEnd("taskLog")
		}
	}
	return data, nil
}

// readLaunchYaml 读取指定 filepath 路径对应的 launch.yaml 文件，并渲染 arch + cluster (兼容原始方式)
func (s *AppManagerServer) readLaunchYaml(filepath, arch, cluster string) (string, error) {
	fileBytes, err := ioutil.ReadFile(filepath)
	if err != nil {
		return "", err
	}
	fileStr := string(fileBytes)

	optionalCluster := ""
	clusterSuffix := ""
	clusterPrefix := ""
	if cluster != "master" {
		optionalCluster = "slave"
		clusterSuffix = "-slave"
		clusterPrefix = "slave-"
	}
	fileStr = strings.ReplaceAll(fileStr, "${CLUSTER}", cluster)
	fileStr = strings.ReplaceAll(fileStr, "${OPTIONAL_CLUSTER}", optionalCluster)
	fileStr = strings.ReplaceAll(fileStr, "${CLUSTER_SUFFIX}", clusterSuffix)
	fileStr = strings.ReplaceAll(fileStr, "${CLUSTER_PREFIX}", clusterPrefix)
	fileStr = strings.ReplaceAll(fileStr, "${ARCH}", arch)
	return fileStr, nil
}

// readBuildComponents 读取指定 filepath 路径对应的 yaml 文件，并返回解析后的 components 列表
func (s *AppManagerServer) readBuildComponents(filepath, branch string) (*jsonvalue.V, error) {
	f, err := os.Open(filepath)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	components := jsonvalue.NewArray()
	decoder := yaml.NewDecoder(f)
	for {
		content := make(map[string]interface{})
		err = decoder.Decode(&content)
		if errors.Is(err, io.EOF) {
			break
		}
		if content == nil {
			continue
		}
		if err != nil {
			return nil, err
		}

		// map -> jsonvalue
		contentJsonStr, err := json.Marshal(content)
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot convert component content to json")
		}
		contentJson, err := jsonvalue.UnmarshalString(string(contentJsonStr))
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot unmarshal string from component content json")
		}

		if _, err := contentJson.Get("componentType"); err == jsonvalue.ErrNotFound || err != nil {
			return nil, errors.New("cannot find componentType in build process")
		}
		if _, err := contentJson.Get("componentName"); err == jsonvalue.ErrNotFound || err != nil {
			return nil, errors.New("cannot find componentName in build process")
		}
		contentJson.SetBool(true).At("useRawOptions")
		if len(strings.TrimSpace(branch)) > 0 {
			containers, err := contentJson.GetArray("options", "containers")
			if err == nil {
				for _, v := range containers.ForRangeArr() {
					v.SetString(branch).At("build", "branch")
				}
			} else if err != jsonvalue.ErrNotFound {
				return nil, errors2.Wrapf(err, "get containers in options failed")
			}
			initContainersJson, err := contentJson.Get("options", "initContainers")
			if err == nil {
				for _, v := range initContainersJson.ForRangeArr() {
					v.SetString(branch).At("build", "branch")
				}
			} else if err != jsonvalue.ErrNotFound {
				return nil, errors2.Wrapf(err, "get initContainers in options failed")
			}
		}
		log.Info().Msgf("current component options: %s", contentJson.MustMarshalString())
		components.Append(contentJson).InTheEnd()
	}

	return components, nil
}

// sendRequest 发送请求到 appmanager server，并检查返回值
func (s *AppManagerServer) sendRequest(method, relativeUrl string,
	params map[string]string, data []byte, contentType string) (*jsonvalue.V, error) {
	fullUrl, err := url.Parse(s.endpoint)
	if err != nil {
		return nil, err
	}
	fullUrl.Path = filepath.Join(fullUrl.Path, relativeUrl)
	request, err := http.NewRequest(method, fullUrl.String(), bytes.NewBuffer(data))
	if err != nil {
		return nil, err
	}

	// 补充 params 到 URL
	if params != nil {
		q := request.URL.Query()
		for key, value := range params {
			q.Add(key, value)
		}
		request.URL.RawQuery = q.Encode()
	}

	if method == "GET" {
		log.Debug().Str("url", fullUrl.String()).Msgf("send request to appmanager")
	} else {
		if len(contentType) > 0 {
			request.Header.Set("Content-Type", contentType)
		} else {
			request.Header.Set("Content-Type", "application/json")
		}
		log.Debug().Str("url", fullUrl.String()).RawJSON("data", data).Msgf("send request to appmanager")
	}
	response, err := s.client.Do(request)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()
	responseBody, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}
	log.Debug().RawJSON("response", responseBody).Msgf("get response from appmanager")
	responseJson, err := jsonvalue.UnmarshalString(string(responseBody))
	if err != nil {
		return nil, err
	}
	if code, err := responseJson.GetInt("code"); err != nil || code != 200 {
		return nil, errors2.Errorf("something wrong with appmanager response: %s", responseBody)
	}
	return responseJson, nil
}
