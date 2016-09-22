#!/bin/sh
#
# Run various surfapi utilities.
# 
# To rebuild an index from scratch:
#
# ./surfapi.sh $MONGOLAB_TEST buildIndex --index=ReferenceNameQuery
#
#

if [ -z $2 ]; then
    echo "usage: $0 <mongo-uri> [action] [options]"
    echo ""       
    echo "       for help: $0 help [action] "       
    exit 1
fi

MONGOLAB_URI=$1
shift

echo "connecting to $MONGOLAB_URI..."

MAIN=SurfapiUtilityMain

# cp_sep=";"   # windows
cp_sep=":"     # mac

echo "java -Xms1024m -Xmx4096m -cp "target/classes${cp_sep}target/dependency/'*'" -DMONGOLAB_URI=$MONGOLAB_URI com.surfapi.javadoc.$MAIN $*"
eval java -Xms1024m -Xmx4096m -cp "target/classes${cp_sep}target/dependency/'*'" -DMONGOLAB_URI=$MONGOLAB_URI com.surfapi.javadoc.$MAIN $*

