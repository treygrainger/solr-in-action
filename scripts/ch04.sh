if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch04.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 4"
echo -e "----------------------------------------\n"
echo -e "pg 85"
echo -e "\n"
cd $SOLR_INSTALL/example/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
sleep 10 
tail -30 solr.log
echo -e "\n\n"
echo -e "pg 91"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 4.4
echo -e "\n\n"
echo -e "pg 97"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 4.7
echo -e "\n\n"
echo -e "pg 100"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 4.8
echo -e "\n\n"
echo -e "pg 101"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 4.9
echo -e "\n\n"
echo -e "pg 109"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 4.12
for ID in `ps waux | grep java | grep [s]tart.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Stopped Solr process: $ID"
done