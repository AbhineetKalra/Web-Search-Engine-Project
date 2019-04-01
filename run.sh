if [ "$1" = "-c" ]; then
	javac -cp \* src/edu/nyu/cs/cs2580/*.java
elif [ "$1" = "-i" ]; then
	java -cp src edu.nyu.cs.cs2580.SearchEngine --mode=index --options=conf/engine.conf 
elif [ "$1" = "-s" ]; then
	java -cp "src;stanford-corenlp-3.7.0.jar;stanford-corenlp-3.7.0-models.jar;json-simple-1.1.1.jar" -Xmx3048m edu.nyu.cs.cs2580.SearchEngine --mode=serve --port=25810 --options=conf/engine.conf
else
	echo "Please enter the correct argument. -c for compile, -i for index or -s for serving the search engine  "
fi
