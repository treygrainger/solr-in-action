if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch2.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}

for ID in `ps waux | grep java | grep start.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Killed process $ID"
    sleep 2
done

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
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
sleep 10 
tail -30 solr.log
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
kill -9 $(ps aux | grep '[j]ava -jar start.jar' | awk '{print $2}')
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
cd $SOLR_INSTALL/realestate
echo -e "Starting Solr realestate server"
java -jar start.jar 1>realestate.log 2>&1 &
sleep 10
tail -30 realestate.log
echo "Stopping Solr"
for ID in `ps waux | grep java | grep start.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Killed process $ID"
done

