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
	"fmt"
	"io"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/i18n"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
	"github.com/erda-project/erda/pkg/excel"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pexpr"
	"github.com/erda-project/erda/pkg/strutil"
)

type AutoTestSpaceData struct {
	SpaceID                  uint64
	ProjectID                uint64
	Locale                   string
	svc                      *Service
	IsCopy                   bool
	sceneSetIDAssociationMap map[uint64]uint64
	sceneIDAssociationMap    map[uint64]uint64
	stepIDAssociationMap     map[uint64]uint64

	Space     *apistructs.AutoTestSpace
	NewSpace  *apistructs.AutoTestSpace
	SceneSets map[uint64][]apistructs.SceneSet
	Scenes    map[uint64][]apistructs.AutoTestScene
	Steps     map[uint64][]apistructs.AutoTestSceneStep
	Configs   []apistructs.AutoTestGlobalConfig

	apistructs.IdentityInfo
}

// AutoTestSpaceDataCreator space data creator need achieve
// these methods
type AutoTestSpaceDataCreator interface {
	SetSpace() error
	SetSceneSets() error
	SetScenes() error
	SetSceneSteps() error
	SetConfigs() error
	SetSingleSceneSet(setID uint64) error
	GetSpaceData() *AutoTestSpaceData
}

type AutoTestSpaceDirector struct {
	Creator AutoTestSpaceDataCreator
}

func (a *AutoTestSpaceDirector) New(m AutoTestSpaceDataCreator) {
	a.Creator = m
}

// Construct define process how to make space data
func (a *AutoTestSpaceDirector) Construct() error {
	if err := a.Creator.SetSpace(); err != nil {
		return err
	}

	if err := a.Creator.SetSceneSets(); err != nil {
		return err
	}

	if err := a.Creator.SetScenes(); err != nil {
		return err
	}

	if err := a.Creator.SetSceneSteps(); err != nil {
		return err
	}

	if err := a.Creator.SetConfigs(); err != nil {
		return err
	}

	return nil
}

func (a *AutoTestSpaceDirector) ConstructSceneSet() error {
	if err := a.Creator.SetSceneSets(); err != nil {
		return err
	}
	if err := a.Creator.SetSceneSets(); err != nil {
		return err
	}
	if err := a.Creator.SetScenes(); err != nil {
		return err
	}
	if err := a.Creator.SetSceneSteps(); err != nil {
		return err
	}

	return nil
}

func (a *AutoTestSpaceDirector) ConstructFromSceneSet(setID uint64) error {
	if err := a.Creator.SetSpace(); err != nil {
		return err
	}
	if err := a.Creator.SetSingleSceneSet(setID); err != nil {
		return err
	}
	if err := a.Creator.SetScenes(); err != nil {
		return err
	}
	if err := a.Creator.SetSceneSteps(); err != nil {
		return err
	}

	return nil
}

func (a *AutoTestSpaceData) addSpaceToExcel(file *excel.XlsxFile) error {
	l := a.svc.bdl.GetLocale(a.Locale)
	title := []excel.Cell{
		excel.NewCell(l.Get(i18n.I18nKeySpaceNum)),
		excel.NewCell(l.Get(i18n.I18nKeySpaceName)),
		excel.NewCell(l.Get(i18n.I18nKeySpaceProjectNum)),
		excel.NewCell(l.Get(i18n.I18nKeySpaceDescription)),
	}

	allLines := [][]excel.Cell{title}
	spaceLine := []excel.Cell{
		excel.NewCell(strutil.String(a.Space.ID)),
		excel.NewCell(a.Space.Name),
		excel.NewCell(strutil.String(a.Space.ProjectID)),
		excel.NewCell(a.Space.Description),
	}
	allLines = append(allLines, spaceLine)
	sheetName := l.Get(i18n.I18nKeySpaceSheetName)

	return excel.AddSheetByCell(file, allLines, sheetName)
}

