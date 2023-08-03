#!/bin/sh
set -e
/usr/sbin/sshd -De &
/bin/sleep 2
./http2ssh-stub