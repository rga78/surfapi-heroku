#!/bin/sh
#


if [ -z $3 ]; then
    echo "usage: $0 <javadoc-args>"
    echo "              <javadoc-args>:"
    echo "                      -sourcepath <sourcepath>"
    echo "                      -subpackages <package> -subpackages <package>..." 
    echo "                      <package> ..." 
    exit 1
fi

# build the doclet path
dp=target/classes
for x in `find target/dependency`; do dp="$dp;$x"; done

javadoc \
        -doclet com.surfapi.javadoc.JsonDoclet \
        -docletpath "$dp" \
        -J-Xms1024m \
        -J-Xmx4096m \
        $*
        





