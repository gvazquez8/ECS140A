all:
	mkdir -p class
	javac -sourcepath ./src/ -d ./class/ ./src/Scanner.java
	javac -sourcepath ./src/ -d ./class/ ./src/Token.java
	javac -sourcepath ./src/ -d ./class/ ./src/CSVParser.java
	javac -sourcepath ./src/ -d ./class/ ./src/PeekableCharacterStream.java
	javac -sourcepath ./src/ -d ./class/ ./src/PeekableCharacterFileStream.java
	javac -sourcepath ./src/ -d ./class/ ./src/NRDatalog.java
	javac -sourcepath ./src/ -d ./class/ ./src/NRDatalogDataLoader.java
	javac -sourcepath ./src/ -d ./class/ ./src/DataSet.java
	javac -sourcepath ./src/ -d ./class/ ./src/NRDatalogExecutionTree.java
	javac -sourcepath ./src/ -d ./class/ ./src/NRDatalogParser.java
	javac -sourcepath ./src/ -d ./class/ ./src/NRDatalogParseTree.java
	javac -sourcepath ./src/ -d ./class/ ./src/Node.java
	javac -sourcepath ./src/ -d ./class/ ./src/Operator.java
tar:
	tar -zcvf Project4Submission.tgz ./src ./README ./makefile ./queries
clean:
	rm -f Project4Submission.tgz
	rm -f -R ./class