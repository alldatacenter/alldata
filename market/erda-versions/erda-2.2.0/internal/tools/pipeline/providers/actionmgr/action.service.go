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

package actionmgr

import (
	"context"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"time"

	"sigs.k8s.io/yaml"

	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/limit_sync_group"
)

type actionService struct {
	p            *provider
	dbClient     *db.Client
	edgeRegister edgepipeline_register.Interface
	clusterInfo  clusterinfo.Interface
}

func (s *actionService) CheckInternalClient(ctx context.Context) error {
	if apis.GetInternalClient(ctx) == "" {
		return fmt.Errorf("not internal client req")
	}
	return nil
}

func (s *actionService) List(ctx context.Context, req *pb.PipelineActionListRequest) (*pb.PipelineActionListResponse, error) {
	if len(req.Locations) == 0 {
		return nil, apierrors.ErrListPipelineAction.InvalidParameter("locations was empty")
	}
	if req.ActionNameWithVersionQuery != nil {
		for _, query := range req.ActionNameWithVersionQuery {
			if query.Name == "" {
				return nil, apierrors.ErrListPipelineAction.InvalidParameter(fmt.Errorf("actionNameWithVersionQuery: name can not empty"))
			}
		}
	}

	actions, err := s.dbClient.ListPipelineAction(req)
	if err != nil {
		return nil, err
	}

	var data []*pb.Action
	for _, action := range actions {
		actionDto, err := action.Convert(req.YamlFormat)
		if err != nil {
			return nil, apierrors.ErrListPipelineAction.InternalError(err)
		}
		data = append(data, actionDto)
	}

	return &pb.PipelineActionListResponse{
		Data: actionsOrderByLocationIndex(req.Locations, data),
	}, nil
}

func actionsOrderByLocationIndex(locations []string, data []*pb.Action) []*pb.Action {
	var locationActionMap = map[string][]*pb.Action{}
	for _, action := range data {
		locationActionMap[action.Location] = append(locationActionMap[action.Location], action)
	}

	var orderAction []*pb.Action
	for _, location := range locations {
		orderAction = append(orderAction, locationActionMap[location]...)
	}
	return orderAction
}

func (s *actionService) Save(ctx context.Context, req *pb.PipelineActionSaveRequest) (*pb.PipelineActionSaveResponse, error) {
	if err := s.CheckInternalClient(ctx); err != nil {
		return nil, apierrors.ErrListPipelineAction.InternalError(err)
	}

	if req.Spec == "" {
		return nil, apierrors.ErrSavePipelineAction.InvalidParameter("spec yml was empty")
	}
	if req.Dice == "" {
		return nil, apierrors.ErrSavePipelineAction.InvalidParameter("dice yml was empty")
	}
	if req.Location == "" {
		return nil, apierrors.ErrSavePipelineAction.InvalidParameter("location was empty")
	}
	if !strings.HasSuffix(req.Location, string(os.PathSeparator)) {
		return nil, apierrors.ErrSavePipelineAction.InvalidParameter(fmt.Errorf("location need %v suffix", string(os.PathSeparator)))
	}

	saveAction, err := PipelineActionSaveRequestToAction(req)
	if err != nil {
		return nil, apierrors.ErrSavePipelineAction.InternalError(err)
	}
	if saveAction.Name == "" {
		return nil, apierrors.ErrSavePipelineAction.InvalidParameter("spec name was empty")
	}
	if saveAction.VersionInfo == "" {
		return nil, apierrors.ErrSavePipelineAction.InvalidParameter("spec version was empty")
	}

	var saveActionResult *db.PipelineAction
	err = Transaction(s.dbClient, func(option mysqlxorm.SessionOption) error {
		saveActionResult, err = s.saveAction(saveAction, req, option)
		if err != nil {
			return err
		}

		return s.syncActionToEdge(func(bdl *bundle.Bundle) error {
			_, err := bdl.SavePipelineAction(req)
			if err != nil {
				return err
			}
			return nil
		})
	})
	if err != nil {
		return nil, apierrors.ErrSavePipelineAction.InternalError(err)
	}

	result, err := saveActionResult.Convert(false)
	if err != nil {
		return nil, apierrors.ErrSavePipelineAction.InternalError(err)
	}

	return &pb.PipelineActionSaveResponse{
		Action: result,
	}, nil
}

func (s *actionService) syncActionToEdge(do func(bdl *bundle.Bundle) error) error {
	if s.edgeRegister.IsEdge() {
		return nil
	}

	edgeClusters, err := s.clusterInfo.ListEdgeClusterInfos()
	if err != nil {
		return err
	}

	wait := limit_sync_group.NewWorker(5)
	for index := range edgeClusters {
		wait.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			index := i[0].(int)
			edgeCluster := edgeClusters[index]

			bdl, err := s.edgeRegister.GetEdgeBundleByClusterName(edgeCluster.Name)
			if err != nil {
				return err
			}
			return do(bdl)
		}, index)
	}

	return wait.Do().Error()
}

