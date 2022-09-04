package deployment

import (
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
)

func initListCommand() {
	listCommand.Flags().StringP("app-id", "", "", "deploy app id")
	listCommand.Flags().Int64P("page", "", 1, "page")
	listCommand.Flags().Int64P("page-size", "", 10, "page size")
	listCommand.Flags().BoolP("wait", "w", true, "wait until finished")
	viper.BindPFlag("deployment.list.app-id", listCommand.Flags().Lookup("app-id"))
	viper.BindPFlag("deployment.list.page", listCommand.Flags().Lookup("page"))
	viper.BindPFlag("deployment.list.page-size", listCommand.Flags().Lookup("page-size"))
}

var listCommand = &cobra.Command{
	Use: "list",
	Short: "list deployments",
	Run: func(cmd *cobra.Command, args []string) {
		endpoint := viper.GetString("endpoint")
		clientId := viper.GetString("client-id")
		clientSecret := viper.GetString("client-secret")
		username := viper.GetString("username")
		password := viper.GetString("password")
		server := appmanagerv1.NewAppManagerServer(
			lib.NewClient(endpoint, clientId, clientSecret, username, password),
			endpoint,
		)

		appId := viper.GetString("deployment.list.app-id")
		page := viper.GetInt64("deployment.list.page")
		pageSize := viper.GetInt64("deployment.list.page-size")
		response, err := server.ListDeployment(appId, page, pageSize)
		if response != nil {
			lib.SwPrint(response.MustMarshal())
		} else {
			log.Error().Err(err).Msgf("list deployment failed")
			lib.Exit()
		}
	},
}
