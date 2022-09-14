package cicdv1

import (
	"bytes"
	"encoding/json"
	"fmt"
	jsonvalue "github.com/Andrew-M-C/go.jsonvalue"
	errors2 "github.com/pkg/errors"
	"github.com/rs/zerolog/log"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
	"gopkg.in/yaml.v3"
	"io/ioutil"
	"net/http"
	"net/url"
	"path/filepath"
	"strings"
	"time"
)

// 最大允许 cicd server 出错次数
const maxErrorTimes = 5

type CicdServer struct {
	endpoint         string
	appmanagerServer *appmanagerv1.AppManagerServer
}

// NewCicdServer 创建
func NewCicdServer(endpoint string, appmanagerServer *appmanagerv1.AppManagerServer) *CicdServer {
	return &CicdServer{
		endpoint:         endpoint,
		appmanagerServer: appmanagerServer,
	}
}

// SyncImage 用于将指定镜像同步到指定环境中，返回 UUID
func (s *CicdServer) SyncImage(env, image string) (string, error) {
	targetUrl := fmt.Sprintf("v2/SyncDockerImage/%s/%s", env, image)
	response, err := s.sendRequest(targetUrl)
	if err != nil {
		return "", err
	}
	uuid, err := response.GetString("Uuid")
	if err != nil {
		return "", errors2.Wrapf(err, "cannot parse cicd server response: %s", response.MustMarshalString())
	}
	log.Info().Msgf("sync image %s to %s has sent to cicd server, uuid=%s", image, env, uuid)
	return uuid, nil
}

// QuerySyncImageTask 用于查询同步镜像任务的当前状态，返回拷包小程序中的 Content Object
// {
//   "status": "done",
//   "process": [
//      "upload_images.done",
//      "download_images.done"
//   ],
//   "Logurl": "http://100.67.76.9:10030/v2/GetSyncImages/log/66E/36fb7434-33e5-11ec-b2d9-702084e188e4"
// }
func (s *CicdServer) QuerySyncImageTask(env, uuid string) (*jsonvalue.V, error) {
	targetUrl := fmt.Sprintf("v2/GetSyncImagesStatus/%s/%s", env, uuid)
	response, err := s.sendRequest(targetUrl)
	if err != nil {
		return nil, err
	}
	content, err := response.Get("Content")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot parse cicd server response: %s", response.MustMarshalString())
	}
	return content, nil
}

