#!/bin/bash
  
# turn on bash's job control
set -m
  
vsftpd &
fg %1

while true; do
	sleep 100000
done
