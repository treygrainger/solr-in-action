#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch9.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 9"
echo -e "----------------------------------------"

echo -e "pg 284"
echo -e "\n"
# chapter 9 expects the user manually creating the ufo core,
# but this script will do that automatically to run the examples
if [ -d "$SOLR_INSTALL/example/solr/ufo" ]; then
  echo -e "Deleting pre-existing 'ufo' core to start fresh."
  rm -rf "$SOLR_INSTALL/example/solr/ufo" 
fi
cd $SOLR_INSTALL/example/solr
cp -r collection1 ufo
rm -rf ufo/data
rm ufo/core.properties
# enabling the core before starting Solr has same effect as the 
# instructions on pg 285 to create the core from the Solr Admin page
echo -e "Creating a new 'ufo' core automatically (instead of manually, as instructed in chapter 9)."
touch ufo/core.properties 
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -Xmx512m -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"

echo -e "pg 286"
echo -e "\n"
# Note: If you downloaded the ufo_awesome.json file to somewhere other 
# than the book's suggested location, you should modify the UFO_DATA_JSON 
# variable below to the location of the ufo_awesome.json file after 
# downloading/extracting as documented in chapter 9
UFO_DATA_JSON="/tmp/chimps_16154-2010-10-20_14-33-35/ufo_awesome.json"
if [ ! -e $UFO_DATA_JSON ]; then
  echo -e "UFO data $UFO_DATA_JSON file not found!"
  echo -e "Please download the UFO data set from http://www.infochimps.com/datasets/60000-documented-ufo-sightings-with-text-descriptions-and-metada."
  echo -e "After downloading, extract the zip to a temp directory such as $UFO_DATA_JSON."
  echo -e "If you download to a different location, you can update the UFO_DATA_JSON variable in this script to point to your copy of the ufo_awesome.json file."
  echo -e "\n"
  exit 1
fi
java -jar $SOLR_IN_ACTION/solr-in-action.jar ufo -jsonInput $UFO_DATA_JSON
echo -e "\n"

echo -e "pg 288"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 9.1 
echo -e "\n"

echo -e "pg 289"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 9.2 
echo -e "\n"

echo -e "pg 296"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 9.4 
echo -e "\n"

echo -e "pg 297"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 9.5 
echo -e "\n"

echo -e "pg 298"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 9.6 
echo -e "\n"

echo -e "pg 301"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 9.7

echo "Stopping Solr"
stopSolr