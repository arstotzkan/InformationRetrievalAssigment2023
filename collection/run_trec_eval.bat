cd collection/
REM change dir into the dir where trec_eval exists

trec_eval qrels.txt top50queryResults.txt
echo Process Completed\nPress any button to exit...
timeout /t -1


