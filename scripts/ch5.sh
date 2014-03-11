#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch5.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 5"
echo -e "----------------------------------------"
rm -rf $SOLR_INSTALL/example/solr/collection1/data #clear docs from other chapters
echo -e "pg 142"
echo -e "\n"
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs
java -jar post.jar ch5/tweets.xml
echo -e "\n"

echo -e "pg 143"
echo -e "\n"
java -Dtype=application/json -jar post.jar ch5/tweets.json
echo -e "\n"
echo -e "Running the ExampleSolrJClient from Listing 5.15"
cd $SOLR_IN_ACTION
java -jar solr-in-action.jar ExampleSolrJClient

echo "Stopping Solr"
stopSolr