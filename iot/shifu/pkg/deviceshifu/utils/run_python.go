package utils

import (
	"fmt"
	"os/exec"

	"github.com/edgenesis/shifu/pkg/logger"
)

const (
	PYTHON = "python"
	CMDARG = "-c"
)

func ProcessInstruction(moduleName string, funcName string, rawData string, scriptDir string) string {
	cmdString := fmt.Sprintf("import %s; print(%s.%s(%s))", moduleName, moduleName, funcName, rawData)
	cmd := exec.Command(PYTHON, CMDARG, cmdString)
	cmd.Dir = scriptDir
	processed, err := cmd.CombinedOutput()
	if err != nil {
		logger.Errorf("Failed process command %v\n, error:%v", cmdString, err.Error())
	}
	return string(processed)
}
