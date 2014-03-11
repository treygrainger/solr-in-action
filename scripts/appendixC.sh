#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: appendixC.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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

waitOnDataImportToFinish(){
  CORENAME=$1
  timeoutInSeconds="60"
  timer="0" 

  while [ $timer -lt $timeoutInSeconds ] && [ $(curl -sL --connect-timeout 2 "http://localhost:8983/solr/$CORENAME/dataimport?command=status" | grep "<str name=\"status\">idle</str>" | wc -l) -ne "1" ]
  do 
    sleep 1
    timer=$[$timer+1]
  done
  if [ $timer == $timeoutInSeconds ]; then
    echo "There was a problem with the data import. Exiting script."; exit 1
  fi
}

stopSolr(){
  for ID in `ps waux | grep java | grep [s]tart.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID > /dev/null
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
echo -e "Appendix C"
echo -e "----------------------------------------"

echo -e "pg 610"
echo -e "\n"
echo -e "Setting up 'solrpedia' core"
cd $SOLR_IN_ACTION/example-docs/
cp -r ch10/cores/ $SOLR_INSTALL/example/solr/
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"
cp $SOLR_IN_ACTION/example-docs/ch10/documents/solrpedia.xml $SOLR_INSTALL/example/
echo -e "Starting data import of solrpedia documents"
curl -Ssf 'http://localhost:8983/solr/solrpedia/dataimport' -H 'Origin: http://localhost:8983' -H 'Accept-Encoding: gzip,deflate,sdch' -H 'Accept-Language: en-US,en;q=0.8' -H 'Content-Type: application/x-www-form-urlencoded' -H 'Accept: application/json, text/javascript, */*; q=0.01' -H 'Referer: http://localhost:8983/solr/' -H 'X-Requested-With: XMLHttpRequest' -H 'Connection: keep-alive' --data 'command=full-import&verbose=true&clean=true&commit=true&wt=json&indent=true&optimize=false&debug=false' --compressed
waitOnDataImportToFinish solrpedia
echo -e "..."
tail -10 solr.log
echo -e "Finished importing solrpedia documents."
echo -e "\n"

stopSolr

echo -e "pg 614"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/appendixC
cp -r cores/ $SOLR_INSTALL/example/solr/
cp documents/solrexchange.xml $SOLR_INSTALL/example/
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"
echo -e "Starting data import of solrexchange documents"
curl -Ssf 'http://localhost:8983/solr/solrexchange/dataimport' -H 'Origin: http://localhost:8983' -H 'Accept-Encoding: gzip,deflate,sdch' -H 'Accept-Language: en-US,en;q=0.8' -H 'Content-Type: application/x-www-form-urlencoded' -H 'Accept: application/json, text/javascript, */*; q=0.01' -H 'Referer: http://localhost:8983/solr/' -H 'X-Requested-With: XMLHttpRequest' -H 'Connection: keep-alive' --data 'command=full-import&verbose=true&clean=true&commit=true&wt=json&indent=true&optimize=false&debug=false' --compressed
waitOnDataImportToFinish solrexchange
echo -e "..."
tail -10 solr.log
echo -e "Finished importing solrexchange documents."
echo -e "\n"

echo "Stopping Solr"
stopSolr