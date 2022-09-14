package deployment

import (
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"gitlab.alibaba-inc.com/pe3/swcli/svc/appmanagerv1"
)

func initGetCommand() {
	getCommand.Flags().Int64("deploy-app-id", 0, "deploy app id")
	getCommand.Flags().BoolP("wait", "w", true, "wait until finished")
	getCommand.MarkFlagRequired("deploy-app-id")
	viper.BindPFlag("deployment.get.deploy-app-id", getCommand.Flags().Lookup("deploy-app-id"))
	viper.BindPFlag("deployment.get.wait", getCommand.Flags().Lookup("wait"))
}

var getCommand = &cobra.Command{
	Use: "get",
	Short: "get specified deployment",
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

		deployAppId := viper.GetInt64("deployment.get.deploy-app-id")
		wait := viper.GetBool("deployment.get.wait")
		response, err := server.GetDeployment(deployAppId, wait)
		if response != nil {
			lib.SwPrint(response.MustMarshal())
			if wait {
				deployStatus, err := response.GetString("deployStatus")
				if err != nil {
					log.Error().Err(err).Msgf("cannot get deployStatus from launch response")
					lib.Exit()
				}
				if deployStatus != "SUCCESS" && deployStatus != "RUNNING" {
					lib.Exit()
				}
			}
		} else {
			log.Error().Err(err).Msgf("get deployment failed")
			lib.Exit()
		}
	},
}
