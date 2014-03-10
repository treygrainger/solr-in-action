if [ "$#" -ne 3  -a "$#" -ne 2 ]; then
  echo "Usage: chapter-examples.sh \$SOLR_IN_ACTION \$SOLR_INSTALL [\$CHAPTER_NUMBER]"
  echo "i.e. chapter-examples.sh ~/solr-in-action ~/solr 16"
  echo "if no \$CHAPTER_NUMBER is specified, all chapters will be executed"
  exit 0
fi

CHAPTER_SCRIPTS_FOLDER=scripts
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}

if [ "$#" -ne 2 ]; then
  CHAPTER_NUMBER=$3
  CHAPTER_SCRIPT="$CHAPTER_SCRIPTS_FOLDER"/ch"$CHAPTER_NUMBER".sh
  CHAPTER_SCRIPT_WITH_ZERO="$CHAPTER_SCRIPTS_FOLDER"/ch0"$CHAPTER_NUMBER".sh
  if [[ -x "$CHAPTER_SCRIPT" ]]; then
    echo "Executing: $CHAPTER_SCRIPT"
    ./"$CHAPTER_SCRIPT" $SOLR_IN_ACTION $SOLR_INSTALL
  elif [[ -x "$CHAPTER_SCRIPT_WITH_ZERO" ]]; then
    echo "Executing: $CHAPTER_SCRIPT"
    ./"$CHAPTER_SCRIPT_WITH_ZERO" $SOLR_IN_ACTION $SOLR_INSTALL
  else
    echo "Could not find chapter $CHAPTER_NUMBER."
  fi
  exit 0
else
  for CHAPTER_SCRIPT in "$CHAPTER_SCRIPTS_FOLDER"/*
  do
    echo $CHAPTER_SCRIPT
    if [[ -x "$CHAPTER_SCRIPT" ]]; then
      $CHAPTER_SCRIPT $SOLR_IN_ACTION $SOLR_INSTALL
    fi
  done
fi
