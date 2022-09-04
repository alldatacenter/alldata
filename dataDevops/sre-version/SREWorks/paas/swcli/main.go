package main

import (
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"gitlab.alibaba-inc.com/pe3/swcli/cmd/apppackage"
	"gitlab.alibaba-inc.com/pe3/swcli/cmd/deployment"
	"gitlab.alibaba-inc.com/pe3/swcli/lib"
	"os"
	"time"
)

var (
	cfgFile string

	rootCmd = &cobra.Command{
		Use:   "swcli",
		Short: "SREWorks Command Line Interface",
	}
)

// Execute executes the root command.
func Execute() error {
	apppackage.Init(rootCmd)
	deployment.Init(rootCmd)
	rootCmd.SetHelpFunc(func(command *cobra.Command, strings []string) {
		command.Println(command.UsageString())
		lib.Exit()
	})
	return rootCmd.Execute()
}

func init() {
	cobra.OnInitialize(initConfig, initLog)

	rootCmd.PersistentFlags().StringVar(&cfgFile, "config", "", "config file (default is $HOME/.swcli.yaml)")
	rootCmd.PersistentFlags().StringP("endpoint", "", "", "endpoint for appmanager")
	rootCmd.PersistentFlags().StringP("client-id", "", "", "client id for appmanager")
	rootCmd.PersistentFlags().StringP("client-secret", "", "", "client secret for appmanager")
	rootCmd.PersistentFlags().StringP("username", "", "", "username for appmanager")
	rootCmd.PersistentFlags().StringP("password", "", "", "password for appmanager")
	rootCmd.PersistentFlags().BoolP("debug", "d", false, "debug mode (default is false)")
	rootCmd.PersistentFlags().BoolP("json", "j", false, "json mode (default is false)")
	rootCmd.PersistentFlags().BoolP("aone-pipeline", "", false, "in aone pipeline (default is false)")
	viper.BindPFlag("endpoint", rootCmd.PersistentFlags().Lookup("endpoint"))
	viper.BindPFlag("client-id", rootCmd.PersistentFlags().Lookup("client-id"))
	viper.BindPFlag("client-secret", rootCmd.PersistentFlags().Lookup("client-secret"))
	viper.BindPFlag("username", rootCmd.PersistentFlags().Lookup("username"))
	viper.BindPFlag("password", rootCmd.PersistentFlags().Lookup("password"))
	viper.BindPFlag("debug", rootCmd.PersistentFlags().Lookup("debug"))
	viper.BindPFlag("json", rootCmd.PersistentFlags().Lookup("json"))
	viper.BindPFlag("aone-pipeline", rootCmd.PersistentFlags().Lookup("aone-pipeline"))
}

func initConfig() {
	if cfgFile != "" {
		viper.SetConfigFile(cfgFile)
	} else {
		home, err := os.UserHomeDir()
		cobra.CheckErr(err)
		viper.AddConfigPath(home)
		viper.SetConfigType("yaml")
		viper.SetConfigName(".swcli")
	}
	viper.AutomaticEnv()
	viper.ReadInConfig()
}

func initLog() {
	zerolog.TimeFieldFormat = zerolog.TimeFormatUnix
	log.Logger = log.Output(zerolog.ConsoleWriter{
		TimeFormat: time.RFC3339,
		Out:        os.Stderr,
	})
	debug := viper.GetBool("debug")
	zerolog.SetGlobalLevel(zerolog.InfoLevel)
	if debug {
		zerolog.SetGlobalLevel(zerolog.DebugLevel)
	}
}

func main() {
	if err := Execute(); err != nil {
		lib.Exit()
	}
}
