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

package autotestv2

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

type AutoTestSpaceDB struct {
	Data *AutoTestSpaceData
}

func (a *AutoTestSpaceDB) CreateNewSpace() (*apistructs.AutoTestSpace, error) {
	var err error
	spaceName := a.Data.Space.Name
	if a.Data.IsCopy {
		spaceName, err = a.Data.svc.GenerateSpaceName(spaceName, int64(a.Data.ProjectID))
		if err != nil {
			return nil, err
		}
	}
	space, err := a.Data.svc.CreateSpace(apistructs.AutoTestSpaceCreateRequest{
		Name:         spaceName,
		ProjectID:    int64(a.Data.ProjectID),
		Description:  a.Data.Space.Description,
		IdentityInfo: a.Data.IdentityInfo,
	})
	a.Data.NewSpace = space
	return space, nil
}

func (a *AutoTestSpaceDB) SetSpace() error {
	space, err := a.Data.svc.GetSpace(a.Data.SpaceID)
	//space, err := a.svc.db.GetAutoTestSpace(a.Data.SpaceID)
	if err != nil {
		return err
	}
	a.Data.Space = space
	return nil
}

func (a *AutoTestSpaceDB) SetSceneSets() error {
	a.Data.SceneSets = map[uint64][]apistructs.SceneSet{}
	sceneSets, err := a.Data.svc.GetSceneSetsBySpaceID(a.Data.Space.ID)
	if err != nil {
		return err
	}
	a.Data.SceneSets[a.Data.Space.ID] = sceneSets
	return nil
}

func (a *AutoTestSpaceDB) SetSingleSceneSet(SetID uint64) error {
	a.Data.SceneSets = map[uint64][]apistructs.SceneSet{}
	sceneSet, err := a.Data.svc.GetSceneSet(SetID)
	if err != nil {
		return err
	}
	a.Data.SceneSets[a.Data.SpaceID] = []apistructs.SceneSet{*sceneSet}
	return nil
}

func (a *AutoTestSpaceDB) SetScenes() error {
	a.Data.Scenes = map[uint64][]apistructs.AutoTestScene{}
	for _, v := range a.Data.SceneSets[a.Data.Space.ID] {
		_, scenes, err := a.Data.svc.ListAutotestScene(apistructs.AutotestSceneRequest{
			SetID:        v.ID,
			IdentityInfo: a.Data.IdentityInfo,
		})
		if err != nil {
			return err
		}
		sceneIDs := []uint64{}
		for _, v := range scenes {
			sceneIDs = append(sceneIDs, v.ID)
		}
		inputs, err := a.Data.svc.ListAutoTestSceneInputByScenes(sceneIDs)
		if err != nil {
			return err
		}
		inputMap := map[uint64][]apistructs.AutoTestSceneInput{}
		for _, input := range inputs {
			inputMap[input.SceneID] = append(inputMap[input.SceneID], input)
		}
		outputs, err := a.Data.svc.ListAutoTestSceneOutputByScenes(sceneIDs)
		if err != nil {
			return err
		}
		outputMap := map[uint64][]apistructs.AutoTestSceneOutput{}
		for _, output := range outputs {
			outputMap[output.SceneID] = append(outputMap[output.SceneID], output)
		}
		for i := 0; i < len(scenes); i++ {
			scenes[i].Inputs = inputMap[scenes[i].ID]
			scenes[i].Output = outputMap[scenes[i].ID]
		}
		a.Data.Scenes[v.ID] = scenes
	}
	return nil
}

func (a *AutoTestSpaceDB) SetSceneSteps() error {
	a.Data.Steps = map[uint64][]apistructs.AutoTestSceneStep{}
	for _, scenes := range a.Data.Scenes {
		for _, scene := range scenes {
			steps, err := a.Data.svc.ListAutoTestSceneStep(scene.ID)
			if err != nil {
				return err
			}
			a.Data.Steps[scene.ID] = steps
		}
	}
	return nil
}

func (a *AutoTestSpaceDB) SetConfigs() error {
	var autoTestGlobalConfigListRequest apistructs.AutoTestGlobalConfigListRequest
	autoTestGlobalConfigListRequest.ScopeID = strconv.Itoa(int(a.Data.Space.ProjectID))
	autoTestGlobalConfigListRequest.Scope = "project-autotest-testcase"
	autoTestGlobalConfigListRequest.UserID = a.Data.UserID
	configs, err := a.Data.svc.autotestSvc.ListGlobalConfigs(autoTestGlobalConfigListRequest)
	if err != nil {
		return err
	}
	a.Data.Configs = configs
	return nil
}

func (a *AutoTestSpaceDB) GetSpaceData() *AutoTestSpaceData {
	return a.Data
}

