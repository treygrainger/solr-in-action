#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch15.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
  exit 0
fi

############ Helper Functions ############
waitOnSolrToStart(){
  timeoutInSeconds="60"
  timer="0" 
  while [ $timer -lt $timeoutInSeconds ] && [ $(curl -sL -w "%{http_code}" --connect-timeout 2 "http://localhost:8983/solr/" -o /dev/null) -ne "200" ]
  do 
    sleep 1
    timer=$[$timer+1]
  done
  if [ $timer == $timeoutInSeconds ]; then
    echo "There was a problem starting Solr. Exiting script."; exit 1
  fi
}

stopSolr(){
  for ID in `ps waux | grep java | grep [s]tart.jar | awk '{print $2}' | sort -r`
  do
    kill -9 $ID
    echo "Stopped running Solr process: $ID"
  done
}

function absolutePath {
  (cd "${1%/*}" &>/dev/null && printf "%s/%s" "$(pwd)" "${1##*/}")
}

SOLR_IN_ACTION=$(absolutePath $1)
SOLR_INSTALL=$(absolutePath $2)

############ Chapter Examples ############
stopSolr
echo -e "----------------------------------------"
echo -e "CHAPTER 15"
echo -e "----------------------------------------"

echo -e "pg 504"
echo -e "\n"
cd $SOLR_INSTALL/example/
cp -r $SOLR_IN_ACTION/example-docs/ch15/cores/ solr/
cp $SOLR_IN_ACTION/solr-in-action.jar solr/lib/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/news/update -jar post.jar ch15/documents/news.xml
curl -Ssf "http://localhost:8983/solr/news/select?q=%22United%20States%22%20AND%20France%20AND%20President%20AND%20_val_%3A%22recip(ord(date)%2C1%2C100%2C100)%22"
echo -e "\n"

echo -e "pg 506"
echo -e "\n"
java -Durl=http://localhost:8983/solr/salestax/update -jar post.jar ch15/documents/salestax.xml
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/salestax/select?q=*%3A*&fq%3D%7B!frange%20l%3D10%20u%3D15%7Dproduct(basePrice%2C%20sum(1%2C%24userSalesTax))&userSalesTax=0.07"
echo -e "\n"

echo -e "pg 507"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/salestax/select?q=*:*&userSalesTax=0.07&fl=id,basePrice,product(basePrice,%20sum(1,%20%24userSalesTax))"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 15.1
echo -e "\n"

echo -e "pg 508"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/salestax/select?q=*:*&userSalesTax=0.07&sort=product(basePrice,%20sum(1,%20%24userSalesTax))%20asc,%20score%20desc"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/salestax/select?q=_query_:%22%7B!func%7Drecip(ord(date),1,100,100)%22&userSalesTax=0.07&totalPriceFunc=product(basePrice,sum(1,%24userSalesTax))&fq=%7B!frange%20l=10%20u=15%20v=%24totalPriceFunc%7D&fl=*,totalPrice:%24totalPriceFunc&sort=%24totalPriceFunc%20asc,score%20desc"
echo -e "\n"

echo -e "pg 520"
echo -e "\n"
java -Durl=http://localhost:8983/solr/customfunction/update -jar post.jar ch15/documents/customfunction.xml
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/customfunction/select?q=*:*&fl=introduction:concat(concat(field1,field2,%20%22,%20%22),%22!%22)"
echo -e "\n"

echo -e "pg 522"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/geospatial/update -jar post.jar ch15/documents/geospatial.xml
echo -e "\n"

echo -e "pg 523"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?q=*:*&fq=%7B!geofilt%20sfield=location%20pt=37.775,-122.419%20d=20%7D"
echo -e "\n"

echo -e "pg 524"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?q=*:*&fq=%7B!bbox%20sfield=location%20pt=37.775,-122.419%20d=20%7D"
echo -e "\n"

echo -e "pg 525"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 15.5
echo -e "\n"

echo -e "pg 526"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 15.6
echo -e "\n"

echo -e "pg 527"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?q=*:*&fq=%7B!geofilt%20sfield=location%20pt=37.775,-122.419%20d=20%7D&fl=*,distance:geodist(location,%2037.775,-122.419)&sort=geodist(location,%2037.775,-122.419)%20asc,%20score%20desc"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?q=*:*&fq=%7B!geofilt%7D&fl=*,distance:geodist()&sort=geodist()%20asc,score%20desc&sfield=location&pt=37.775,-122.419&d=20"
echo -e "\n"

echo -e "pg 532"
echo -e "\n"
stopSolr
cd $SOLR_INSTALL/example/webapps/
cp -r $SOLR_IN_ACTION/example-docs/ch15/jts/ ./
jar -uf solr.war WEB-INF/lib/jts.jar
rm -rf WEB-INF/
cd $SOLR_INSTALL/example/
cp solr/geospatial/conf/jts_schema.xml solr/geospatial/conf/schema.xml
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?q=*:*&fq=%7B!geofilt%20pt=37.775,-122.419%20sfield=location_rpt%20d=5%7D"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?q=*:*&fq=%7B!bbox%20pt=37.775,-122.419%20sfield=location_rpt%20d=5%7D"
echo -e "\n"

echo -e "pg 533"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?q=*:*&fq=location_rpt:%22Intersects(-90%20-90%2090%2090)%22"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?q=*:*&fl=id,location_rpt,city&fq=location_rpt:%22IsWithin(POLYGON((-85.4997%2034.7442,-84.9723%2030.6134,-81.2809%2030.5255,-80.9294%2032.0196,-83.3024%2034.8321,-85.4997%2034.7442)))%20distErrPct=0%22"
echo -e "\n"

echo -e "pg 535"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?sort=score%20asc&q=%7B!geofilt%20pt=37.775,-122.419%20sfield=location%20d=5%20score=distance%7D"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?sort=score%20asc&q=%7B!geofilt%20pt=37.775,-122.419%20sfield=location_rpt%20d=5%20score=distance%20filter=false%7D"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/geospatial/select?sort=%24distance%20asc&fl=id,distance:%24distance&q=*:*&distance=query(%24distFilter)&distFilter=%7B!geofilt%20pt=37.775,-122.419%20sfield=location_rpt%20d=5%20score=distance%20filter=true%7D"
echo -e "\n"

echo -e "pg 536"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -jar ../solr-in-action.jar ch15.DistanceFacetDocGenerator -file ch15/documents/distancefacet.xml
java -Durl=http://localhost:8983/solr/distancefacet/update -jar post.jar ch15/documents/distancefacet.xml
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 15.8
echo -e "\n"

echo -e "pg 538"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/pivotfaceting/update -jar post.jar ch15/documents/pivotfaceting.xml
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 15.9
echo -e "\n"

echo -e "pg 544"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/join_restaurants/update -jar post.jar ch15/documents/join_restaurants.xml
echo -e "\n"

echo -e "pg 545"
echo -e "\n"
java -Durl=http://localhost:8983/solr/join_useractions/update -jar post.jar ch15/documents/join_useractions.xml
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/join_restaurants/select?fl=restaurantname,text&q=%22Indian%22&fq=%7B!join%20fromIndex=join_useractions%20toIndex=join_restaurants%20from=restaurantid%20to=id%7Duserid:user123%20AND%20actiontype:clicked%20AND%20actiondate:%5BNOW-14DAYS%20TO%20*%5D"

echo "Stopping Solr"
stopSolr