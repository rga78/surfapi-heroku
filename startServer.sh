#!/bin/sh
#
# Start surfapi server.
#
# ServerMain: start the server, use raw json files as DB
# MongoMain: start the server, use mongo as DB
# 

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

# cp_sep=";"   # windows
cp_sep=":"     # mac


echo "java -cp "target/classes${cp_sep}target/dependency/'*'" com.surfapi.web.$MAIN $*"
eval java -cp "target/classes${cp_sep}target/dependency/'*'"  com.surfapi.web.$MAIN $*

# -rx- windows: start mintty -p 30,30 -e java -cp 'target/classes;target/dependency/*' com.surfapi.web.$MAIN $*

