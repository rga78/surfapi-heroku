#!/bin/sh
#
# Upload javadoc to mongodb.
# Build indexes for new javadoc.
#

if [ -z $3 ]; then
    echo "usage: $0 <mongo-uri> <libraryid> <javadoc-args>"
    echo "              <javadoc-args>:"
    echo "                      -sourcepath <sourcepath>"
    echo "                      -subpackages <package> -subpackages <package>..." 
    echo "                      <package> ..." 
    exit 1
fi

MONGOLAB_URI=$1
shift

MONGO_LIBRARYID=$1
shift

#
# run javadoc
#
./mongodoclet.sh $MONGOLAB_URI $MONGO_LIBRARYID $*

#
# add library to indexes
#
./surfapi.sh $MONGOLAB_URI buildIndex --libraryId=$MONGO_LIBRARYID