// Export accept space export request and return uint64 file id,
// then send file id to channel make export sync
func (svc *Service) Export(req apistructs.AutoTestSpaceExportRequest) (uint64, error) {
	// check parameter
	if !req.FileType.Valid() {
		return 0, apierrors.ErrExportAutoTestSpace.InvalidParameter("fileType")
	}

	fileName := svc.MakeAutotestFileName(req.SpaceName)
	if req.FileType == apistructs.TestSpaceFileTypeExcel {
		fileName += ".xlsx"
	}
	fileReq := apistructs.TestFileRecordRequest{
		FileName:     fileName,
		Description:  req.SpaceName,
		Type:         apistructs.FileSpaceActionTypeExport,
		State:        apistructs.FileRecordStatePending,
		ProjectID:    req.ProjectID,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			AutotestSpaceFileExtraInfo: &apistructs.AutoTestSpaceFileExtraInfo{
				ExportRequest: &req,
			},
		},
	}
	id, err := svc.CreateFileRecord(fileReq)
	if err != nil {
		return 0, err
	}

	return id, nil
}

func (svc *Service) ExportFile(record *dao.TestFileRecord) {
	extra := record.Extra.AutotestSpaceFileExtraInfo
	if extra == nil || extra.ExportRequest == nil {
		logrus.Errorf("autotest space export func missing request data")
		return
	}

	req := extra.ExportRequest
	id := record.ID
	fileName := record.FileName
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSpace.InternalError(err))
		return
	}
	w := bytes.Buffer{}
	spaceDBData := AutoTestSpaceDB{Data: &AutoTestSpaceData{
		svc:          svc,
		IdentityInfo: req.IdentityInfo,
		Locale:       req.Locale,
		SpaceID:      req.ID,
		IsCopy:       req.IsCopy,
	},
	}

	creator := AutoTestSpaceDirector{}
	creator.New(&spaceDBData)
	if err := creator.Construct(); err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSpace.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
			logrus.Error(apierrors.ErrExportAutoTestSpace.InternalError(err))
		}
		return
	}
	spaceData := creator.Creator.GetSpaceData()

	if err := spaceData.ConvertToExcel(&w, fileName); err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSpace.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
			logrus.Error(apierrors.ErrExportAutoTestSpace.InternalError(err))
		}
		return
	}

	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: fileName,
		ByteSize:        int64(w.Len()),
		FileReader:      ioutil.NopCloser(&w),
		From:            "Autotest space",
		IsPublic:        true,
		ExpiredAt:       nil,
	}
	file, err := svc.bdl.UploadFile(uploadReq)
	if err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSpace.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
			logrus.Error(apierrors.ErrExportAutoTestSpace.InternalError(err))
		}
		return
	}

	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateSuccess, ApiFileUUID: file.UUID}); err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSpace.InternalError(err))
		return
	}
}

func (svc *Service) ExportSceneSet(req apistructs.AutoTestSceneSetExportRequest) (uint64, error) {
	if !req.FileType.Valid() {
		return 0, apierrors.ErrExportAutoTestSceneSet.InvalidParameter("fileType")
	}

	fileName := svc.MakeAutotestFileName(req.SceneSetName)
	if req.FileType == apistructs.TestSceneSetFileTypeExcel {
		fileName += ".xlsx"
	}
	fileReq := apistructs.TestFileRecordRequest{
		FileName:     fileName,
		Description:  req.SceneSetName,
		Type:         apistructs.FileSceneSetActionTypeExport,
		State:        apistructs.FileRecordStatePending,
		ProjectID:    req.ProjectID,
		SpaceID:      req.SpaceID,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			AutotestSceneSetFileExtraInfo: &apistructs.AutoTestSceneSetFileExtraInfo{
				ExportRequest: &req,
			},
		},
	}
	id, err := svc.CreateFileRecord(fileReq)
	if err != nil {
		return 0, apierrors.ErrExportAutoTestSceneSet.InternalError(err)
	}
	return id, nil
}

func (svc *Service) ExportSceneSetFile(record *dao.TestFileRecord) {
	extra := record.Extra.AutotestSceneSetFileExtraInfo
	if extra == nil || extra.ExportRequest == nil {
		logrus.Errorf("autotest scene set export missing request data")
		return
	}

	req := extra.ExportRequest
	id := record.ID
	fileName := record.FileName
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		return
	}
	spaceDBData := AutoTestSpaceDB{Data: &AutoTestSpaceData{
		svc:          svc,
		IdentityInfo: req.IdentityInfo,
		Locale:       req.Locale,
		SpaceID:      req.SpaceID,
		IsCopy:       req.IsCopy,
	},
	}

	creator := AutoTestSpaceDirector{}
	creator.New(&spaceDBData)
	if err := creator.ConstructFromSceneSet(req.ID); err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
			logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		}
		return
	}
	spaceData := creator.Creator.GetSpaceData()
	w := bytes.Buffer{}
	if err := spaceData.ConvertSceneSetToExcel(&w, fileName); err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
			logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		}
		return
	}
	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: fileName,
		ByteSize:        int64(w.Len()),
		FileReader:      ioutil.NopCloser(&w),
		From:            "Autotest Scene Set",
		IsPublic:        true,
		ExpiredAt:       nil,
	}
	file, err := svc.bdl.UploadFile(uploadReq)
	if err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
			logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		}
		return
	}

	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateSuccess, ApiFileUUID: file.UUID}); err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		return
	}
}

func (svc *Service) MakeAutotestFileName(origin string) string {
	return fmt.Sprintf("%s_%s", origin, time.Now().Format("20060102150405"))
}
