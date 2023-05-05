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
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/excel"
)

// AutoTestSpaceExcel convert excel data to space datas
// will achieve AutoTestSpaceDataCreator implement
type AutoTestSpaceExcel struct {
	sheets [][][]string
	Data   *AutoTestSpaceData
}

func convertToUint64PermitZero(idStr string) (uint64, error) {
	id, err := strconv.Atoi(idStr)
	if err != nil {
		return 0, err
	}
	return uint64(id), nil
}
func convertStrIDToUint64(idStr string) (uint64, error) {
	id, err := strconv.Atoi(idStr)
	if err != nil {
		return 0, err
	}
	if id == 0 {
		return 0, fmt.Errorf("invalid id")
	}
	return uint64(id), nil
}

// SetSpace get space data from sheets[0]
func (a *AutoTestSpaceExcel) SetSpace() error {
	var err error
	spaceSheet := a.sheets[0]
	if len(spaceSheet) != 2 {
		return fmt.Errorf("missing space data")
	}
	if len(spaceSheet[1]) != 4 {
		return fmt.Errorf("invalid space data row")
	}
	space := apistructs.AutoTestSpace{}
	space.ID, err = convertStrIDToUint64(spaceSheet[1][0])
	if err != nil {
		return err
	}
	space.Name = spaceSheet[1][1]
	space.ProjectID = int64(a.Data.ProjectID)
	space.Description = spaceSheet[1][3]
	a.Data.Space = &space
	return nil
}

// SetSceneSets get scene sets data from sheets[1]
func (a *AutoTestSpaceExcel) SetSceneSets() error {
	var err error
	sheet := a.sheets[1]
	if len(sheet) < 1 {
		return fmt.Errorf("missing scene set data")
	}
	a.Data.SceneSets = map[uint64][]apistructs.SceneSet{}
	for _, setRow := range sheet[1:] {
		if len(setRow) != 5 {
			return fmt.Errorf("invalid scene set data")
		}
		sceneSet := apistructs.SceneSet{}
		sceneSet.ID, err = convertStrIDToUint64(setRow[0])
		if err != nil {
			return err
		}
		sceneSet.Name = setRow[1]
		sceneSet.SpaceID = a.Data.Space.ID
		sceneSet.PreID, err = convertToUint64PermitZero(setRow[3])
		if err != nil {
			return err
		}
		sceneSet.Description = setRow[4]
		a.Data.SceneSets[a.Data.Space.ID] = append(a.Data.SceneSets[a.Data.Space.ID], sceneSet)
	}
	return nil
}

func (a *AutoTestSpaceExcel) SetSingleSceneSet(setID uint64) error {
	return nil
}

// SetScenes get scenes, input, output data
// and insert input, output data to scenes
func (a *AutoTestSpaceExcel) SetScenes() error {
	var err error
	a.Data.Scenes = map[uint64][]apistructs.AutoTestScene{}

	inputSheet := a.sheets[3]
	if len(inputSheet) < 1 {
		return fmt.Errorf("missing input data")
	}
	inputList := []apistructs.AutoTestSceneInput{}
	for _, inputRow := range inputSheet[1:] {
		if len(inputRow) != 7 {
			return fmt.Errorf("invalid input data")
		}
		input := apistructs.AutoTestSceneInput{}
		input.ID, err = convertStrIDToUint64(inputRow[0])
		if err != nil {
			return err
		}
		input.Name = inputRow[1]
		input.Value = inputRow[2]
		input.Temp = inputRow[3]
		input.SceneID, err = convertStrIDToUint64(inputRow[4])
		if err != nil {
			return err
		}
		input.SpaceID = a.Data.Space.ID
		input.Description = inputRow[6]
		inputList = append(inputList, input)
	}

	outputSHeet := a.sheets[4]
	if len(outputSHeet) < 1 {
		return fmt.Errorf("missing output data")
	}
	outputList := []apistructs.AutoTestSceneOutput{}
	for _, outputRow := range outputSHeet[1:] {
		output := apistructs.AutoTestSceneOutput{}
		output.ID, err = convertStrIDToUint64(outputRow[0])
		if err != nil {
			return err
		}
		output.Name = outputRow[1]
		output.Value = outputRow[2]
		output.SceneID, err = convertStrIDToUint64(outputRow[3])
		if err != nil {
			return err
		}
		output.SpaceID = a.Data.Space.ID
		output.Description = outputRow[5]
		outputList = append(outputList, output)
	}

	sheet := a.sheets[2]
	if len(sheet) < 1 {
		return fmt.Errorf("missing scene data")
	}
	for _, sceneRow := range sheet[1:] {
		if len(sceneRow) < 7 {
			return fmt.Errorf("invalid scene data")
		}
		scene := apistructs.AutoTestScene{}
		scene.ID, err = convertStrIDToUint64(sceneRow[0])
		if err != nil {
			return err
		}
		scene.Name = sceneRow[1]
		scene.SetID, err = convertStrIDToUint64(sceneRow[2])
		if err != nil {
			return err
		}
		scene.SpaceID = a.Data.Space.ID
		scene.PreID, err = convertToUint64PermitZero(sceneRow[4])
		if err != nil {
			return err
		}
		scene.RefSetID, err = convertToUint64PermitZero(sceneRow[5])
		if err != nil {
			return err
		}
		scene.Description = sceneRow[6]
		// group id
		if len(sceneRow) >= 8 {
			scene.GroupID, err = convertToUint64PermitZero(sceneRow[7])
			if err != nil {
				return err
			}
		}
		// policy
		if len(sceneRow) >= 9 {
			scene.Policy = apistructs.PolicyType(sceneRow[8])
			if !scene.Policy.IsValid() {
				return fmt.Errorf("invalid policy, scene name: %s, policy: %s", scene.Name, scene.Policy)
			}
		}
		for _, input := range inputList {
			if input.SceneID == scene.ID {
				scene.Inputs = append(scene.Inputs, input)
			}
		}
		for _, output := range outputList {
			if output.SceneID == scene.ID {
				scene.Output = append(scene.Output, output)
			}
		}
		if a.Data.Scenes[scene.SetID] == nil {
			a.Data.Scenes[scene.SetID] = []apistructs.AutoTestScene{}
		}
		a.Data.Scenes[scene.SetID] = append(a.Data.Scenes[scene.SetID], scene)
	}
	return nil
}