func (a *AutoTestSpaceData) addSceneSetToExcel(file *excel.XlsxFile) error {
	l := a.svc.bdl.GetLocale(a.Locale)
	title := []excel.Cell{
		excel.NewCell(l.Get(i18n.I18nKeySceneSetNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneSetName)),
		excel.NewCell(l.Get(i18n.I18nKeySceneSetSpaceNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneSetPreNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneSetDescription)),
	}
	allLines := [][]excel.Cell{title}
	for _, sceneSets := range a.SceneSets {
		sceneSetLines := [][]excel.Cell{}
		for _, sceneSet := range sceneSets {
			sceneSetLines = append(sceneSetLines, []excel.Cell{
				excel.NewCell(strutil.String(sceneSet.ID)),
				excel.NewCell(sceneSet.Name),
				excel.NewCell(strutil.String(sceneSet.SpaceID)),
				excel.NewCell(strutil.String(sceneSet.PreID)),
				excel.NewCell(sceneSet.Description),
			})
		}
		allLines = append(allLines, sceneSetLines...)
	}
	sheetName := l.Get(i18n.I18nKeySceneSetSheetName)

	return excel.AddSheetByCell(file, allLines, sheetName)
}

func (a *AutoTestSpaceData) addSceneToExcel(file *excel.XlsxFile) error {
	l := a.svc.bdl.GetLocale(a.Locale)

	sceneTitle := []excel.Cell{
		excel.NewCell(l.Get(i18n.I18nKeySceneNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneName)),
		excel.NewCell(l.Get(i18n.I18nKeySceneSceneSetNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneSpaceNum)),
		excel.NewCell(l.Get(i18n.I18nKeyScenePreNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneRefSetNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneDescription)),
		excel.NewCell(l.Get(i18n.I18nKeySceneGroupID)),
		excel.NewCell(l.Get(i18n.I18nKeyScenePolicy)),
	}

	inputTitle := []excel.Cell{
		excel.NewCell(l.Get(i18n.I18nKeySceneInputNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneInputName)),
		excel.NewCell(l.Get(i18n.I18nKeySceneInputValue)),
		excel.NewCell(l.Get(i18n.I18nKeySceneInputTemp)),
		excel.NewCell(l.Get(i18n.I18nKeySceneInputSceneNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneInputSpaceNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneInputDescription)),
	}

	outputTitle := []excel.Cell{
		excel.NewCell(l.Get(i18n.I18nKeySceneOutputNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneOutputName)),
		excel.NewCell(l.Get(i18n.I18nKeySceneOutputValue)),
		excel.NewCell(l.Get(i18n.I18nKeySceneOutputSceneNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneOutputSpaceNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneOutputDescription)),
	}

	allSceneLines := [][]excel.Cell{sceneTitle}
	allInputLines := [][]excel.Cell{inputTitle}
	allOutputLines := [][]excel.Cell{outputTitle}
	for _, scenes := range a.Scenes {
		sceneLines := [][]excel.Cell{}
		for _, scene := range scenes {
			sceneLines = append(sceneLines, []excel.Cell{
				excel.NewCell(strutil.String(scene.ID)),
				excel.NewCell(scene.Name),
				excel.NewCell(strutil.String(scene.SetID)),
				excel.NewCell(strutil.String(scene.SpaceID)),
				excel.NewCell(strutil.String(scene.PreID)),
				excel.NewCell(strutil.String(scene.RefSetID)),
				excel.NewCell(scene.Description),
				excel.NewCell(strutil.String(scene.GroupID)),
				excel.NewCell(scene.Policy.ToString()),
			})

			inputLines := [][]excel.Cell{}
			for _, input := range scene.Inputs {
				inputLines = append(inputLines, []excel.Cell{
					excel.NewCell(strutil.String(input.ID)),
					excel.NewCell(input.Name),
					excel.NewCell(input.Value),
					excel.NewCell(input.Temp),
					excel.NewCell(strutil.String(input.SceneID)),
					excel.NewCell(strutil.String(input.SpaceID)),
					excel.NewCell(input.Description),
				})
			}
			allInputLines = append(allInputLines, inputLines...)

			outputLines := [][]excel.Cell{}
			for _, output := range scene.Output {
				outputLines = append(outputLines, []excel.Cell{
					excel.NewCell(strutil.String(output.ID)),
					excel.NewCell(output.Name),
					excel.NewCell(output.Value),
					excel.NewCell(strutil.String(output.SceneID)),
					excel.NewCell(strutil.String(output.SpaceID)),
					excel.NewCell(output.Description),
				})
			}
			allOutputLines = append(allOutputLines, outputLines...)
		}
		allSceneLines = append(allSceneLines, sceneLines...)
	}
	sceneSheetName := l.Get(i18n.I18nKeySceneSheetName)
	inputSheetName := l.Get(i18n.I18nKeySceneInputSheetName)
	outputSheetName := l.Get(i18n.I18nKeySceneOutputSheetName)
	if err := excel.AddSheetByCell(file, allSceneLines, sceneSheetName); err != nil {
		return err
	}
	if err := excel.AddSheetByCell(file, allInputLines, inputSheetName); err != nil {
		return err
	}

	return excel.AddSheetByCell(file, allOutputLines, outputSheetName)
}

