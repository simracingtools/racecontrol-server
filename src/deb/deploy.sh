#!/bin/bash

DEPLOY_DIR=/opt/race-control
FILE_OWNERSHIP=racecontrol:racecontrol
TARGET_DIR=$(pwd)/target

if [ -d ./src/ ] && [ -d ./target/ ]; then
  echo "OK - running from project base directory"
else
  echo "$TARGET_DIR"
  echo "NOK - this script has to be run from project base directory"
  pwd
  exit 1
fi

if [ -d "$DEPLOY_DIR" ]; then
  echo "Deploy directory $DEPLOY_DIR exists"
else
  echo "Deploy directory $DEPLOY_DIR does not exist - try to create ..."
  sudo mkdir -p $DEPLOY_DIR
  sudo chown "$FILE_OWNERSHIP" $DEPLOY_DIR
  echo "ok"
fi

TEMPLATES_COPIED=false

if [ -f "$DEPLOY_DIR/application.properties" ]; then
  echo "$DEPLOY_DIR/application.properties exists"
else
  echo "Creating application.properties template in $DEPLOY_DIR"
  sudo cp src/main/resources/application.properties $DEPLOY_DIR/application.properties.template
  TEMPLATES_COPIED=true
fi

if [ -f "$DEPLOY_DIR/logback.xml" ]; then
  echo "$DEPLOY_DIR/logback.xml exists"
else
  echo "Creating logback.xml template in $DEPLOY_DIR"
  sudo cp src/deb/opt/race-control/logback.xml $DEPLOY_DIR/logback.xml.template
  TEMPLATES_COPIED=true
fi

if [ -f "$DEPLOY_DIR/env" ]; then
  echo "$DEPLOY_DIR/env exists"
else
  echo "Creating env in $DEPLOY_DIR"
  sudo cp env $DEPLOY_DIR/env.template
  TEMPLATES_COPIED=true
fi

if [ "$TEMPLATES_COPIED" == "true" ]; then
  echo "New installation, no service will be stopped"
else
  echo "Updating existing installation - stopping service"
  sudo service racecontrol stop
fi

SOURCE_FILE=$(ls target/racecontrol-server*.jar)
echo "Copy $SOURCE_FILE to $DEPLOY_DIR"
sudo cp "$SOURCE_FILE" $DEPLOY_DIR/racecontrol-server.jar
sudo chown "$FILE_OWNERSHIP" $DEPLOY_DIR/*

if [ "$TEMPLATES_COPIED" == "true" ]; then
  echo
  echo "Files deployed, templates created - modify to your needs before enabling the service"
else
  echo "Files deployed, try to restart service"
  sudo service racecontrol start
  sudo service racecontrol status
fi

echo
echo "Finished !"
