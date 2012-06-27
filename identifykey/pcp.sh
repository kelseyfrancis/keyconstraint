#!/bin/bash

if [ ! -e "target/pcp.jar" ]
then
  mvn install
fi

java -jar target/pcp.jar src/test/resources/440.wav
