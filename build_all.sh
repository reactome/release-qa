#! /bin/bash

BASE_DIR=$(pwd)

cd $BASE_DIR/QACommon
mvn clean install

cd $BASE_DIR/release-qa
mvn clean package

cd $BASE_DIR
echo -e "\n\nFinal jar:"
ls -lht release-qa-distribution/target/*.jar
