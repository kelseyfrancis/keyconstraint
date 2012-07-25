#!/bin/bash

if [ ! -e "target/identifykey.jar" ]
then
  mvn install
fi

java -Xmx512M -jar target/identifykey.jar "$@"