//SetSceneSteps get steps data and judge step`s children
func (a *AutoTestSpaceExcel) SetSceneSteps() error {
	var err error
	a.Data.Steps = map[uint64][]apistructs.AutoTestSceneStep{}
	sheet := a.sheets[5]

	if len(sheet) < 1 {
		return fmt.Errorf("missing scene step data")
	}

	for _, stepRow := range sheet[1:] {
		if len(stepRow) < 9 {
			return fmt.Errorf("invalid scene step data")
		}
		step := apistructs.AutoTestSceneStep{}
		step.ID, err = convertStrIDToUint64(stepRow[0])
		if err != nil {
			return err
		}
		step.Name = stepRow[1]
		step.Value = stepRow[2]
		step.Type = apistructs.StepAPIType(stepRow[3])
		step.PreID, err = convertToUint64PermitZero(stepRow[4])
		if err != nil {
			return err
		}
		step.SceneID, err = convertStrIDToUint64(stepRow[5])
		if err != nil {
			return err
		}
		step.SpaceID = a.Data.Space.ID
		step.PreType = apistructs.PreType(stepRow[7])
		step.APISpecID, err = convertToUint64PermitZero(stepRow[8])
		if err != nil {
			return err
		}
		if len(stepRow) > 9 {
			step.IsDisabled, err = strconv.ParseBool(stepRow[9])
			if err != nil {
				return err
			}
		}
		if a.Data.Steps[step.SceneID] == nil {
			a.Data.Steps[step.SceneID] = []apistructs.AutoTestSceneStep{}
		}
		a.Data.Steps[step.SceneID] = append(a.Data.Steps[step.SceneID], step)
	}

	for sceneID, scs := range a.Data.Steps {
		type idType struct {
			PreID   uint64
			PreType apistructs.PreType
		}
		stepMap := make(map[idType]*apistructs.AutoTestSceneStep)
		for i := range scs {
			stepMap[idType{scs[i].PreID, scs[i].PreType}] = &scs[i]
		}
		var steps []apistructs.AutoTestSceneStep
		for head := uint64(0); ; {
			s, ok := stepMap[idType{head, apistructs.PreTypeSerial}]
			if !ok {
				break
			}
			head = s.ID
			for head2 := s.ID; ; {
				s2, ok := stepMap[idType{head2, apistructs.PreTypeParallel}]
				if !ok {
					break
				}
				head2 = s2.ID
				s.Children = append(s.Children, *s2)
			}
			steps = append(steps, *s)
		}
		a.Data.Steps[sceneID] = steps
	}

	return nil
}

