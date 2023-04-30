cd collection/
REM change dir into the dir where trec_eval exists

echo first 20 texts
trec_eval qrels.txt top20queryResults.txt

echo first 30 texts
trec_eval qrels.txt top30queryResults.txt

echo first 50 texts
trec_eval qrels.txt top50queryResults.txt
echo Process Completed\nPress any button to exit...
timeout /t -1


