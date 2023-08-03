package cmd

import (
	"github.com/edgenesis/shifu/pkg/shifuctl/cmd/add"
	"github.com/edgenesis/shifu/pkg/shifuctl/cmd/install"
	"github.com/spf13/cobra"
)

var (
	// Used for flags.
	rootCmd = &cobra.Command{
		Use:   "shifuctl",
		Short: "Command line tool for Shifu",
	}
)

// Execute executes the root command.
func Execute() error {
	return rootCmd.Execute()
}

func init() {
	rootCmd.AddCommand(versionCmd)
	rootCmd.AddCommand(add.AddCmd)
	rootCmd.AddCommand(install.InstallCmd)
}
