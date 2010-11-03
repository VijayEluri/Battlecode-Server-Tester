#!/bin/bash
if [ -e bs-tester.tar.gz ]; then
  rm bs-tester.tar.gz
fi

cp -r bs-tester.jar README.txt scripts etc run.sh setup.sh bs-tester
tar -cvvf bs-tester.tar bs-tester
gzip bs-tester.tar
rm -rf bs-tester/*
