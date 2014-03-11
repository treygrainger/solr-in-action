#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch6.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 6"
echo -e "----------------------------------------"

echo -e "pg 167"
echo -e "\n"
cp $SOLR_IN_ACTION/example-docs/ch6/schema.xml $SOLR_INSTALL/example/solr/collection1/conf/
cp $SOLR_IN_ACTION/example-docs/ch6/wdfftypes.txt $SOLR_INSTALL/example/solr/collection1/conf/
echo -e "Updated schema.xml and wdfftypes.txt for chapter 6"
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"

echo -e "pg 186"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs
java -jar post.jar ch6/tweets.xml

echo "Stopping Solr"
stopSolr