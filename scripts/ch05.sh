if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch5.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}
kill -9 $(ps aux | grep '[j]ava -jar start.jar' | awk '{print $2}') #stops Solr if running from previous chapter
sleep 2 #give process time to stop

echo -e "----------------------------------------\n"
echo -e "CHAPTER 5"
echo -e "----------------------------------------\n"
echo -e "\n\n"
echo -e "pg 142"
echo -e "\n"
cd $SOLR_INSTALL/example/
java -jar start.jar &
sleep 10 #give Solr time to start
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
