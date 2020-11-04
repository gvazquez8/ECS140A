all:
	mkdir -p class
	javac -sourcepath ./src/ -d ./class/ ./src/Scanner.java
	javac -sourcepath ./src/ -d ./class/ ./src/Token.java
	javac -sourcepath ./src/ -d ./class/ ./src/CSVParser.java
	javac -sourcepath ./src/ -d ./class/ ./src/PeekableCharacterStream.java
	javac -sourcepath ./src/ -d ./class/ ./src/PeekableCharacterFileStream.java
	javac -sourcepath ./src/ -d ./class/ ./src/XHTMLSettings.java
	javac -sourcepath ./src/ -d ./class/ ./src/XLanguageDecorator.java
	javac -sourcepath ./src/ -d ./class/ ./src/XLanguageParser.java
tar:
	tar -zcvf Project2Submission.tgz ./src ./README ./makefile
clean:
	rm -f Project2Submission.tgz
	rm -f -R ./class