// SetConfigs get api configs
func (a *AutoTestSpaceExcel) SetConfigs() error {
	sheet := a.sheets[6]
	a.Data.Configs = []apistructs.AutoTestGlobalConfig{}
	if len(sheet) < 1 {
		return fmt.Errorf("missing config data")
	}
	for _, configRow := range sheet[1:] {
		if len(configRow) != 7 {
			return fmt.Errorf("invalid config data")
		}
		config := apistructs.AutoTestGlobalConfig{}
		config.Scope = configRow[0]
		config.ScopeID = configRow[1]
		config.Ns = configRow[2]
		config.DisplayName = configRow[3]
		config.Desc = configRow[4]
		config.APIConfig = &apistructs.AutoTestAPIConfig{Global: map[string]apistructs.AutoTestConfigItem{}}
		config.APIConfig.Domain = configRow[5]
		headerStr := configRow[6]
		var header map[string]string
		if err := json.Unmarshal([]byte(headerStr), &header); err != nil {
			return err
		}
		config.APIConfig.Header = header
		a.Data.Configs = append(a.Data.Configs, config)
	}

	itemConfigSheet := a.sheets[7]
	if len(itemConfigSheet) < 1 {
		return fmt.Errorf("missing api config data")
	}
	for _, itemConfigRow := range itemConfigSheet[1:] {
		if len(itemConfigRow) != 5 {
			return fmt.Errorf("invalid api config item")
		}
		item := apistructs.AutoTestConfigItem{}
		item.Name = itemConfigRow[1]
		item.Value = itemConfigRow[3]
		item.Type = itemConfigRow[2]
		item.Desc = itemConfigRow[4]
		for _, config := range a.Data.Configs {
			if config.Ns == itemConfigRow[0] {
				config.APIConfig.Global[item.Name] = item
			}
		}
	}
	return nil
}

func (a *AutoTestSpaceExcel) GetSpaceData() *AutoTestSpaceData {
	return a.Data
}

// Import accept space import request and return uint64 file id,
// then send file id to channel make import sync
func (svc *Service) Import(req apistructs.AutoTestSpaceImportRequest, r *http.Request) (uint64, error) {
	if !req.FileType.Valid() {
		return 0, apierrors.ErrImportAutoTestSpace.InvalidParameter("fileType")
	}
	if req.ProjectID == 0 {
		return 0, apierrors.ErrImportAutoTestSpace.MissingParameter("projectID")
	}

	_, err := svc.bdl.GetProject(req.ProjectID)
	if err != nil {
		return 0, apierrors.ErrImportAutoTestSpace.InvalidParameter(fmt.Errorf("project not found, id: %d", req.ProjectID))
	}

	f, fileHeader, err := r.FormFile("file")
	if err != nil {
		return 0, err
	}
	defer f.Close()

	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: fileHeader.Filename,
		FileReader:      f,
		From:            "autotest-space",
		IsPublic:        true,
		ExpiredAt:       nil,
	}
	file, err := svc.bdl.UploadFile(uploadReq)
	if err != nil {
		return 0, err
	}

	fileReq := apistructs.TestFileRecordRequest{
		FileName:     fileHeader.Filename,
		Description:  fmt.Sprintf("ProjectID: %d", req.ProjectID),
		ProjectID:    req.ProjectID,
		Type:         apistructs.FileSpaceActionTypeImport,
		ApiFileUUID:  file.UUID,
		State:        apistructs.FileRecordStatePending,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			AutotestSpaceFileExtraInfo: &apistructs.AutoTestSpaceFileExtraInfo{
				ImportRequest: &req,
			},
		},
	}
	id, err := svc.CreateFileRecord(fileReq)
	if err != nil {
		return 0, err
	}
	return id, nil
}

func (svc *Service) ImportFile(record *dao.TestFileRecord) {
	extra := record.Extra.AutotestSpaceFileExtraInfo
	if extra == nil || extra.ImportRequest == nil {
		logrus.Errorf("autotest space import func missing request data")
		return
	}

	req := extra.ImportRequest
	id := record.ID
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
		return
	}

	f, err := svc.bdl.DownloadDiceFile(record.ApiFileUUID)
	if err != nil {
		logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
			logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
		}
		return
	}
	defer f.Close()

	switch req.FileType {
	case apistructs.TestSpaceFileTypeExcel:
		sheets, err := excel.Decode(f)
		if err != nil {
			logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
			if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
				logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
			}
			return
		}
		if len(sheets) != 8 {
			logrus.Error(apierrors.ErrImportAutoTestSpace.InvalidParameter("sheet"))
			if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
				logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
			}
			return
		}
		spaceExcelData := AutoTestSpaceExcel{
			sheets: sheets,
			Data: &AutoTestSpaceData{
				ProjectID:    req.ProjectID,
				IdentityInfo: req.IdentityInfo,
				svc:          svc,
			},
		}
		// make space data creator
		creator := AutoTestSpaceDirector{}
		creator.New(&spaceExcelData)
		if err := creator.Construct(); err != nil {
			logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
			if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
				logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
			}
			return
		}
		data := creator.Creator.GetSpaceData()
		_, err = data.Copy()
		if err != nil {
			logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
			if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail}); err != nil {
				logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
			}
			return
		}
	default:
	}
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateSuccess}); err != nil {
		logrus.Error(apierrors.ErrImportAutoTestSpace.InternalError(err))
	}
}

