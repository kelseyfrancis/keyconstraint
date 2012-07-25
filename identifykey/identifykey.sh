#!/bin/bash

if [ ! -e "target/identifykey.jar" ]
then
  mvn install
fi

java -jar target/identifykey.jar "$@"
