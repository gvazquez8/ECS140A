all:
	mkdir -p class
	javac -sourcepath ./src/ -d ./class/ ./src/Scanner.java
	javac -sourcepath ./src/ -d ./class/ ./src/Token.java
	javac -sourcepath ./src/ -d ./class/ ./src/CSVParser.java
	javac -sourcepath ./src/ -d ./class/ ./src/PeekableCharacterStream.java
	javac -sourcepath ./src/ -d ./class/ ./src/PeekableCharacterFileStream.java

clean:
	rm -R ./class
