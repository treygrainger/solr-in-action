#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch8.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 8"
echo -e "----------------------------------------"

echo -e "pg 257"
echo -e "\n"
cd $SOLR_INSTALL/example/
cp -r $SOLR_IN_ACTION/example-docs/ch8/cores/restaurants/ solr/restaurants/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/restaurants/update -Dtype=application/json -jar post.jar ch8/documents/restaurants.json
echo -e "\n"

echo -e "pg 258"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/restaurants/select?q=*:*"
echo -e "\n"

echo -e "pg 259"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.3
echo -e "\n"

echo -e "pg 260"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.4
echo -e "\n"

echo -e "pg 263"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.5
echo -e "\n"

echo -e "pg 264"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.6
echo -e "\n"

echo -e "pg 265"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.7
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.8
echo -e "\n"

echo -e "pg 267"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.9
echo -e "\n"

echo -e "pg 269"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.10
echo -e "\n"

echo -e "pg 270"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.11
echo -e "\n"

echo -e "pg 271"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.12
echo -e "\n"

echo -e "pg 272"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.13
echo -e "\n"

echo -e "pg 274"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.14
echo -e "\n"

echo -e "pg 275"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.15
echo -e "\n"

echo -e "pg 278"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.16

echo "Stopping Solr"
stopSolr