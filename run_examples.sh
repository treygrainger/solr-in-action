if [ "$#" -ne 3  -a "$#" -ne 2 ]; then
  echo "Usage: run.sh \$SOLR_IN_ACTION \$SOLR_INSTALL [$CHAPTER_NUMBER]"
  echo "i.e. run.sh ~/solr-in-action ~/solr 16"
  echo "if no $CHAPTER_NUMBER is specified, all chapters will be executed"
  exit 0
fi

SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}

if [ "$#" -ne 2 ]; then
  CHAPTER_NUMBER=$3
  CHAPTER_SCRIPT=scripts/ch"$CHAPTER_NUMBER".sh
  echo "$CHAPTER_SCRIPT"
  if [[ -x "$CHAPTER_SCRIPT" ]]; then
    $CHAPTER_SCRIPT $SOLR_IN_ACTION $SOLR_INSTALL
  else
    echo "Chapter $CHAPTER_NUMBER has no examples to run."
  fi
  exit 0
else
for CHAPTER_SCRIPT in scripts/*
  do
    if [[ -x "CHAPTER_SCRIPT" ]]; then
      $CHAPTER_SCRIPT $SOLR_IN_ACTION $SOLR_INSTALL
    fi
  done
fi