func (s *actionService) saveAction(saveAction *db.PipelineAction, req *pb.PipelineActionSaveRequest, option mysqlxorm.SessionOption) (*db.PipelineAction, error) {
	actions, err := s.dbClient.ListPipelineAction(&pb.PipelineActionListRequest{
		Locations: []string{req.Location},
		ActionNameWithVersionQuery: []*pb.ActionNameWithVersionQuery{
			{
				Name:    saveAction.Name,
				Version: saveAction.VersionInfo,
			},
		},
	}, option)
	if err != nil {
		return nil, apierrors.ErrSavePipelineAction.InternalError(err)
	}

	var insert = true
	for _, action := range actions {
		if action.Location == req.Location {
			saveAction.ID = action.ID
			insert = false
			break
		}
	}

	if insert {
		saveAction.TimeCreated = time.Now()
		saveAction.ID = uuid.SnowFlakeID()
		err := s.dbClient.InsertPipelineAction(saveAction)
		if err != nil {
			return nil, apierrors.ErrSavePipelineAction.InternalError(err)
		}
	} else {
		err := s.dbClient.UpdatePipelineAction(saveAction.ID, saveAction, option)
		if err != nil {
			return nil, apierrors.ErrSavePipelineAction.InternalError(err)
		}
	}
	return saveAction, nil
}

func PipelineActionSaveRequestToAction(req *pb.PipelineActionSaveRequest) (saveAction *db.PipelineAction, err error) {
	var specInfo apistructs.Spec
	err = yaml.Unmarshal([]byte(req.Spec), &specInfo)
	if err != nil {
		return nil, err
	}

	saveAction = &db.PipelineAction{}

	saveAction.VersionInfo = specInfo.Version
	saveAction.Name = specInfo.Name
	saveAction.Desc = specInfo.Desc
	saveAction.DisplayName = specInfo.DisplayName
	saveAction.IsDefault = specInfo.IsDefault
	saveAction.IsPublic = specInfo.Public
	saveAction.LogoUrl = specInfo.LogoUrl
	saveAction.Category = specInfo.Category
	saveAction.TimeUpdated = time.Now()

	saveAction.Spec = req.Spec
	saveAction.Location = req.Location
	saveAction.Dice = req.Dice
	saveAction.Readme = req.Readme
	return saveAction, nil
}

func (s *actionService) Delete(ctx context.Context, req *pb.PipelineActionDeleteRequest) (*pb.PipelineActionDeleteResponse, error) {
	if err := s.CheckInternalClient(ctx); err != nil {
		return nil, apierrors.ErrListPipelineAction.InternalError(err)
	}

	if req.Location == "" {
		return nil, apierrors.ErrDeletePipelineAction.InvalidParameter("location was empty")
	}
	if req.Name == "" {
		return nil, apierrors.ErrDeletePipelineAction.InvalidParameter("name was empty")
	}
	if req.Version == "" {
		return nil, apierrors.ErrDeletePipelineAction.InvalidParameter("version was empty")
	}

	err := Transaction(s.dbClient, func(option mysqlxorm.SessionOption) error {
		err := s.deleteAction(req)
		if err != nil {
			return err
		}

		return s.syncActionToEdge(func(bdl *bundle.Bundle) error {
			return bdl.DeletePipelineAction(req)
		})
	})
	if err != nil {
		return nil, err
	}

	return &pb.PipelineActionDeleteResponse{}, nil
}

func (s *actionService) deleteAction(req *pb.PipelineActionDeleteRequest) error {
	actions, err := s.dbClient.ListPipelineAction(&pb.PipelineActionListRequest{
		Locations: []string{req.Location},
		ActionNameWithVersionQuery: []*pb.ActionNameWithVersionQuery{
			{
				Name:    req.Name,
				Version: req.Version,
			},
		},
	})
	if err != nil {
		return apierrors.ErrSavePipelineAction.InternalError(err)
	}

	var deleteAction *db.PipelineAction
	for _, action := range actions {
		if action.Location == req.Location {
			deleteAction = &action
			break
		}
	}

	if deleteAction == nil {
		return apierrors.ErrSavePipelineAction.InternalError(fmt.Errorf("not find action name %v version %v location %v", req.Name, req.Version, req.Location))
	}

	deleteAction.SoftDeletedAt = time.Now().UnixNano() / 1e6
	err = s.dbClient.DeletePipelineAction(deleteAction.ID, deleteAction)
	if err != nil {
		return apierrors.ErrSavePipelineAction.InternalError(err)
	}

	return nil
}

