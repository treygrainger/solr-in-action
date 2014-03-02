if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch6.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}
kill -9 $(ps aux | grep '[j]ava -jar start.jar' | awk '{print $2}') #stops Solr if running from previous chapter
sleep 2 #give process time to stop

echo -e "----------------------------------------\n"
echo -e "CHAPTER 6"
echo -e "----------------------------------------\n"
echo -e "\n\n"
echo -e "pg 167"
echo -e "\n"
cp $SOLR_IN_ACTION/example-docs/ch6/schema.xml $SOLR_INSTALL/example/solr/collection1/conf/
cp $SOLR_IN_ACTION/example-docs/ch6/wdfftypes.txt $SOLR_INSTALL/example/solr/collection1/conf/
echo -e "Updated schema.xml and wdfftypes.txt for chapter 6"
cd $SOLR_INSTALL/example/
java -jar start.jar &
sleep 10 #give Solr time to start
echo -e "\n\n"
echo -e "pg 186"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs
java -jar post.jar ch6/tweets.xml
