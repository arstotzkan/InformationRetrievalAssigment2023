cd collection/
REM change dir into the dir where trec_eval exists

echo Comparing qrels.txt with top20queryResults.txt
trec_eval qrels.txt top20queryResults.txt

echo Comparing qrels.txt with top30queryResults.txt
trec_eval qrels.txt top30queryResults.txt

echo Comparing qrels.txt with top50queryResults.txt
trec_eval qrels.txt top50queryResults.txt
echo Process Completed\nPress any button to exit...
timeout /t -1


