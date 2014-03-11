#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch2.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 2"
echo -e "----------------------------------------"

echo -e "pg 27"
echo -e "\n"
java -version 2>&1
echo -e "\n"

echo -e "pg 28"
echo -e "\n"
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"

echo -e "pg 33"
echo -e "\n"
cd $SOLR_INSTALL/example/exampledocs
java -jar post.jar *.xml
echo -e "\n"

echo -e "pg 37"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 2.1
echo -e "\n"

echo -e "pg 45"
echo -e "\n"
echo "Stopping Solr"
stopSolr
echo "Copying example folder to a new application called \"realestate\""
cd $SOLR_INSTALL/
rm -rf realestate
cp -R example realestate
cd realestate/
rm -rf example-DIH/
rm -rf multicore/
rm -rf example-schemaless/
cd solr/
echo "Creating a realestate core in place of the collection1 core"
rm -rf realestate
mv collection1 realestate
echo "name=realestate" > realestate/core.properties
echo -e "\n"

echo -e "pg 46"
echo -e "\n"
cd $SOLR_INSTALL/realestate
echo -e "Starting Solr realestate server"
java -jar start.jar 1>realestate.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 realestate.log

echo "Stopping Solr"
stopSolr