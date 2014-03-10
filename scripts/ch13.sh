if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch13.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}

for ID in `ps waux | grep java | grep [s]tart.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Stopped previous Solr process: $ID"
done #stops Solr if running from previous chapter

echo -e "----------------------------------------\n"
echo -e "CHAPTER 13"
echo -e "----------------------------------------\n"
echo -e "pg 407"
echo -e "\n"
cd $SOLR_INSTALL
cp -r example shard1
cd shard1
cp -r solr/collection1 solr/logmill
rm -rf solr/logmill/data
find . -name core.properties -exec rm {} \;
echo "name=logmill" > solr/logmill/core.properties
echo -e "Starting Solr on 8983 for hosting shard1 of the logmill collection; see $SOLR_INSTALL/shard1/shard1.log for errors and log messages"
java -Dcollection.configName=logmill -DzkRun -DnumShards=2 -Dbootstrap_confdir=./solr/logmill/conf -jar start.jar 1>shard1.log 2>&1 &
sleep 12
tail -30 $SOLR_INSTALL/shard1/shard1.log
echo -e "\n\n"
echo -e "pg 409"
echo -e "\n"
cd $SOLR_INSTALL/
cp -r shard1/ shard2/
cd shard2/
rm -rf solr/logmill/conf/
echo -e "Starting Solr on 8984 for hosting shard2 of the logmill collection; see $SOLR_INSTALL/shard2/shard2.log for errors and log messages"
java -DzkHost=localhost:9983 -Djetty.port=8984 -jar start.jar 1>shard2.log 2>&1 &
sleep 12 
tail -30 $SOLR_INSTALL/shard2/shard2.log
echo -e "\n\n"
echo -e "pg 410"
echo -e "\n"
echo -e "Indexing log messages into the logmill collection using CloudSolrServer"
cd $SOLR_IN_ACTION
java -jar solr-in-action.jar indexlog -log=example-docs/ch13/solr.log
echo -e "\n\n"
echo -e "pg 437"
echo -e "\n"
cd $SOLR_INSTALL/shard1/
cp -r solr/logmill/conf /tmp/support_conf
cd $SOLR_INSTALL/shard1/scripts/cloud-scripts
echo -e "Uploading configuration directory to ZooKeeper using Solr's zkcli utility"
./zkcli.sh -zkhost localhost:9983 -cmd upconfig -confdir /tmp/support_conf -confname support
echo -e "\n\n"
echo -e "pg 439"
echo -e "\n"
echo -e "Creating the support collection using the Collections API"
cd $SOLR_IN_ACTION
java -jar solr-in-action.jar listing 13.6
echo -e "\n\n"
echo -e "pg 440"
echo -e "\n"
echo -e "Creating the logmill-write alias using the Collections API"
java -jar solr-in-action.jar listing 13.7
for ID in `ps waux | grep java | grep [s]tart.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Stopped Solr process: $ID"
done

