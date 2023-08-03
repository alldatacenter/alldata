package cmd

import (
	"fmt"
	"os"
	"path"

	"github.com/edgenesis/shifu/pkg/shifuctl/cmd/utils"
	"github.com/spf13/cobra"
)

const (
	VersionFile = "version.txt"
)

var versionCmd = &cobra.Command{
	Use:   "version",
	Short: "Print the version shifuctl",
	Run: func(cmd *cobra.Command, args []string) {
		printVersion()
	},
}

func printVersion() {
	shifuRootDir, err := utils.GetShifuRootDir()
	if err != nil {
		panic(err)
	}

	versionPath := path.Join(shifuRootDir, VersionFile)
	versionByteSlice, err := os.ReadFile(versionPath)
	if err != nil {
		panic(err)
	}

	versionStr := string(versionByteSlice)

	fmt.Println("shifuctl version: " + versionStr)
}
