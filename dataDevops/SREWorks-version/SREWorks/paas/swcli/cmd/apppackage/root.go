package apppackage

import (
	"github.com/spf13/cobra"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
)

var subCommand = &cobra.Command{
	Use:   "app-package",
	Short: "Manage app packages in appmanager server",
}

func Init(root *cobra.Command) {
	initBuildCommand()
	initGetLatestCommand()
	initRemoteSyncCommand()
	initOneflowCommand()
	initImportCommand()

	root.AddCommand(subCommand)
	subCommand.AddCommand(buildCommand)
	subCommand.AddCommand(getLatestCommand)
	subCommand.AddCommand(remoteSyncCommand)
	subCommand.AddCommand(oneflowCommand)
	subCommand.AddCommand(importCommand)

	subCommand.SetHelpFunc(func(command *cobra.Command, strings []string) {
		command.Println(command.UsageString())
		lib.Exit()
	})
}