func (a *AutoTestSpaceData) addSceneStepToExcel(file *excel.XlsxFile) error {
	l := a.svc.bdl.GetLocale(a.Locale)
	title := []excel.Cell{
		excel.NewCell(l.Get(i18n.I18nKeySceneStepNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepName)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepValue)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepType)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepPreNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepSceneNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepSpaceNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepPreType)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepApiSpecNum)),
		excel.NewCell(l.Get(i18n.I18nKeySceneStepIsDisabled)),
	}

	allLines := [][]excel.Cell{title}
	for _, steps := range a.Steps {
		stepLines := [][]excel.Cell{}
		for _, step := range steps {
			stepLines = append(stepLines, []excel.Cell{
				excel.NewCell(strutil.String(step.ID)),
				excel.NewCell(step.Name),
				excel.NewCell(step.Value),
				excel.NewCell(step.Type.String()),
				excel.NewCell(strutil.String(step.PreID)),
				excel.NewCell(strutil.String(step.SceneID)),
				excel.NewCell(strutil.String(step.SpaceID)),
				excel.NewCell(strutil.String(step.PreType)),
				// set api spec id zero because api test markets are not exported
				excel.NewCell("0"),
				excel.NewCell(strutil.String(step.IsDisabled)),
			})
			for _, pv := range step.Children {
				stepLines = append(stepLines, []excel.Cell{
					excel.NewCell(strutil.String(pv.ID)),
					excel.NewCell(pv.Name),
					excel.NewCell(pv.Value),
					excel.NewCell(pv.Type.String()),
					excel.NewCell(strutil.String(pv.PreID)),
					excel.NewCell(strutil.String(pv.SceneID)),
					excel.NewCell(strutil.String(pv.SpaceID)),
					excel.NewCell(strutil.String(pv.PreType)),
					// set api spec id zero because api test markets are not exported
					excel.NewCell("0"),
					excel.NewCell(strutil.String(pv.IsDisabled)),
				})
			}
		}
		allLines = append(allLines, stepLines...)
	}
	sheetName := l.Get(i18n.I18nKeySceneStepSheetName)

	return excel.AddSheetByCell(file, allLines, sheetName)
}

func (a *AutoTestSpaceData) addConfigsToExcel(file *excel.XlsxFile) error {
	l := a.svc.bdl.GetLocale(a.Locale)
	configTitle := []excel.Cell{
		excel.NewCell(l.Get(i18n.I18nKeyConfigScopeName)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigScopeNum)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigNsName)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigDisplayName)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigDescription)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigDomain)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigHeader)),
	}
	globalConfigTitle := []excel.Cell{
		excel.NewCell(l.Get(i18n.I18nKeyConfigGlobalNsName)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigGlobalName)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigGlobalType)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigGlobalValue)),
		excel.NewCell(l.Get(i18n.I18nKeyConfigGlobalDescription)),
	}

	allConfigLines := [][]excel.Cell{configTitle}
	allGlobalConfigLines := [][]excel.Cell{globalConfigTitle}

	for _, config := range a.Configs {
		allConfigLines = append(allConfigLines, []excel.Cell{
			excel.NewCell(config.Scope),
			excel.NewCell(config.ScopeID),
			excel.NewCell(config.Ns),
			excel.NewCell(config.DisplayName),
			excel.NewCell(config.Desc),
			excel.NewCell(config.APIConfig.Domain),
			excel.NewCell(jsonparse.JsonOneLine(config.APIConfig.Header)),
		})
		gConfigLines := [][]excel.Cell{}
		for _, gConfig := range config.APIConfig.Global {
			gConfigLines = append(gConfigLines, []excel.Cell{
				excel.NewCell(config.Ns),
				excel.NewCell(gConfig.Name),
				excel.NewCell(gConfig.Type),
				excel.NewCell(gConfig.Value),
				excel.NewCell(gConfig.Desc),
			})
		}
		allGlobalConfigLines = append(allGlobalConfigLines, gConfigLines...)
	}

	configSheetName := l.Get(i18n.I18nKeyConfigSheetName)
	gConfigSheetName := l.Get(i18n.I18nKeyConfigGlobalSheetName)
	if err := excel.AddSheetByCell(file, allConfigLines, configSheetName); err != nil {
		return err
	}
	return excel.AddSheetByCell(file, allGlobalConfigLines, gConfigSheetName)
}

