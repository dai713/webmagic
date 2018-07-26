#!/bin/bash

source ./config.sh

nohup java -jar -Djava.net.preferIPv4Stack=true -Dspring.profiles.active=prod -Dserver.port=${port} ${jarName} &