func (svc *Service) ImportSceneSet(req apistructs.AutoTestSceneSetImportRequest, r *http.Request) (uint64, error) {
	if !req.FileType.Valid() {
		return 0, apierrors.ErrImportAutotestSceneSet.InvalidParameter("fileType")
	}
	if req.SpaceID == 0 {
		return 0, apierrors.ErrImportAutotestSceneSet.MissingParameter("spaceID")
	}

	space, err := svc.bdl.GetTestSpace(req.SpaceID)
	if err != nil {
		return 0, apierrors.ErrExportAutoTestSceneSet.InvalidParameter(fmt.Errorf("autotest space not found, id: %d", req.SpaceID))
	}

	f, fileHeader, err := r.FormFile("file")
	if err != nil {
		return 0, err
	}
	defer f.Close()

	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: fileHeader.Filename,
		FileReader:      f,
		From:            "autotest-scene-set",
		IsPublic:        true,
		ExpiredAt:       nil,
	}
	file, err := svc.bdl.UploadFile(uploadReq)
	if err != nil {
		return 0, err
	}

	fileReq := apistructs.TestFileRecordRequest{
		FileName:     fileHeader.Filename,
		Description:  fmt.Sprintf("SpaceName: %s", space.Name),
		ProjectID:    uint64(space.ProjectID),
		Type:         apistructs.FileSceneSetActionTypeImport,
		ApiFileUUID:  file.UUID,
		SpaceID:      space.ID,
		State:        apistructs.FileRecordStatePending,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			AutotestSceneSetFileExtraInfo: &apistructs.AutoTestSceneSetFileExtraInfo{
				ImportRequest: &req,
			},
		},
	}
	id, err := svc.CreateFileRecord(fileReq)
	if err != nil {
		return 0, err
	}
	return id, nil
}

func (svc *Service) ImportSceneSetFile(record *dao.TestFileRecord) {
	extra := record.Extra.AutotestSceneSetFileExtraInfo
	if extra == nil || extra.ImportRequest == nil {
		logrus.Errorf("autotest scene set import missing request data")
		return
	}

	req := extra.ImportRequest
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
		return
	}
	space, err := svc.GetSpace(req.SpaceID)
	if err != nil {
		logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateFail, Description: fmt.Sprintf("%s, err: %v", record.Description, err)}); err != nil {
			logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
		}
		return
	}

	f, err := svc.bdl.DownloadDiceFile(record.ApiFileUUID)
	if err != nil {
		logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateFail, Description: fmt.Sprintf("%s, err: %v", record.Description, err)}); err != nil {
			logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
		}
		return
	}
	defer f.Close()

	switch req.FileType {
	case apistructs.TestSceneSetFileTypeExcel:
		baseSheets := make([][][]string, 1)
		sheets, err := excel.Decode(f)
		baseSheets = append(baseSheets, sheets...)
		if err != nil {
			logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
			if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateFail, Description: fmt.Sprintf("%s, err: %v", record.Description, err)}); err != nil {
				logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
			}
			return
		}
		if len(sheets) != 5 {
			logrus.Error(apierrors.ErrImportAutotestSceneSet.InvalidParameter("sheet"))
			if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateFail, Description: fmt.Sprintf("%s, invalid sheets length: %d", record.Description, len(sheets))}); err != nil {
				logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
			}
			return
		}
		spaceExcelData := AutoTestSpaceExcel{
			sheets: baseSheets,
			Data: &AutoTestSpaceData{
				ProjectID:    req.ProjectID,
				SpaceID:      req.SpaceID,
				IdentityInfo: req.IdentityInfo,
				svc:          svc,
				Space:        space,
				NewSpace:     space,
			},
		}
		creator := AutoTestSpaceDirector{}
		creator.New(&spaceExcelData)
		if err := creator.ConstructSceneSet(); err != nil {
			logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
			if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateFail, Description: fmt.Sprintf("%s, err file data: %v", record.Description, err)}); err != nil {
				logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
			}
			return
		}
		data := creator.Creator.GetSpaceData()
		err = data.CopyFromSceneSets()
		if err != nil {
			logrus.Error(apierrors.ErrExportAutoTestSceneSet.InternalError(err))
			if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateFail, Description: fmt.Sprintf("%s, import sceneset data err: %v", record.Description, err)}); err != nil {
				logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
			}
			return
		}
	default:

	}
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateSuccess}); err != nil {
		logrus.Error(apierrors.ErrImportAutotestSceneSet.InternalError(err))
	}
}