// ConvertToExcel export space`s data to excel
func (a *AutoTestSpaceData) ConvertToExcel(w io.Writer, fileName string) error {
	file := excel.NewXLSXFile()
	if err := a.addSpaceToExcel(file); err != nil {
		return err
	}
	if err := a.addSceneSetToExcel(file); err != nil {
		return err
	}
	if err := a.addSceneToExcel(file); err != nil {
		return err
	}
	if err := a.addSceneStepToExcel(file); err != nil {
		return err
	}
	if err := a.addConfigsToExcel(file); err != nil {
		return err
	}
	excel.WriteFile(w, file, fileName)
	return nil
}

func (a *AutoTestSpaceData) ConvertSceneSetToExcel(w io.Writer, fileName string) error {
	file := excel.NewXLSXFile()
	if err := a.addSceneSetToExcel(file); err != nil {
		return err
	}
	if err := a.addSceneToExcel(file); err != nil {
		return err
	}
	if err := a.addSceneStepToExcel(file); err != nil {
		return err
	}
	if err := excel.WriteFile(w, file, fileName); err != nil {
		return err
	}
	return nil
}

func (a *AutoTestSpaceData) copyPreCheck() error {
	var total int
	for sceneSetID := range a.Scenes {
		if len(a.Scenes[sceneSetID]) >= maxSize {
			return fmt.Errorf("一个场景集合下，限制500个测试场景")
		}
		total += len(a.Scenes[sceneSetID])
	}
	if total > spaceMaxSize {
		return fmt.Errorf("一个空间下，限制五万个场景")
	}
	return nil
}

func (a *AutoTestSpaceData) lockedSourceSpace() error {
	var err error
	if !a.Space.IsOpen() {
		return apierrors.ErrCopyAutoTestSpace.InternalError(fmt.Errorf("目标测试空间已锁定"))
	}
	a.Space.Status = apistructs.TestSpaceLocked
	a.Space, err = a.svc.UpdateAutoTestSpace(*a.Space, a.IdentityInfo.UserID)
	if err != nil {
		return apierrors.ErrCopyAutoTestSpace.InternalError(err)
	}
	return nil
}

func (a *AutoTestSpaceData) unlockSourceSpace() error {
	a.Space.Status = apistructs.TestSpaceOpen
	if _, err := a.svc.UpdateAutoTestSpace(*a.Space, a.UserID); err != nil {
		return err
	}
	return nil
}

func (a *AutoTestSpaceData) CreateNewSpace() error {
	var err error
	spaceName := a.Space.Name
	if a.IsCopy {
		spaceName, err = a.svc.GenerateSpaceName(spaceName, int64(a.ProjectID))
		if err != nil {
			return err
		}
	}
	space, err := a.svc.CreateSpace(apistructs.AutoTestSpaceCreateRequest{
		Name:         spaceName,
		ProjectID:    int64(a.ProjectID),
		Description:  a.Space.Description,
		IdentityInfo: a.IdentityInfo,
	})
	if err != nil {
		return err
	}
	a.NewSpace = space
	return nil
}

func (a *AutoTestSpaceData) Copy() (*apistructs.AutoTestSpace, error) {
	var err error
	if err = a.copyPreCheck(); err != nil {
		return nil, err
	}

	// lock source space if copy from db
	if a.IsCopy {
		if err := a.lockedSourceSpace(); err != nil {
			return nil, err
		}
	}
	if err = a.CreateNewSpace(); err != nil {
		return nil, err
	}

	a.NewSpace.Status = apistructs.TestSpaceCopying
	a.NewSpace, err = a.svc.UpdateAutoTestSpace(*a.NewSpace, a.UserID)
	if err != nil {
		return nil, err
	}

	defer func() {
		// unlock source space
		if a.IsCopy {
			if err := a.unlockSourceSpace(); err != nil {
				logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
				return
			}
		}
		if err != nil {
			a.NewSpace.Status = apistructs.TestSpaceFailed
			if _, err := a.svc.UpdateAutoTestSpace(*a.NewSpace, a.UserID); err != nil {
				logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
				return
			}
		}
	}()

	if err = a.CopySceneSets(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return a.NewSpace, err
	}
	if err = a.CopyScenes(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return a.NewSpace, err
	}
	if err = a.CopySceneSteps(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return a.NewSpace, err
	}
	if err = a.CopyInputs(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return a.NewSpace, err
	}
	if err = a.CopyOutputs(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return a.NewSpace, err
	}
	a.NewSpace.Status = apistructs.TestSpaceOpen
	if _, err = a.svc.UpdateAutoTestSpace(*a.NewSpace, a.UserID); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return a.NewSpace, err
	}

	return a.NewSpace, nil
}

