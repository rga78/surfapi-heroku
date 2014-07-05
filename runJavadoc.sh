#!/bin/sh

if [ -z $1 ]; then
    echo "usage: $0 <mainClass>"
    echo "      <mainClass>: PostProcessorMain, ResetMongoMain"
    exit 1
fi

if [ -z $MONGOLAB_URI ]; then
    MONGOLAB_URI=mongodb://localhost/test
fi

echo "connecting to $MONGOLAB_URI..."

MAIN=$1
shift

echo "java -Xms1024m -Xmx4096m -cp 'target/classes;target/dependency/*' -DMONGOLAB_URI=$MONGOLAB_URI com.surfapi.javadoc.$MAIN $*"
java -Xms1024m -Xmx4096m -cp 'target/classes;target/dependency/*' -DMONGOLAB_URI=$MONGOLAB_URI com.surfapi.javadoc.$MAIN $*

