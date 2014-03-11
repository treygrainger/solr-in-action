#!/bin/bash
if [ "$#" -ne 2 ]; then
  echo -e "Usage: ch16.sh \$SOLR_IN_ACTION \$SOLR_INSTALL"
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
echo -e "CHAPTER 16"
echo -e "----------------------------------------"

echo -e "pg 551"
echo -e "\n"
cd $SOLR_INSTALL/example/
cp -r $SOLR_IN_ACTION/example-docs/ch16/cores/ solr/
echo -e "Starting Solr example server on port 8983; see $SOLR_INSTALL/example/solr.log for errors and log messages"
java -jar start.jar 1>solr.log 2>&1 &
waitOnSolrToStart
echo -e "..."
tail -10 solr.log
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/no-title-boost/update -jar post.jar ch16/documents/no-title-boost.xml
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.1
echo -e "\n"

echo -e "pg 554"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.2
echo -e "\n"

echo -e "pg 557"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/title-boost/update -jar post.jar ch16/documents/title-boost.xml
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.3
echo -e "\n"

echo -e "pg 558"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/no-title-boost/select?q=restaurant_name:(red%20lobster)^10%20OR%20description:(red%20lobster)"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/no-title-boost/select?defType=edismax&q=red%20lobster&qf=restaurant_name^10%20description"
echo -e "\n"

echo -e "pg 559"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/no-title-boost/select?q=restaurant_name:(red^2%20lobster^8)%20OR%20description:(red^2%20lobster^8)"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/no-title-boost/select?q=restaurant_name:(red^2%20lobster^8)^10%20OR%20description:(red^2%20lobster^8)"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/no-title-boost/select?defType=edismax&q=red^2%20lobster^8&qf=restaurant_name^10%20description"
echo -e "\n"

echo -e "pg 560"
echo -e "\n"
cd $SOLR_IN_ACTION/example-docs/
java -Durl=http://localhost:8983/solr/distance-relevancy/update -jar post.jar ch16/documents/distance-relevancy.xml
echo -e "\n"
java -Durl=http://localhost:8983/solr/news-relevancy/update -jar post.jar ch16/documents/news-relevancy.xml
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/distance-relevancy/select?q=restaurant_name:(Burger%20King)%20AND%20_query_:%22\{\!func\}recip(geodist(location,37.765,-122.43),1,10,1)%22"
echo -e "\n"

echo -e "pg 561"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/news-relevancy/select?fq=\{\!cache=false%20v=$keywords\}&q=_query_:%22\{\!func\}scale(query(\$keywords),0,100)%22%20AND%20_query_:%22\{\!func\}div(100,map(geodist(location,\$pt),0,1,1))%22%20AND%20_query_:%22\{\!func\}recip(rord(publicationDate),1,100,1)%22%20AND%20_query_:%22\{\!func\}scale(popularity,0,100)%22&keywords=%22street%20festival%22&pt=33.748,-84.391"
echo -e "\n"

echo -e "pg 567"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/no-title-boost/select?defType=edismax&q=red%20lobster&qf=restaurant_name%20description&fl=id,restaurant_name"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/no-title-boost/elevate?defType=edismax&q=red%20lobster&qf=restaurant_name%20description&fl=id,restaurant_name"
echo -e "\n"

echo -e "pg 571"
echo -e "\n"
java -Durl=http://localhost:8983/solr/jobs/update/csv -Dtype=text/csv -jar post.jar ch16/documents/jobs.csv
echo -e "\n"

echo -e "pg 572"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.5
echo -e "\n"

echo -e "pg 573"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.6
echo -e "\n"

echo -e "pg 575"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/jobs/mlt?df=jobdescription&q=J2EE&mlt.fl=jobtitle,jobdescription"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.7
echo -e "\n"

echo -e "pg 576"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.8
echo -e "\n"

echo -e "pg 578"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.9
echo -e "\n"

echo -e "pg 582"
echo -e "\n"
curl -Ssf "http://localhost:8983/solr/jobs/clustering?q=solr%20OR%20lucene&rows=100&carrot.title=jobtitle&carrot.snippet=jobtitle&LingoClusteringAlgorithm.desiredClusterCountBase=25&df=content"
echo -e "\n"

echo -e "pg 583"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.11
echo -e "\n"

echo -e "pg 584"
echo -e "\n"
java -jar $SOLR_IN_ACTION/solr-in-action.jar listing 16.12

echo "Stopping Solr"
stopSolr