if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch10.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}

for ID in `ps waux | grep java | grep start.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Killed process $ID"
done

echo -e "----------------------------------------\n"
echo -e "CHAPTER 10"
echo -e "----------------------------------------\n"
echo -e "\n"
echo -e "pg 307"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
cp -r ch10/cores/ $SOLR_INSTALL/example/solr/
echo -e "\n"
echo -e "pg 308"
echo -e "\n"
cd $SOLR_
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
sleep 10 
tail -30 solr.log
cp $SOLR_IN_ACTION/example-docs/ch10/documents/solrpedia.xml $SOLR_INSTALL/example/
echo -e "\n"
echo -e "pg 309"
echo -e "\n"
curl 'http://localhost:8983/solr/solrpedia/dataimport' -H 'Origin: http://localhost:8983' -H 'Accept-Encoding: gzip,deflate,sdch' -H 'Accept-Language: en-US,en;q=0.8' -H 'Content-Type: application/x-www-form-urlencoded' -H 'Accept: application/json, text/javascript, */*; q=0.01' -H 'Referer: http://localhost:8983/solr/' -H 'X-Requested-With: XMLHttpRequest' -H 'Connection: keep-alive' --data 'command=full-import&verbose=true&clean=true&commit=true&wt=json&indent=true&entity=page&optimize=false&debug=false' --compressed
echo -e "Sleeping for 15 seconds to let the data import to complete."
sleep 15
tail -10 solr.log
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 10.1
echo -e "\n"
echo -e "pg 310"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 10.2
echo -e "\n"
echo -e "pg 311"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 10.3
echo -e "\n"
echo -e "pg 316"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 10.7
echo -e "\n"
echo -e "pg 317"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 10.8
echo -e "\n"
echo -e "pg 319"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 10.10
echo -e "\n"
echo -e "pg 326"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 10.15
echo -e "\n"
echo -e "pg 327"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 10.16