func Transaction(dbClient *db.Client, do func(option mysqlxorm.SessionOption) error) error {
	txSession := dbClient.NewSession()
	defer txSession.Close()
	if err := txSession.Begin(); err != nil {
		return err
	}
	err := do(mysqlxorm.WithSession(txSession))
	if err != nil {
		if rbErr := txSession.Rollback(); rbErr != nil {
			return err
		}
		return err
	}
	if cmErr := txSession.Commit(); cmErr != nil {
		return cmErr
	}
	return nil
}

func (s *actionService) InitAction(addr string) {
	s.p.Log.Info("Start init action")
	defer s.p.Log.Info("end init action")

	// load action repo by addr
	repo := LoadActionRepo(addr)
	for _, v := range repo.versions {
		// NewSaveRequest by version addr
		saveRequest, err := NewSaveRequest(v)
		if err != nil {
			s.p.Log.Errorf("make create request error %v", err)
			continue
		}
		_, err = s.Save(apis.WithInternalClientContext(context.Background(), "pipeline"), saveRequest)
		if err != nil {
			s.p.Log.Errorf("Save action request %v error %v", saveRequest, err)
			continue
		}
	}
}

func NewSaveRequest(dirname string) (*pb.PipelineActionSaveRequest, error) {
	fileInfos, err := ioutil.ReadDir(dirname)
	if err != nil {
		return nil, fmt.Errorf("failed to ReadDir %v", err)
	}

	var request = &pb.PipelineActionSaveRequest{}
	for _, fileInfo := range fileInfos {
		if fileInfo.IsDir() {
			continue
		}
		switch {
		case strings.EqualFold(fileInfo.Name(), "spec.yml") || strings.EqualFold(fileInfo.Name(), "spec.yaml"):
			specJson, err := ioutil.ReadFile(filepath.Join(dirname, fileInfo.Name()))
			if err != nil {
				return nil, fmt.Errorf("failed to ReadFile spec %v", err)
			}
			request.Spec = string(specJson)
		case strings.EqualFold(fileInfo.Name(), "dice.yml") || strings.EqualFold(fileInfo.Name(), "dice.yaml"):
			diceJson, err := ioutil.ReadFile(filepath.Join(dirname, fileInfo.Name()))
			if err != nil {
				return nil, fmt.Errorf("failed to ReadFile dice %v", err)
			}
			request.Dice = string(diceJson)
		case strings.EqualFold(fileInfo.Name(), "readme.md") || strings.EqualFold(fileInfo.Name(), "readme.markdown"):
			readmeJson, err := ioutil.ReadFile(filepath.Join(dirname, fileInfo.Name()))
			if err != nil {
				return nil, fmt.Errorf("failed to ReadFile readme %v", err)
			}
			request.Readme = string(readmeJson)
		}
	}

	if request.Spec == "" {
		return nil, fmt.Errorf("spec can not empty")
	}

	var spec apistructs.Spec
	err = yaml.Unmarshal([]byte(request.Spec), &spec)
	if err != nil {
		return nil, fmt.Errorf("failed to Unmarshal spec %v", err)
	}
	if spec.Type != string(apistructs.SpecActionType) {
		return nil, fmt.Errorf("pipeline action only support action")
	}

	request.Location = apistructs.PipelineTypeDefault.String() + "/"
	return request, nil
}

func LoadActionRepo(addr string) *Repo {
	repo := &Repo{
		addr: addr,
	}
	repo.locate(repo.addr, 0)
	return repo
}

type Repo struct {
	// addr workPath
	addr string
	// versions action version dir path
	versions []string
}

// locate Recursively traverse folders
func (repo *Repo) locate(dirname string, deep int) {
	infos, ok := isThereSpecFile(dirname)
	if ok {
		repo.versions = append(repo.versions, dirname)
		return
	}

	for _, cur := range infos {
		// only find path /repoName/actions
		if deep == 1 && cur.Name() != "actions" {
			continue
		}
		repo.locate(filepath.Join(dirname, cur.Name()), deep+1)
	}
}

// isThereSpecFile  check is there have spec.yml
func isThereSpecFile(dirname string) ([]os.FileInfo, bool) {
	var dirs []os.FileInfo
	infos, err := ioutil.ReadDir(dirname)
	if err != nil {
		return nil, false
	}
	for _, file := range infos {
		if file.IsDir() {
			dirs = append(dirs, file)
			continue
		}
		if strings.EqualFold(file.Name(), "spec.yml") || strings.EqualFold(file.Name(), "spec.yaml") {
			return nil, true
		}
	}
	return dirs, false
}
