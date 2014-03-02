if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch5.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 5"
echo -e "----------------------------------------\n"
echo -e "\n\n"
echo -e "pg 142"
echo -e "\n"
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
sleep 10 
tail -30 solr.log
cd $SOLR_IN_ACTION/example-docs
java -jar post.jar ch5/tweets.xml
echo -e "\n\n"
echo -e "pg 143"
echo -e "\n"
java -Dtype=application/json -jar post.jar ch5/tweets.json
echo -e "\n\n"
echo -e "pg 143"
echo -e "\n"
echo -e "Running the ExampleSolrJClient from Listing 5.15"
cd $SOLR_IN_ACTION
java -jar solr-in-action.jar ExampleSolrJClient
