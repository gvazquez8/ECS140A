import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class XLanguageParser {
    protected class ParsingException extends Exception {
        public ParsingException(EBNF EBNF_RULE, Token token) {
            super("In parsing " + EBNF_RULE.toString() + " unexpected token \"" + token.getText() + "\" of type "
                    + token.getType().toString() + " on line " + token.getLineNumber());
        }
    }
    protected class InvalidFileException extends Exception {
        public InvalidFileException(String file) {
            super(file + " is not a valid x file");
        }
    }

    private static final String[] KEYWORDS = new String[] {"unsigned", "char", "short", "int", "long",
                                                           "float", "double", "while", "if", "return",
                                                           "void", "main"};

    private static final List<String> INT_OR_FLOAT_KEYWORDS = Arrays.asList(new String[] {"unsigned", "char", "short", "int", "long", "float", "double"});

    private enum EBNF {
        PROGRAM, DECLARATION, MAIN_DECLARATION, FUNCTION_DEFINITION, DECLARATION_TYPE, VARIABLE_DECLARATION, FUNCTION_DECLARATION,
        BLOCK, PARAMETER_BLOCK, DATA_TYPE, CONSTANT, STATEMENT, PARAMETER, INTEGER_TYPE, FLOAT_TYPE, ASSIGNMENT, WHILE_LOOP, IF_STATEMENT,
        RETURN_STATEMENT, EXPRESSION, TERM, FACTOR, RELATIONAL_OPERATOR, ADD_OPERATOR, MULT_OPERATOR
    }

    public Scanner scanner;

    protected Token nextToken;
    protected Token nextPeekedToken;

    XLanguageParser(String xfile) throws IOException, InvalidFileException {
        if (!xfile.substring(xfile.length() - 2).equals(".x")) {
            throw new InvalidFileException(xfile);
        }
        scanner = new Scanner(new PeekableCharacterFileStream(xfile), Arrays.asList(KEYWORDS));
        getNextToken();
    }

    protected  void getNextToken() {
        nextToken = scanner.getNextToken();
        nextPeekedToken = scanner.peekNextToken();
    }

    public void Run() throws ParsingException {
        Program();
    }

    protected  void Program() throws ParsingException {
        // Program := {Declaration} MainDeclaration {FunctionDefinition}
        while (!nextToken.getText().equals("void") && !nextPeekedToken.getText().equals("main")) {
            Declaration();
        }

        MainDeclaration();

        while (nextToken.getType() == Token.TokenType.KEYWORD ) {
            FunctionDefinition();
        }

    }

    protected  void Declaration() throws ParsingException {
        // Declaration := DeclarationType (VariableDeclaration | FunctionDeclaration)

        DeclarationType();

        if (nextToken.getText().equals("=") || nextToken.getText().equals(";")) {
            VariableDeclaration();
        }
        else if (nextToken.getText().equals("(")) {
            FunctionDeclaration();
        }
        else {
            throw new ParsingException(EBNF.DECLARATION, nextToken);
        }
    }

    protected  void MainDeclaration() throws ParsingException {
        // MainDeclaration := void main ( ) Block

        if (nextToken.getText().equals("void")) {
            Keyword();
            getNextToken();
            if (nextToken.getText().equals("main")) {
                Keyword();
                getNextToken();
                if (nextToken.getText().equals("(")) {
                    Operator();
                    getNextToken();
                    if (nextToken.getText().equals(")")) {
                        Operator();
                        getNextToken();
                        Block();

                    }
                    else {
                        throw new ParsingException(EBNF.MAIN_DECLARATION, nextToken);
                    }
                }
                else {
                    throw new ParsingException(EBNF.MAIN_DECLARATION, nextToken);
                }
            }
            else {
                throw new ParsingException(EBNF.MAIN_DECLARATION, nextToken);
            }
        }
        else {
            throw new ParsingException(EBNF.MAIN_DECLARATION, nextToken);
        }
    }

    protected  void FunctionDefinition() throws ParsingException {
        // FunctionDefinition := DeclarationType ParameterBlock Block

        if (nextToken.getType() == Token.TokenType.KEYWORD) {
            DeclarationType();
            ParameterBlock(); // set a bool "isFunction" to true
            Block();

        }
        else {
            throw new ParsingException(EBNF.FUNCTION_DEFINITION, nextToken);
        }
    }

    protected  void DeclarationType() throws ParsingException {
        // DeclarationType := DataType Identifier

        if (INT_OR_FLOAT_KEYWORDS.contains(nextToken.getText())) {
            DataType();
            if (nextToken.getType() == Token.TokenType.IDENTIFIER) {
                Identifier();
            }
            else {
                throw new ParsingException(EBNF.DECLARATION_TYPE, nextToken);
            }
        }
        else {
            throw new ParsingException(EBNF.DECLARATION_TYPE, nextToken);
        }
    }

    protected  void VariableDeclaration() throws ParsingException {
        // VariableDeclaration := [= Constant] ;

        if (nextToken.getText().equals("=")) {
            Operator();
            getNextToken();
            Constant();
        }

        if (nextToken.getText().equals(";")) {
            Operator();
            getNextToken();
        }
        else {
            throw new ParsingException(EBNF.VARIABLE_DECLARATION, nextToken);
        }
    }

    protected  void FunctionDeclaration() throws ParsingException {
        // FunctionDeclaration := ParameterBlock ;

        ParameterBlock();
        if (nextToken.getText().equals(";")) {
            Operator();
            getNextToken();
        }
        else {
            throw new ParsingException(EBNF.FUNCTION_DECLARATION, nextToken);
        }
    }

    protected  void Block() throws ParsingException {
        // Block := { {Declaration} [(Statement {Statement} {FunctionDefinition})] }

        if (nextToken.getText().equals("{")) {
            Operator();
            getNextToken();

            while (nextToken.getType() == Token.TokenType.KEYWORD && INT_OR_FLOAT_KEYWORDS.contains(nextToken.getText())) {
                Declaration();
            }

            if (nextToken.getType() == Token.TokenType.IDENTIFIER ||
               (!INT_OR_FLOAT_KEYWORDS.contains(nextToken.getText()) && nextToken.getType() == Token.TokenType.KEYWORD)) { // check if identifier or if is a keyword that is not a part of INT_OR_FLOAT_TYPES
                Statement();
                while(nextToken.getType() == Token.TokenType.IDENTIFIER ||
                     (!INT_OR_FLOAT_KEYWORDS.contains(nextToken.getText()) && nextToken.getType() == Token.TokenType.KEYWORD)) {
                    Statement();
                }

                while(nextToken.getType() == Token.TokenType.KEYWORD) {
                    FunctionDefinition();
                }
            }

            if (nextToken.getText().equals("}")) {
                Operator();
                getNextToken();
            }
            else {
                throw new ParsingException(EBNF.BLOCK, nextToken);
            }

        }
        else {
            throw new ParsingException(EBNF.BLOCK, nextToken);
        }
    }

    protected  void ParameterBlock() throws ParsingException {
        // ParameterBlock := ( [Parameter {, Parameter}] )

        if (nextToken.getText().equals("(")) {
            Operator();
            getNextToken();

            if (nextToken.getType() == Token.TokenType.KEYWORD) {
                Parameter();
                while (nextToken.getText().equals(",")) {
                    Operator();
                    getNextToken();
                    Parameter();
                }
            }

            if (nextToken.getText().equals(")")) {
                Operator();
                getNextToken();
            }
            else {
                throw new ParsingException(EBNF.PARAMETER_BLOCK, nextToken);
            }
        }
        else {
            throw new ParsingException(EBNF.PARAMETER_BLOCK, nextToken);
        }
    }

    protected  void DataType() throws ParsingException {
       // DataType := IntegerType | FloatType

       if (INT_OR_FLOAT_KEYWORDS.contains(nextToken.getText())) {
           if (nextToken.getText().equals("float") || nextToken.getText().equals("double")) {
               FloatType();
           }
           else {
               IntegerType();
           }
       }
       else {
           throw new ParsingException(EBNF.DATA_TYPE, nextToken);
       }
    }

    protected  void Constant() throws ParsingException {
        // Constant := IntConstant | FloatConstant

        if (nextToken.getType() == Token.TokenType.INT_CONSTANT || nextToken.getType() == Token.TokenType.FLOAT_CONSTANT) {
            if (nextToken.getType() == Token.TokenType.INT_CONSTANT) {
                IntConstant();
            }
            else {
                FloatConstant();
            }

            getNextToken();
        }
        else {
            throw new ParsingException(EBNF.CONSTANT, nextToken);
        }
    }

    protected  void Statement() throws ParsingException {
        // Statement := Assignment | WhileLoop | IfStatement | ReturnStatement | (Expression ;)

        if (nextToken.getType() == Token.TokenType.IDENTIFIER && nextPeekedToken.getText().equals("=")) {
            Assignment();
        }
        else if (nextToken.getText().equals("while")) {
            WhileLoop();
        }
        else if (nextToken.getText().equals("if")) {
            IfStatement();
        }
        else if (nextToken.getText().equals("return")) {
            ReturnStatement();
        }
        else { // possible errors here
            Expression();
            if (nextToken.getText().equals(";")) {
                Operator();
                getNextToken();
            }
            else {
                throw new ParsingException(EBNF.STATEMENT, nextToken);
            }
        }
    }

    protected  void Parameter() throws ParsingException {
        // Parameter := DataType Identifier

        DataType();
        if (nextToken.getType() == Token.TokenType.IDENTIFIER) {
            Identifier();
        }
        else {
            throw new ParsingException(EBNF.PARAMETER, nextToken);
        }
    }

    protected  void IntegerType() throws ParsingException {
        // IntegerType := [unsigned] ( char | short | int | long )
        if (nextToken.getText().equals("unsigned")) {
            Keyword();
            getNextToken();
        }

        String text= nextToken.getText();

        if (text.equals("char") || text.equals("short") || text.equals("int") || text.equals("long")) {
            Keyword();
            getNextToken();
        }
        else {
            throw new ParsingException(EBNF.INTEGER_TYPE, nextToken);
        }
    }

    protected  void FloatType() throws ParsingException {
        // FloatType := float | double

        if (nextToken.getText().equals("float") || nextToken.getText().equals("double")) {
            Keyword();
            getNextToken();
        }
        else {
            throw new ParsingException(EBNF.FLOAT_TYPE, nextToken);
        }
    }

    protected  void Assignment() throws ParsingException {
        // Assignment := Identifier = {Identifier =} Expression ;

        if (nextToken.getType() == Token.TokenType.IDENTIFIER) {
            Identifier();
            if (nextToken.getText().equals("=")) {
                Operator();
                getNextToken();

                while (nextToken.getType() == Token.TokenType.IDENTIFIER && nextPeekedToken.getText().equals("=")) {
                    getNextToken();
                    if (nextToken.getText().equals("=") ) {
                        Operator();
                        Identifier();
                    }
                    else {
                        throw new ParsingException(EBNF.ASSIGNMENT, nextToken);
                    }
                }

                Expression();

                if (nextToken.getText().equals(";")) {
                    Operator();
                    getNextToken();
                }
                else {
                    throw new ParsingException(EBNF.ASSIGNMENT, nextToken);
                }
            }
            else {
                throw new ParsingException(EBNF.ASSIGNMENT, nextToken);
            }
        }
        else {
            throw new ParsingException(EBNF.ASSIGNMENT, nextToken);
        }
    }

    protected  void WhileLoop() throws ParsingException {
        // WhileLoop := while ( Expression ) Block

        if (nextToken.getText().equals("while")) {
            Keyword();
            getNextToken();
            if (nextToken.getText().equals("(")) {
                Operator();
                getNextToken();
                Expression();
                if (nextToken.getText().equals(")")) {
                    Operator();
                    getNextToken();
                    Block();
                }
                else {
                    throw new ParsingException(EBNF.WHILE_LOOP, nextToken);
                }
            }
            else {
                throw new ParsingException(EBNF.WHILE_LOOP, nextToken);
            }
        }
        else {
            throw new ParsingException(EBNF.WHILE_LOOP, nextToken);
        }
    }

    protected  void IfStatement() throws ParsingException {
        // IfStatement := if ( Expression ) Block

        if (nextToken.getText().equals("if")) {
            Keyword();
            getNextToken();
            if (nextToken.getText().equals("(")) {
                Operator();
                getNextToken();
                Expression();
                if (nextToken.getText().equals(")")) {
                    Operator();
                    getNextToken();
                    Block();
                }
                else {
                    throw new ParsingException(EBNF.IF_STATEMENT, nextToken);
                }
            }
            else {
                throw new ParsingException(EBNF.IF_STATEMENT, nextToken);
            }
        }
        else {
            throw new ParsingException(EBNF.IF_STATEMENT, nextToken);
        }
    }

    protected  void ReturnStatement() throws ParsingException {
        // ReturnStatement := return Expression ;

        if (nextToken.getText().equals("return")) {
            Keyword();
            getNextToken();
            Expression();
            if (nextToken.getText().equals(";")) {
                Operator();
                getNextToken();
            }
            else {
                throw new ParsingException(EBNF.RETURN_STATEMENT, nextToken);
            }
        }
        else {
            throw new ParsingException(EBNF.RETURN_STATEMENT, nextToken);
        }
    }

    protected  void Expression() throws ParsingException {
        // Expression := SimpleExpression [ RelationOperator SimpleExpression ]
        SimpleExpression();

        if (nextToken.getType() == Token.TokenType.OPERATOR && !nextToken.getText().equals(")") && !nextToken.getText().equals(",") &&
            !nextToken.getText().equals(";") && !nextToken.getText().equals("}")) {
            RelationOperator();
            SimpleExpression();
        }

    }

    protected  void SimpleExpression() throws ParsingException {
        // SimpleExpression := Term { AddOperator Term }

        Term();

        while (nextToken.getText().equals("+") || nextToken.getText().equals("-")) {
            AddOperator();
            Term();
        }
    }

    protected  void Term() throws ParsingException {
        // Term := Factor { MultOperator Term }

        Factor();

        while (nextToken.getText().equals("*") || nextToken.getText().equals("/")) {
            MultOperator();
            Factor();
        }
    }

    protected  void Factor() throws ParsingException {
        // Factor := ( ( Expression ) ) | Constant | (Identifier [ ( [ Expression {, Expression }] ) ] )

        if (nextToken.getText().equals("(")) {
            Operator();
            getNextToken();
            Expression();
            if (nextToken.getText().equals(")")) {
                Operator();
                getNextToken();
            }
            else {
                throw new ParsingException(EBNF.FACTOR, nextToken);
            }
        }
        else if (nextToken.getType() == Token.TokenType.INT_CONSTANT || nextToken.getType() == Token.TokenType.FLOAT_CONSTANT) {
            Constant();
        }
        else if (nextToken.getType() == Token.TokenType.IDENTIFIER) {
            Identifier();
            if (nextToken.getText().equals("(")) {
                Operator();
                getNextToken();
                if (!nextToken.getText().equals(")")) {
                    Expression();
                    while (nextToken.getText().equals(",")) {
                        Operator();
                        getNextToken();
                        Expression();
                    }
                    if (nextToken.getText().equals(")")) {
                        Operator();
                        getNextToken();
                    }
                    else {
                        throw new ParsingException(EBNF.FACTOR, nextToken);
                    }
                }
            }
        }
        else {
            throw new ParsingException(EBNF.FACTOR, nextToken);
        }
    }

    protected  void RelationOperator() throws ParsingException {
        // RelationOperator := ( == ) | < | > | ( <= ) | ( >= ) | ( != )
        String op = nextToken.getText();
        if (op.equals("==") || op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=") || op.equals("!=")) {
            Operator();
            getNextToken();
        }
        else {
            throw new ParsingException(EBNF.RELATIONAL_OPERATOR, nextToken);
        }
    }

    protected  void AddOperator() throws ParsingException {
        // AddOperator := + | -

        if (nextToken.getText().equals("+")|| nextToken.getText().equals("-")) {
            Operator();
            getNextToken();
        }
        else {
            throw new ParsingException(EBNF.ADD_OPERATOR, nextToken);
        }
    }

    protected void MultOperator() throws ParsingException {
        // MultOperator := * | /

        if (nextToken.getText().equals("*") || nextToken.getText().equals("/")) {
            Operator();
            getNextToken();
        }
        else {
            throw new ParsingException(EBNF.MULT_OPERATOR, nextToken);
        }
    }

    protected  void Identifier() {
        getNextToken();
    }

    protected  void IntConstant() {}

    protected  void FloatConstant() {}

    protected  void Operator() {}

    protected  void Keyword() {}

    protected  void Function() {}

    protected void Variable() {}

    public static void main(String[] args) throws IOException, ParsingException, InvalidFileException {
        XLanguageParser parser = new XLanguageParser(args[0]);

        parser.Run();

        System.out.println(args[0] + " is a valid X file!");
    }
}
