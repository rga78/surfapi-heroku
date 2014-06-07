#!/bin/sh

if [ -z $1 ]; then
    echo "usage: $0 <mainClass>"
    echo "      <mainClass>: ServerMain, MongoMain"
    exit 1
fi

MONGO_DBNAME=test

MAIN=$1
shift

# echo "java -Xms1024m -Xmx4096m -cp 'target/classes;target/dependency/*' -Dcom.surfapi.mongo.db.name=$MONGO_DBNAME com.surfapi.web.$MAIN $*"
echo "java -cp 'target/classes;target/dependency/*' -Dcom.surfapi.mongo.db.name=$MONGO_DBNAME com.surfapi.web.$MAIN $*"
start mintty -p 30,30 -e java -cp 'target/classes;target/dependency/*' -Dcom.surfapi.mongo.db.name=$MONGO_DBNAME com.surfapi.web.$MAIN $*

