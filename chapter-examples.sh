CHAPTERS="1, 2, 3, 4, 5, 6, 7, 8, 9, 10, A, B, C"

if [ "$#" -ne 3  -a "$#" -ne 2 ]; then
  echo "Usage: chapter-examples.sh \$SOLR_IN_ACTION \$SOLR_INSTALL [\$CHAPTER_NUMBER]"
  echo "i.e. chapter-examples.sh ~/solr-in-action ~/solr 16"
  echo "if no \$CHAPTER_NUMBER is specified, all chapters will be executed"
  echo "Valid chapter selections include: $CHAPTERS"
  exit 0
fi

CHAPTER_SCRIPTS_FOLDER=scripts
SOLR_IN_ACTION=${1%/}
SOLR_INSTALL=${2%/}

if [ "$#" -ne 2 ]; then
  CHAPTER_NUMBER=$3

  case $CHAPTER_NUMBER in
    [1-16]) 
      CHAPTER_SCRIPT="$CHAPTER_SCRIPTS_FOLDER"/ch"$CHAPTER_NUMBER".sh
      ;;
    [Aa])
      CHAPTER_SCRIPT="$CHAPTER_SCRIPTS_FOLDER"/appendixA.sh
      ;;
    [Bb])
      CHAPTER_SCRIPT="$CHAPTER_SCRIPTS_FOLDER"/appendixB.sh
      ;;
    [Cc])
      CHAPTER_SCRIPT="$CHAPTER_SCRIPTS_FOLDER"/appendixC.sh
      ;;
  esac

  if [[ -x "$CHAPTER_SCRIPT" ]]; then
    echo "Executing: $CHAPTER_SCRIPT"
    ./"$CHAPTER_SCRIPT" $SOLR_IN_ACTION $SOLR_INSTALL
  else
    echo -e "Could not find chapter $CHAPTER_NUMBER."
  fi
  exit 0
else
  for CHAPTER in {1..16}
  do
    CHAPTER_SCRIPT=$CHAPTER_SCRIPTS_FOLDER/ch"$CHAPTER".sh
    echo -e "\n"
    echo $CHAPTER_SCRIPT
    if [[ -x "$CHAPTER_SCRIPT" ]]; then
      ./"$CHAPTER_SCRIPT" $SOLR_IN_ACTION $SOLR_INSTALL 2>&1
    fi
  done
  for CHAPTER in {A..C}
  do
    CHAPTER_SCRIPT=$CHAPTER_SCRIPTS_FOLDER/appendix"$CHAPTER".sh
    echo -e "\n"
    echo $CHAPTER_SCRIPT
    if [[ -x "$CHAPTER_SCRIPT" ]]; then
      ./"$CHAPTER_SCRIPT" $SOLR_IN_ACTION $SOLR_INSTALL 2>&1
    fi
  done
fi
