#!/bin/bash
#
# Copyright 2017 IBM Corp. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the “License”);
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an “AS IS” BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# load configuration variables
source local.env

function usage() {
  echo "Usage: $0 [--install,--uninstall,--update,--recycle,--env]"
}

function install() {

  echo "Creating database..."
  # ignore "database already exists error"
  curl -s -X PUT $CLOUDANT_URL/users | grep -v file_exists
  curl -s -X PUT $CLOUDANT_URL/complaints | grep -v file_exists
  curl -s -X PUT $CLOUDANT_URL/moods | grep -v file_exists

  echo "Creating $PACKAGE_NAME package"
  bx wsk package create $PACKAGE_NAME\
    -p services.cloudant.url $CLOUDANT_URL\
    -p services.appid.url $APPID_URL\
    -p services.appid.clientId $APPID_CLIENTID\
    -p services.appid.secret $APPID_SECRET\
    -p services.ta.url $TONE_ANALYZER_URL\
    -p services.ta.username $TONE_ANALYZER_USERNAME\
    -p services.ta.password $TONE_ANALYZER_PASSWORD\

  echo "Creating actions"
  bx wsk action create $PACKAGE_NAME/validate-token \
    actions/validate/build/libs/validate.jar \
    --main serverless.followup.auth.Hello \
    --annotation final true

  bx wsk action create $PACKAGE_NAME/users-register \
    actions/users/build/libs/users.jar \
    --main serverless.followup.users.Hello \
    --annotation final true

  bx wsk action create $PACKAGE_NAME/complaints-put \
    actions/complaints/build/libs/complaints.jar \
    --main serverless.followup.complaints.Hello \
    --annotation final true
}

function uninstall() {
  echo "Removing actions..."
  bx wsk action delete $PACKAGE_NAME/validate-token
  bx wsk action delete $PACKAGE_NAME/users-register
  bx wsk action delete $PACKAGE_NAME/complaints-put

  echo "Removing package..."
  bx wsk package delete $PACKAGE_NAME

  echo "Done"
  bx wsk list
}

function update() {
  echo "Updating actions..."
  wsk action update $PACKAGE_NAME/validate-token    actions/validate/build/libs/validate.jar
  wsk action update $PACKAGE_NAME/users-register    actions/users/build/libs/users.jar
  wsk action update $PACKAGE_NAME/complaints-put    actions/complaints/build/libs/complaints.jar
}

function showenv() {
  echo "PACKAGE_NAME=$PACKAGE_NAME"
  echo "CLOUDANT_URL=$CLOUDANT_URL"
}

function recycle() {
  uninstall
  install
}

case "$1" in
"--install" )
install
;;
"--uninstall" )
uninstall
;;
"--update" )
update
;;
"--env" )
showenv
;;
"--recycle" )
recycle
;;
* )
usage
;;
esac
