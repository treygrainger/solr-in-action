#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch14.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi

############ Helper Functions ############
waitOnSolrToStart(){
  timeoutInSeconds="60"
  timer="0" 
  while [ $timer -lt $timeoutInSeconds ] && [ $(curl -sL -w "%{http_code}" --connect-timeout 2 "http://localhost:8983/solr/" -o /dev/null) -ne "200" ]
  do 
    sleep 1
    timer=$[$timer+1]
  done
  if [ $timer == $timeoutInSeconds ]; then
    echo "There was a problem starting Solr. Exiting script."; exit 1
  fi
}

stopSolr(){
  for ID in `ps waux | grep java | grep [s]tart.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Stopped running Solr process: $ID"
  done
}

function absolutePath {
  (cd "${1%/*}" &>/dev/null && printf "%s/%s" "$(pwd)" "${1##*/}")
}

SOLR_IN_ACTION=$(absolutePath $1)
SOLR_INSTALL=$(absolutePath $2)

############ Chapter Examples ############
stopSolr
echo -e "----------------------------------------"
echo -e "CHAPTER 14"
echo -e "----------------------------------------"

echo -e "pg 455"
echo -e "\n"
cd $SOLR_INSTALL/example/
cp -r $SOLR_IN_ACTION/example-docs/ch14/cores/ solr/
cp $SOLR_IN_ACTION/solr-in-action.jar solr/lib/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"

echo -e "pg 467"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/field-per-language/update -jar post.jar ch14/documents/field-per-language.xml
echo -e "\n"

echo -e "pg 468"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.4
echo -e "\n"

echo -e "pg 469"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.5
echo -e "\n"

echo -e "pg 472"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -jar -Durl=http://localhost:8983/solr/english/update post.jar ch14/documents/english.xml
java -jar -Durl=http://localhost:8983/solr/spanish/update post.jar ch14/documents/spanish.xml
java -jar -Durl=http://localhost:8983/solr/french/update post.jar ch14/documents/french.xml
curl -Ssf "http://localhost:8983/solr/aggregator/select?shards=localhost:8983/solr/english,localhost:8983/solr/spanish,localhost:8983/solr/french&df=content&q=*:*"
echo -e "\n"

echo -e "pg 483"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/multi-language-field/update -jar post.jar ch14/documents/multi-language-field.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.9
echo -e "\n"

echo -e "pg 489"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/langid/update -jar post.jar ch14/documents/langid.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.12
echo -e "\n"

echo -e "pg 493"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/langid2/update -jar post.jar ch14/documents/langid.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.14
echo -e "\n"

echo -e "pg 497"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/multi-langid/update -jar post.jar ch14/documents/multi-langid.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.17
echo -e "\n"

echo -e "pg 498"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/multi-langid/update?mtf-langid.hidePrependedLangs=true -jar post.jar ch14/documents/multi-langid.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.18

echo "Stopping Solr"
stopSolr