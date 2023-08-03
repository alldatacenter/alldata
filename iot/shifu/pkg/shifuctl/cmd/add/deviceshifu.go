package add

import (
	"io/fs"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"github.com/edgenesis/shifu/pkg/shifuctl/cmd/utils"
)

const (
	DeviceShifuTemplate = "deviceshifutemplate"
)

// addcmd creates the new deviceShifu cmd source file from cmd/deviceshifu/cmdtemplate
func addCmd(ds, shifuRootDir string) {
	cmdDeviceShifuDir := filepath.Join(shifuRootDir, "cmd", "deviceshifu")
	cmdDeviceShifuTemplate := filepath.Join(cmdDeviceShifuDir, "cmdtemplate", "main.go")
	templateByteSlice, err := os.ReadFile(cmdDeviceShifuTemplate)
	if err != nil {
		panic(err)
	}

	templateStr := string(templateByteSlice)
	templateStr = strings.ReplaceAll(templateStr, DeviceShifuTemplate, ds)
	templateByteSlice = []byte(templateStr)

	cmdDeviceShifuGeneratedDir := filepath.Join(cmdDeviceShifuDir, ds)
	_, err = exec.Command("mkdir", cmdDeviceShifuGeneratedDir).Output()
	if err != nil {
		panic(err)
	}

	cmdDeviceShifuGeneratedSource := filepath.Join(cmdDeviceShifuGeneratedDir, "main.go")
	err = os.WriteFile(cmdDeviceShifuGeneratedSource, templateByteSlice, 0644)
	if err != nil {
		panic(err)
	}
}

func addPkg(ds, shifuRootDir string) {
	pkgDeviceShifuDir := filepath.Join(shifuRootDir, "pkg", "deviceshifu")
	pkgDeviceShifuTemplateDir := filepath.Join(pkgDeviceShifuDir, DeviceShifuTemplate)
	pkgDeviceShifuGeneratedDir := filepath.Join(pkgDeviceShifuDir, ds)
	_, err := exec.Command("mkdir", pkgDeviceShifuGeneratedDir).Output()
	if err != nil {
		panic(err)
	}

	err = filepath.Walk(pkgDeviceShifuTemplateDir, func(path string, info fs.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if info.IsDir() {
			return nil
		}

		templateByteSlice, err := os.ReadFile(path)
		if err != nil {
			return err
		}

		templateStr := string(templateByteSlice)
		templateStr = strings.ReplaceAll(templateStr, DeviceShifuTemplate, ds)
		templateByteSlice = []byte(templateStr)

		inputFileName := info.Name()
		outputFileName := strings.ReplaceAll(inputFileName, DeviceShifuTemplate, ds)
		outputFilePath := filepath.Join(pkgDeviceShifuGeneratedDir, outputFileName)

		err = os.WriteFile(outputFilePath, templateByteSlice, 0644)
		if err != nil {
			return err
		}

		return nil
	})
	if err != nil {
		panic(err)
	}
}

func addDeviceShifu(ds string) {
	shifuRootDir, err := utils.GetShifuRootDir()
	if err != nil {
		panic(err)
	}

	addCmd(ds, shifuRootDir)
	addPkg(ds, shifuRootDir)
}
