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
echo -e "pg xxx"
echo -e "\n"
cd $SOLR_INSTALL/example/
java -jar start.jar &
sleep 10 #give Solr time to start
