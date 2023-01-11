package utils

import (
	"fmt"
	"os"
)

func GetShifuRootDir() (string, error) {
	shifuRootDir := os.Getenv("SHIFU_ROOT_DIR")
	if shifuRootDir == "" {
		errStr := "Please set SHIFU_ROOT_DIR environment to Shifu source code root directory." +
			"For example: export SHIFU_ROOT_DIR=~/go/src/github.com/edgenesis/shifu/"
		fmt.Println(errStr)
		return "", fmt.Errorf("Invalid SHIFU_ROOT_DIR")
	}

	return shifuRootDir, nil
}
