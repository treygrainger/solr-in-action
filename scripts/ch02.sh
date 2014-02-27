if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch2.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}
kill -9 $(ps aux | grep '[j]ava -jar start.jar' | awk '{print $2}') #stops Solr if running from previous chapter
sleep 2 #give process time to stop

echo -e "----------------------------------------\n"
echo -e "CHAPTER 2"
echo -e "----------------------------------------\n"
echo -e "\n\n"
echo -e "pg 27"
echo -e "\n"
java -version
echo -e "\n\n"
echo -e "pg 28"
echo -e "\n"
cd $SOLR_INSTALL/example/
java -jar start.jar &
sleep 10 #give Solr time to start
echo -e "\n\n"
echo -e "pg 33"
echo -e "\n"
cd $SOLR_INSTALL/example/exampledocs
java -jar post.jar *.xml
echo -e "\n\n"
echo -e "pg 37"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 2.1
echo -e "\n\n"
echo -e "pg 45"
echo -e "\n"
echo "Stopping Solr"
kill -9 $(ps aux | grep '[j]ava -jar start.jar' | awk '{print $2}') #stops Solr if running from previous chapter
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
echo -e "\n\n"
echo -e "pg 46"
echo -e "\n"
echo "Starting Solr"
cd $SOLR_INSTALL/realestate
java -jar start.jar &
sleep 10 #give Solr time to start
echo "Stopping Solr"
kill -9 $(ps aux | grep '[j]ava -jar start.jar' | awk '{print $2}') #stops Solr if running from previous chapter