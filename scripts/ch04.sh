if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch4.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}
kill -9 $(ps aux | grep '[j]ava -jar start.jar' | awk '{print $2}') #stops Solr if running from previous chapter
sleep 2 #give process time to stop

echo -e "----------------------------------------\n"
echo -e "CHAPTER 4"
echo -e "----------------------------------------\n"
echo -e "\n\n"
echo -e "pg 85"
echo -e "\n"
cd $SOLR_INSTALL/example/
java -jar start.jar &
sleep 10 #give Solr time to start
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



