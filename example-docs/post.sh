#!/bin/sh

if [ -z $1 ]
then
  echo "Please provide at least one file to index."
  exit 1
fi

FILES=$*
URL=http://localhost:8983/solr/collection1/update
#URL=http://localhost:8983/solr/update

for f in $FILES; do
  echo Posting file $f to $URL
  curl $URL --data-binary @$f -H 'Content-type:application/xml' 
  echo
done

curl $URL --data-binary '<commit/>' -H 'Content-type:application/xml'
