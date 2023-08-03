package add

import (
	"github.com/spf13/cobra"
)

var (
	ds string
)

var AddCmd = &cobra.Command{
	Use:   "add",
	Short: "add new modules for shifu",
	Args:  cobra.MinimumNArgs(1),
}

var deviceShifuCmd = &cobra.Command{
	Use:   "deviceshifu",
	Short: "Add a new deviceShifu",
	Run: func(cmd *cobra.Command, args []string) {
		addDeviceShifu(ds)
	},
}

func init() {
	AddCmd.AddCommand(deviceShifuCmd)

	deviceShifuCmd.PersistentFlags().StringVarP(&ds, "name", "n", "", "The name of deviceShifu you want to add. For example: shifuctl add deviceshifu --name deviceshifumqtt")

	if err := deviceShifuCmd.MarkPersistentFlagRequired("name"); err != nil {
		panic(err)
	}
}
