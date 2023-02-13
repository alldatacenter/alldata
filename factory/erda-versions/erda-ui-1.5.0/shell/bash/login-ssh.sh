#!/usr/bin/expect

spawn ssh root@10.0.6.199
expect "*password*"
send "lDlqI3b6Ej778L8sFZI0JX1fjjBSmD\r"
expect "*root*"
send "kubectl edit dice dice\r"
interact
