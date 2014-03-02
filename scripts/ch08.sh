if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch08.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}
for ID in `ps waux | grep java | grep [s]tart.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Killed previous Solr process: $ID"
done #stops Solr if running from previous chapter
sleep 2 #give processes time to die
echo -e "----------------------------------------\n"
echo -e "CHAPTER 8"
echo -e "----------------------------------------\n"
echo -e "\n"
echo -e "pg 257"
echo -e "\n"
cd $SOLR_INSTALL/example/
cp -r $SOLR_IN_ACTION/example-docs/ch8/cores/restaurants/ solr/restaurants/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
sleep 10 #give Solr time to start
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/restaurants/update -Dtype=application/json -jar post.jar ch8/documents/restaurants.json
echo -e "\n"
echo -e "pg 258"
echo -e "\n"
curl "http://localhost:8983/solr/restaurants/select?q=*:*"
echo -e "\n"
echo -e "pg 259"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.3
echo -e "\n"
echo -e "pg 260"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.4
echo -e "\n"
echo -e "pg 263"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.5
echo -e "\n"
echo -e "pg 264"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.6
echo -e "\n"
echo -e "pg 265"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.7
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.8
echo -e "\n"
echo -e "pg 267"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.9
echo -e "\n"
echo -e "pg 269"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.10
echo -e "\n"
echo -e "pg 270"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.11
echo -e "\n"
echo -e "pg 271"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.12
echo -e "\n"
echo -e "pg 272"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.13
echo -e "\n"
echo -e "pg 274"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.14
echo -e "\n"
echo -e "pg 275"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.15
echo -e "\n"
echo -e "pg 278"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 8.16