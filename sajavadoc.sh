#!/bin/sh

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

# run javadoc
./mongodoclet.sh $MONGOLAB_URI $MONGO_LIBRARYID $*

# run post-processor (for building indexes)
./runJavadoc.sh $MONGOLAB_URI PostProcessorMain buildIndex --libraryId=$MONGO_LIBRARYID






