if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch14.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 14"
echo -e "----------------------------------------\n"
echo -e "pg 455"
echo -e "\n"
cd $SOLR_INSTALL/example/
cp -r $SOLR_IN_ACTION/example-docs/ch14/cores/ solr/
cp $SOLR_IN_ACTION/solr-in-action.jar solr/lib/
java -jar start.jar &
sleep 10 #give Solr time to start
echo -e "\n"
echo -e "pg 467"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/field-per-language/update -jar post.jar ch14/documents/field-per-language.xml
echo -e "\n"
echo -e "pg 468"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.4
echo -e "\n"
echo -e "pg 469"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.5
echo -e "\n"
echo -e "pg 472"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -jar -Durl=http://localhost:8983/solr/english/update post.jar ch14/documents/english.xml
java -jar -Durl=http://localhost:8983/solr/spanish/update post.jar ch14/documents/spanish.xml
java -jar -Durl=http://localhost:8983/solr/french/update post.jar ch14/documents/french.xml
curl "http://localhost:8983/solr/aggregator/select?shards=localhost:8983/solr/english,localhost:8983/solr/spanish,localhost:8983/solr/french&df=content&q=*:*"
echo -e "\n"
echo -e "pg 483"
echo -e "\n"
Add document
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/multi-language-field/update -jar post.jar ch14/documents/multi-language-field.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.9
echo -e "\n"
echo -e "pg 489"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/langid/update -jar post.jar ch14/documents/langid.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.12
echo -e "\n"
echo -e "pg 493"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/langid2/update -jar post.jar ch14/documents/langid.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.14
echo -e "\n"
echo -e "pg 497"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/multi-langid/update -jar post.jar ch14/documents/multi-langid.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.17
echo -e "\n"
echo -e "pg 498"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/multi-langid/update?mtf-langid.hidePrependedLangs=true -jar post.jar ch14/documents/multi-langid.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 14.18
for ID in `ps waux | grep java | grep [s]tart.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Stopped Solr process: $ID"
done