func (a *AutoTestSpaceData) CopyFromSceneSets() error {
	if err := a.CopySceneSets(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return err
	}
	if err := a.CopyScenes(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return err
	}
	if err := a.CopySceneSteps(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return err
	}
	if err := a.CopyInputs(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return err
	}
	if err := a.CopyOutputs(); err != nil {
		logrus.Error(apierrors.ErrCopyAutoTestSpace.InternalError(err))
		return err
	}
	return nil
}

func (a *AutoTestSpaceData) CopySceneSets() error {
	var preID uint64 = 0
	a.sceneSetIDAssociationMap = map[uint64]uint64{}

	for _, each := range a.SceneSets[a.Space.ID] {
		name := a.generateSceneSetName(each.Name)
		newSetID, err := a.svc.CreateSceneSet(apistructs.SceneSetRequest{
			Name:         name,
			Description:  each.Description,
			PreID:        preID,
			IdentityInfo: a.IdentityInfo,
			ProjectId:    a.ProjectID,
			SpaceID:      a.NewSpace.ID,
		})
		if err != nil {
			return err
		}
		a.sceneSetIDAssociationMap[each.ID] = newSetID
		preID = newSetID
	}
	return nil
}

func (a *AutoTestSpaceData) generateSceneSetName(sceneSetName string) string {
	name := sceneSetName
	idx := 1
	for {
		if !a.svc.IsExistSceneSetName(a.NewSpace.ID, name) {
			return name
		}
		name = fmt.Sprintf("%s(%d)", sceneSetName, idx)
		idx++
	}
}

