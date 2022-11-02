package lib

import (
	"fmt"
	"github.com/spf13/viper"
	"github.com/tidwall/pretty"
	"os"
)

func SwPrint(content []byte) {
	if viper.GetBool("json") {
		fmt.Fprint(os.Stdout, string(content))
	} else {
		fmt.Fprint(os.Stdout, string(pretty.Color(pretty.Pretty(content), pretty.TerminalStyle)))
	}
}

func Exit() {
	if viper.GetBool("aone-pipeline") {
		fmt.Fprint(os.Stdout, "BUILD-ERROR\n")
	}
	os.Exit(1)
}