// SyncAppPackageImages 传送指定 appPackageId 对应的包到指定 unitId 单元，并返回实际替换的镜像列表
func (s *CicdServer) SyncAppPackageImages(arch, appId string, appPackageId int64, unitId string,
	timeout int) (*jsonvalue.V, error) {
	replaceImages := jsonvalue.NewArray()

	// 获取 registry endpoint 信息
	unit, err := s.appmanagerServer.GetUnit(unitId)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot get unit info from appmanager server")
	}
	registryEndpoint, err := unit.GetString("registryEndpoint")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot find registry endpoint in unit %s info", unitId)
	}

	// 获取 appPackage 信息
	appPackage, err := s.appmanagerServer.GetAppPackage(appId, appPackageId)
	if err != nil {
		return nil, errors2.Wrapf(err, "get app package from appmanager failed")
	}
	appSchemaStr, err := appPackage.GetString("appSchema")
	if err != nil {
		return nil, errors2.Wrapf(err, "get app schema from appmanager failed, appPackage=%s",
			appPackage.MustMarshalString())
	}
	appSchema := make(map[string]interface{})
	err = yaml.Unmarshal([]byte(appSchemaStr), &appSchema)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot unmarshal app schema yaml %s", appSchemaStr)
	}
	appSchemaJsonStr, err := json.Marshal(appSchema)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot marshal app schema map to json|%v", appSchema)
	}
	appSchemaJson, err := jsonvalue.Unmarshal(appSchemaJsonStr)
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot unmarshal app schema to json|%s", appSchemaJsonStr)
	}
	componentPackages, err := appSchemaJson.GetArray("componentPackages")
	if err != nil {
		return nil, errors2.Wrapf(err, "cannot get componentPackages from json|%s", appSchemaStr)
	}
	for componentPackage := range componentPackages.IterArray() {
		packageExtStr, err := componentPackage.V.GetString("packageExt")
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot get packageExt from json|%s", componentPackage.V.MustMarshalString())
		}
		packageExt := make(map[string]interface{})
		err = yaml.Unmarshal([]byte(packageExtStr), &packageExt)
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot unmarshal package ext yaml %s", packageExtStr)
		}
		packageExtJsonStr, _ := json.Marshal(packageExt)
		packageExtJson, err := jsonvalue.Unmarshal(packageExtJsonStr)
		if err != nil {
			return nil, errors2.Wrapf(err, "cannot unmarshal package ext yaml to json|%s", packageExtJsonStr)
		}
		spec, _ := packageExtJson.Get("spec")
		images, _ := spec.GetArray("images")
		for imageIter := range images.IterArray() {
			imageName, _ := imageIter.V.GetString("image")
			sha256, _ := imageIter.V.GetString("sha256")

			// 针对 abm.io 开头的放行
			if strings.HasPrefix(imageName, "abm.io") {
				continue
			}

			// 如果架构不匹配，跳过
			imageArch, imageArchErr := imageIter.V.GetString("arch")
			if imageArchErr == nil && len(imageArch) > 0 && imageArch != arch {
				log.Info().Msgf("image arch not match, skip, imageName=%s, sha256=%s", imageName, sha256)
				continue
			}

			replaceImages.Append(jsonvalue.NewObject(map[string]interface{}{
				"image": imageName,
				"actualImage": fmt.Sprintf("%s/%s:%s", registryEndpoint,
					imageName[strings.Index(imageName, "/")+1:strings.LastIndex(imageName, ":")], sha256),
			})).InTheEnd()
			log.Info().Msgf("prepare to sync image %s with sha256 %s", imageName, sha256)
			cicdImage := fmt.Sprintf("%s:%s",
				imageName[strings.Index(imageName, "/")+1:strings.LastIndex(imageName, ":")], sha256)

			log.Info().Msgf("cicd image parameter is %s", cicdImage)

			// 开始同步镜像
			uuid, err := s.SyncImage(unitId, cicdImage)
			if err != nil {
				return nil, errors2.Wrapf(err, "cannot sync image to unit-id %s", unitId)
			}
			retryTimes := timeout / 5
			errorTimes := 0
			success := false
			for i := 0; i < retryTimes; i++ {
				time.Sleep(5 * time.Second)
				response, err := s.QuerySyncImageTask(unitId, uuid)
				if err != nil {
					errorTimes++
					if errorTimes > maxErrorTimes {
						return nil, errors2.Wrapf(err, "cicd server may be broken, please contact them")
					} else {
						log.Info().Msgf("cicd server response is invalid, retry. err=%s", err)
						continue
					}
				}
				status, err := response.GetString("status")
				if err != nil {
					errorTimes++
					if errorTimes > maxErrorTimes {
						return nil, errors2.Wrapf(err, "cicd server may be broken, please contact theme")
					} else {
						log.Info().Msgf("cannot get status from cicd server, retry. err=%s", err)
						continue
					}
				}
				errorTimes = 0
				if status == "done" {
					log.Info().Msgf("image %s has synced to unit-id %s, response=%s",
						cicdImage, unitId, response.MustMarshalString())
					success = true
					break
				} else if status == "doing" {
					log.Info().Msgf("not finished yet...")
					continue
				} else if status == "failed" {
					return nil, errors2.Wrapf(err, "sync image %s to unit-id %s failed, reason=%s",
						cicdImage, unitId, response.MustMarshalString())
				} else {
					return nil, errors2.Wrapf(err, "unknown response from cicd server, response=%s",
						response.MustMarshalString())
				}
			}
			if success {
				continue
			} else {
				return nil, errors2.Wrapf(err, "sync images to unitId %s failed", unitId)
			}
		}
	}

	return replaceImages, nil
}

// sendRequest 发送请求到 cicd server，并返回输出内容
func (s *CicdServer) sendRequest(targetUrl string) (*jsonvalue.V, error) {
	fullUrl, err := url.Parse(s.endpoint)
	if err != nil {
		return nil, err
	}
	fullUrl.Path = filepath.Join(fullUrl.Path, targetUrl)
	request, err := http.NewRequest("GET", fullUrl.String(), bytes.NewBuffer(nil))
	if err != nil {
		return nil, err
	}

	log.Debug().Str("url", fullUrl.String()).Msgf("send request to cicd")
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()
	responseBody, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}
	log.Debug().RawJSON("response", responseBody).Msgf("get response from cicd")
	responseJson, err := jsonvalue.UnmarshalString(string(responseBody))
	if err != nil {
		return nil, err
	}
	if success, err := responseJson.GetBool("Success"); err != nil || success != true {
		return nil, errors2.Errorf("something wrong with cicd response: %s", responseBody)
	}
	return responseJson, nil
}