func (a *AutoTestSpaceData) CopyScenes() error {
	var err error
	a.sceneIDAssociationMap = map[uint64]uint64{}
	a.stepIDAssociationMap = map[uint64]uint64{}

	for _, sceneSet := range a.SceneSets[a.Space.ID] {
		var preID uint64
		for _, each := range a.Scenes[sceneSet.ID] {
			sceneName := each.Name
			if a.IsCopy {
				sceneName, err = a.svc.GenerateSceneName(sceneName, a.ProjectID)
				if err != nil {
					return err
				}
			}
			newScene := &dao.AutoTestScene{
				Name:        sceneName,
				Description: each.Description,
				SpaceID:     a.NewSpace.ID,
				SetID:       a.sceneSetIDAssociationMap[sceneSet.ID],
				PreID:       preID,
				CreatorID:   a.UserID,
				Status:      apistructs.DefaultSceneStatus,
				RefSetID:    a.sceneSetIDAssociationMap[each.RefSetID],
				Policy:      each.Policy,
			}
			if each.RefSetID != 0 && newScene.RefSetID == 0 {
				quoteSceneSet, err := a.svc.db.GetSceneSet(each.RefSetID)
				if err == nil && quoteSceneSet != nil && quoteSceneSet.SpaceID == a.NewSpace.ID {
					newScene.RefSetID = each.RefSetID
				}
			}
			if err = a.svc.db.Insert(newScene, preID); err != nil {
				return err
			}

			a.sceneIDAssociationMap[each.ID] = newScene.ID
			preID = newScene.ID
			// construct the group relation by source group id
			if each.GroupID != 0 {
				newScene.GroupID = a.sceneIDAssociationMap[each.GroupID]
				if err = a.svc.db.UpdateAutotestScene(newScene); err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func (a *AutoTestSpaceData) CopyInputs() error {
	var err error
	for _, scenes := range a.Scenes {
		for _, scene := range scenes {
			for _, oldInput := range scene.Inputs {
				oldInput.Value = replaceInputValue(oldInput.Value, a.sceneIDAssociationMap)
				newInput := &dao.AutoTestSceneInput{
					Name:        oldInput.Name,
					Value:       oldInput.Value,
					Temp:        oldInput.Temp,
					Description: oldInput.Description,
					SceneID:     a.sceneIDAssociationMap[scene.ID],
					SpaceID:     a.NewSpace.ID,
					CreatorID:   a.UserID,
				}
				if err = a.svc.db.CreateAutoTestSceneInput(newInput); err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func (a *AutoTestSpaceData) CopyOutputs() error {
	var err error
	for _, scenes := range a.Scenes {
		for _, scene := range scenes {
			for _, oldOutput := range scene.Output {
				oldOutput.Value = replacePreStepValue(oldOutput.Value, a.stepIDAssociationMap)
				newOutput := &dao.AutoTestSceneOutput{
					Name:        oldOutput.Name,
					Value:       oldOutput.Value,
					Description: oldOutput.Description,
					SceneID:     a.sceneIDAssociationMap[scene.ID],
					SpaceID:     a.NewSpace.ID,
					CreatorID:   a.UserID,
				}
				if err = a.svc.db.CreateAutoTestSceneOutput(newOutput); err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func (a *AutoTestSpaceData) CopySceneSteps() error {
	var err error
	a.stepIDAssociationMap = map[uint64]uint64{}

	for oldSceneID, steps := range a.Steps {
		var head uint64
		for _, each := range steps {
			each.Value = replacePreStepValue(each.Value, a.stepIDAssociationMap)

			newStep := &dao.AutoTestSceneStep{
				Type:       each.Type,
				Value:      each.Value,
				Name:       each.Name,
				PreID:      head,
				PreType:    each.PreType,
				SceneID:    a.sceneIDAssociationMap[oldSceneID],
				SpaceID:    a.NewSpace.ID,
				APISpecID:  each.APISpecID,
				IsDisabled: each.IsDisabled,
				CreatorID:  a.UserID,
			}
			if err = a.svc.db.CreateAutoTestSceneStep(newStep); err != nil {
				return err
			}
			a.stepIDAssociationMap[each.ID] = newStep.ID
			// replace value again because loop strategy may use self id
			tmpValue := replacePreStepValue(newStep.Value, a.stepIDAssociationMap)
			if tmpValue != newStep.Value {
				newStep.Value = tmpValue
				if err = a.svc.db.UpdateAutotestSceneStep(newStep); err != nil {
					return err
				}
			}

			head = newStep.ID
			pHead := newStep.ID

			for _, pv := range each.Children {
				pv.Value = replacePreStepValue(pv.Value, a.stepIDAssociationMap)

				newPStep := &dao.AutoTestSceneStep{
					Type:       pv.Type,
					Value:      pv.Value,
					Name:       pv.Name,
					PreID:      pHead,
					PreType:    pv.PreType,
					SceneID:    a.sceneIDAssociationMap[oldSceneID],
					SpaceID:    a.NewSpace.ID,
					APISpecID:  pv.APISpecID,
					IsDisabled: pv.IsDisabled,
					CreatorID:  a.UserID,
				}

				if err = a.svc.db.CreateAutoTestSceneStep(newPStep); err != nil {
					return err
				}
				pHead = newPStep.ID
				a.stepIDAssociationMap[pv.ID] = newPStep.ID
				// replace value again because loop strategy may use self id
				tmpValue = replacePreStepValue(newPStep.Value, a.stepIDAssociationMap)
				if tmpValue != newPStep.Value {
					newPStep.Value = tmpValue
					if err = a.svc.db.UpdateAutotestSceneStep(newPStep); err != nil {
						return err
					}
				}
			}
		}
	}
	return nil
}

func replaceInputValue(value string, sceneIDMap map[uint64]uint64) string {
	if len(sceneIDMap) <= 0 {
		return value
	}

	return strutil.ReplaceAllStringSubmatchFunc(pexpr.PhRe, value, func(subs []string) string {
		phData := subs[0]
		inner := subs[1]
		inner = strings.Trim(inner, " ")
		ss := strings.SplitN(inner, ".", 3)
		if len(ss) < 2 {
			return phData
		}

		switch ss[0] {
		case expression.Outputs:
			if len(ss) > 2 {
				preIdInt, err := strconv.Atoi(ss[1])
				if err == nil {
					value, ok := sceneIDMap[uint64(preIdInt)]
					if ok {
						phData = strings.Replace(subs[0], ss[1], strconv.Itoa(int(value)), 1)
					}
				} else {
					logrus.Errorf("atoi name error: %v", err)
				}
				tmp := strings.SplitN(ss[2], "_", 2)
				if len(tmp) < 2 {
					return phData
				}
				oldStepID, err := strconv.Atoi(tmp[0])
				if err != nil {
					return phData
				}
				newStepID, ok := sceneIDMap[uint64(oldStepID)]
				if ok {
					phData = strings.Replace(phData, tmp[0], strconv.Itoa(int(newStepID)), 1)
				}
			}

			return phData
		default:
			return phData
		}
	})
}
