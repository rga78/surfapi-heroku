#!/bin/sh

if [ -z $1 ]; then
    echo "usage: $0 <mainClass>"
    echo "      <mainClass>: ServerMain, MongoMain"
    exit 1
fi

if [ -z $MONGOLAB_URI ]; then
    MONGOLAB_URI=mongodb://localhost/test
fi

echo connecting to MONGOLAB_URI=$MONGOLAB_URI

MAIN=$1
shift

echo "java -cp 'target/classes;target/dependency/*' -DMONGOLAB_URI=$MONGOLAB_URI com.surfapi.web.$MAIN $*"
start mintty -p 30,30 -e java -cp 'target/classes;target/dependency/*' -DMONGOLAB_URI=$MONGOLAB_URI com.surfapi.web.$MAIN $*

