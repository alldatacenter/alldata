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

package auto_test_scenes

import (
	// leftPage
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/leftPage"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/leftPage/fileFormModal"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/leftPage/fileSearch"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/leftPage/fileTree"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/leftPage/leftHead/leftHeadAddSceneSet"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/leftPage/leftHead/leftHeadTitle"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/leftPage/leftHead/moreOperation"

	// fileConfig
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/fileInfo"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/fileInfoHead/fileInfoTitle"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/inParamsForm"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/inParamsTitle"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/outPutForm"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/outPutTitle"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stages"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/addApiButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/addConfigSheetButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/addCopyApiFormModal"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/addCustomScriptButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/addWaitButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/apiEditorDrawer"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/apiEditorDrawer/apiEditorContainer/apiEditor"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/apiEditorDrawer/apiEditorContainer/marketProto"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/configSheetDrawer/configSheetInParams"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/configSheetDrawer/configSheetSelect"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/customScriptDrawer/customScriptForm"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/nestedSceneDrawer/nestedSceneInParams"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/nestedSceneDrawer/nestedSceneSelect"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesOperations/waitEditorDrawer/waitEditor"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stagesTitle"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesSetConfig"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesSetConfig/scenesSetInfo"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesSetConfig/scenesSetTitle"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesSetConfig/scenesStages"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesSetConfig/scenesStagesOperations/addScenesButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesSetConfig/scenesStagesOperations/exportScenesButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesSetConfig/scenesStagesTitle"

	// fileExecute
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeAlertInfo"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeHead/cancelExecuteButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeHead/executeHistory"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeHead/executeHistory/executeHistoryButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeHead/executeHistory/executeHistoryPop"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeHead/executeHistory/executeHistoryPop/executeHistoryRefresh"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeHead/executeHistory/executeHistoryPop/executeHistoryTable"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeHead/refreshButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeInfo"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeTaskBreadcrumb"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeTaskTable"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileExecute/executeTaskTitle"

	// button
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesSetConfig/scenesStagesOperations/referSceneSetButton"
	_ "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/tabExecuteButton"
)
