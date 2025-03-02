package main

import (
	"flag"
	"fmt"
	"log"

	"github.com/gorcon/rcon"
)

func main() {
	addr := flag.String("addr", "localhost", "Your name")
	cmd := flag.String("cmd", "help", "Your name")
	port := flag.String("port", "25565", "Your age")
	pwd := flag.String("pwd", "1231", "Your age")

	// Parse the flags
	flag.Parse()

	conn, err := rcon.Dial(*addr +  ":" + *port, *pwd)
	
	if err != nil {
		log.Fatal(err)
	}
	defer conn.Close()

	response, err := conn.Execute(*cmd)
	if err != nil {
		log.Fatal(err)
	}
	
	fmt.Println(response)	
}