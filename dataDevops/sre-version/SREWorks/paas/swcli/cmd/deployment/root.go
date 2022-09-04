package deployment

import (
	"github.com/spf13/cobra"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
)

var subCommand = &cobra.Command{
	Use:   "deployment",
	Short: "Manage deployments in appmanager server",
}

func Init(root *cobra.Command) {
	initListCommand()
	initGetCommand()
	initLaunchCommand()
	initRemoteLaunchCommand()

	root.AddCommand(subCommand)
	subCommand.AddCommand(listCommand)
	subCommand.AddCommand(getCommand)
	subCommand.AddCommand(launchCommand)
	subCommand.AddCommand(remoteLaunchCommand)

	subCommand.SetHelpFunc(func(command *cobra.Command, strings []string) {
		command.Println(command.UsageString())
		lib.Exit()
	})
}
