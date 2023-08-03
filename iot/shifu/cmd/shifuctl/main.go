package main

import (
	"github.com/edgenesis/shifu/pkg/shifuctl/cmd"
)

func main() {
	if err := cmd.Execute(); err != nil {
		panic(err)
	}
}
