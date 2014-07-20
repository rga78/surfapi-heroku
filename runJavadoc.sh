#!/bin/sh

if [ -z $2 ]; then
    echo "usage: $0 <mongo-uri> <mainClass>"
    echo "      <mainClass>: PostProcessorMain, ResetMongoMain"
    exit 1
fi

MONGOLAB_URI=$1
shift

echo "connecting to $MONGOLAB_URI..."

MAIN=$1
shift

echo "java -Xms1024m -Xmx4096m -cp 'target/classes;target/dependency/*' -DMONGOLAB_URI=$MONGOLAB_URI com.surfapi.javadoc.$MAIN $*"
java -Xms1024m -Xmx4096m -cp 'target/classes;target/dependency/*' -DMONGOLAB_URI=$MONGOLAB_URI com.surfapi.javadoc.$MAIN $*

