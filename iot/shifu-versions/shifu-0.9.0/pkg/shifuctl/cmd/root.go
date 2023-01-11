package cmd

import (
	"github.com/spf13/cobra"

	"github.com/edgenesis/shifu/pkg/shifuctl/cmd/add"
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
}
