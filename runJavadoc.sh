#!/bin/sh

if [ -z $1 ]; then
    echo "usage: $0 <mainClass>"
    echo "      <mainClass>: JavadocMain, ExtractMain, PostProcessorMain, ResetMongoMain"
    exit 1
fi

MONGO_DBNAME=test

MAIN=$1
shift

echo "java -Xms1024m -Xmx4096m -cp 'target/classes;target/dependency/*' -Dcom.surfapi.mongo.db.name=$MONGO_DBNAME com.surfapi.javadoc.$MAIN $*"
java -Xms1024m -Xmx4096m -cp 'target/classes;target/dependency/*' -Dcom.surfapi.mongo.db.name=$MONGO_DBNAME com.surfapi.javadoc.$MAIN $*



