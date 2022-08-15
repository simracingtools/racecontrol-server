#!/bin/bash

git pull
mvn clean package -Dmaven.test.skip=true
src/deb/deploy.sh