package main

import (
	"fmt"
	"net/http"
	"os"
)

func getHumidity(w http.ResponseWriter, req *http.Request) {
	dat, _ := os.ReadFile("raw_data")
	fmt.Print(string(dat))
	fmt.Fprintln(w, string(dat))
}

func main() {
	http.HandleFunc("/humidity", getHumidity)

	http.ListenAndServe(":11111", nil)
}
