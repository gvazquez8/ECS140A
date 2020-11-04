import java.io.IOException;
import java.util.*;

public class Scanner {

    /*
    Token Rules:
    Identifier := ( _ | Alpha ) { ( _ | Digit | Alpha ) } (done)
    Operator := ( | , | ) | { | } | = | == | < | > | <= | >= | != | + | - | * | / | ; (done)
    IntConstant := [ - ] Digit { Digit } (done)
    FloatConstant := [ - ] Digit { Digit } [ . Digit { Digit } ] (done)
    StringConstant := " { ( CharacterLiteral | EscapedCharacter ) } " (done)
    Digit := 0 – 9 (done)
    Alpha := A – Z | a – z (done)
    WhiteSpace := Space | Tab | CarriageReturn | NewLine (done)
    CharacterLiteral := Space - ! | # - [ | ] - ~  (done)
    EscapedCharacter := \b | \n | \r | \t | \\ | \' | \" (done)
     */
    private static final char NEWLINE = '\n';
    private static final char TAB = '\t';
    private static final char CR = '\r';
    private static final char SPACE = ' ';
    private static final char DOUBLE_QUOTES = '\"';
    private static final char UNDERSCORE = '_';
    private static final char DOT = '.';
    private static final char[] ESCAPED_CHARACTERS = new char[] {'\b', '\n', '\r', '\t', '\\', '\'', '\"'};
    private static final String[] OPERATORS = new String[] {"(", ",", ")", "{", "}", "=", "==", "<", ">", "<=", ">=", "!=", "+", "-", "*", "/", ";"};

    private final PeekableCharacterFileStream stream;
    private final List<String> keywords;
    private Token seekedToken = null;
    private boolean previousTokenConstantOrIdentifier = false;
    private int nextTokenLineNum = -1;
    private int nextTokenCharPos = -1;

    public Scanner(PeekableCharacterStream stream, List<String> keywordList) {
        this.stream = (PeekableCharacterFileStream) stream;
        keywords = keywordList;

    }

    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isCharacterLiteral(char c) { // CharacterLiteral := Space - ! | # - [ | ] - ~
        return (c >= SPACE && c <= '!') || (c >= '#' && c <= '[') || (c >= ']' && c <= '~');
    }

    private boolean isOperator(String str) { // Operator := ( | , | ) | { | } | = | == | < | > | <= | >= | != | + | - | * | / | ;
        if (str.length() > 2) {
            return false;
        }

        for (String op : OPERATORS) {
            if (str.equals(op)) {
                return true;
            }
        }

        return false;
    }

    private boolean isOperator(char c) {
        String str = "" + c;
        return isOperator(str);
    }

    private boolean isEscapeChar(char c) {

        for (char escChar : ESCAPED_CHARACTERS) {
            if (c == escChar) {
                return true;
            }
        }
        return false;
    }

    private boolean isStringConstant(String str) {
        if (str.length() <= 2) { // the shortest string constant is "" (empty) so if there are less than 2 characters, it is not a string const.
            return false;
        }

        if (str.charAt(0) == DOUBLE_QUOTES && str.charAt(str.length()-1) == DOUBLE_QUOTES) {
            for (int i = 1; i < str.length() -1; i++) {
                char c = str.charAt(i);
                if (!(isEscapeChar(c) || isCharacterLiteral(c))) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    private boolean isIntConstant(String str) {
        try {
            Integer.parseInt(str);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isIntConstant(char c) {
        String s = "" + c;
        return isIntConstant(s);
    }

    private boolean isFloatConstant(String str) {
        try {
            Float.parseFloat(str);
            if (str.charAt(str.length()-1) == DOT) { return false; }
            return true;
        }
        catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    private boolean isNumber(String s) {
        return isIntConstant(s) || isFloatConstant(s);
    }

    private boolean isNumber(char c) {
        return isNumber(""+c);
    }

    private boolean isConstant(String s) {
        return isNumber(s) || isStringConstant(s);
    }

    private boolean isConstant(char c) {
        return isConstant(""+c);
    }

    private boolean isIdentifier(String str) {
        if (str.length() == 0) {
            return false;
        }

        if (str.charAt(0) == UNDERSCORE || isAlpha(str.charAt(0))) {
            for (char c : str.toCharArray()) {
                if (!(c == UNDERSCORE || isAlpha(c) || isDigit(c))) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    private boolean isKeyword(String str) {
        return keywords.contains(str);
    }

    private boolean isValidCharacter(char c) {
        return (isOperator(c) || isAlpha(c) || isDigit(c));
    }

    private boolean isWhiteSpace(char c) {
        return (c == NEWLINE || c == TAB || c == SPACE || c == CR);
    }

    private Token tokenizeWord(String tokenStr) {
        Token.TokenType type = Token.TokenType.INVALID;

        if (isKeyword(tokenStr)) {
            previousTokenConstantOrIdentifier = false;
            type = Token.TokenType.KEYWORD;
        }
        else if (isOperator(tokenStr)) {
            previousTokenConstantOrIdentifier = false;
            type = Token.TokenType.OPERATOR;
        }
        else if (isIntConstant(tokenStr)) {
            previousTokenConstantOrIdentifier = true;
            type = Token.TokenType.INT_CONSTANT;
        }
        else if (isFloatConstant(tokenStr)) {
            previousTokenConstantOrIdentifier = true;
            type = Token.TokenType.FLOAT_CONSTANT;
        }
        else if (isStringConstant(tokenStr)) {
            previousTokenConstantOrIdentifier = true;
            type = Token.TokenType.STRING_CONSTANT;
        }
        else if (isIdentifier(tokenStr)) {
            previousTokenConstantOrIdentifier = true;
            type = Token.TokenType.IDENTIFIER;
        }
        else {
            previousTokenConstantOrIdentifier = false;
        }

        return new Token(tokenStr, type, nextTokenLineNum, nextTokenCharPos);
    }

    private String getTokenString() {
        StringBuilder builder = new StringBuilder();
        boolean inDoubleQuotes = false;
        while(stream.moreAvailable()) {
            if (builder.length() == 0) {
                nextTokenLineNum = stream.getCurrentRowIndex(); // Set line num of the next token
                nextTokenCharPos = stream.getCurrentColIndex(); // Set char pos of next token
            }
            char c = (char)stream.getNextChar();
            char peeked = (char)stream.peekNextChar();

            if (c == DOUBLE_QUOTES) { inDoubleQuotes = !inDoubleQuotes; } // Allow character literals and esc chars

            if (isWhiteSpace(c) && !inDoubleQuotes) { // if we encounter white space not in quotes
                if (builder.length() == 0) { continue; } // skip over it if we don't have a string
                break; // stop building token string
            }

            builder.append(c); // append the letter to the string

            if (!inDoubleQuotes) {
                if (isOperator(""+c+peeked)) { // the next operator is two characters long (>=, <=, etc.)
                      continue;
                }
                else if (isNumber(builder.toString()) && isOperator(peeked)) { // Ex: 5+ or 53-
                    break;
                }
                else if (isOperator(c) && isOperator(peeked) && !isOperator(""+c+peeked)) { // two operators next to each other but invalid when paired so separate them
                    break;
                }
                else if (previousTokenConstantOrIdentifier && isOperator(c) && isNumber(peeked)) { // Ex: -5 or -42.3
                    break;
                }
                else if (isOperator(c) && !isNumber(peeked)) { // Ex: -Fort => - , Fort
                    break;
                }
                else if (isIdentifier(builder.toString()) && (isOperator(peeked) || (!isIdentifier(""+peeked) && !isNumber(peeked)))) { // hello) => hello, )
                    break;
                }
                else if (!isValidCharacter(c) && builder.length() == 1) { // Ex: #$4 => # , $ , 4
                    break;
                }
            }

            if (isStringConstant(builder.toString()) && isOperator(peeked)) { break; }

        }

        if ((char)stream.peekNextChar() == SPACE || (char)stream.peekNextChar() == NEWLINE) { stream.getNextChar(); }
        return builder.toString();
    }

    public Token peekNextToken() {
        if (seekedToken ==  null) {
            seekedToken = getNextToken();
        }
        return seekedToken;
    }

    public Token getNextToken() {
        if (seekedToken != null) {
            Token next = seekedToken;
            seekedToken = null;
            return next;
        }
        if (!stream.moreAvailable()) {
            return new Token("", Token.TokenType.NONE, stream.getCurrentRowIndex(), stream.getCurrentColIndex());
        }

        String potentialToken = getTokenString();
        return tokenizeWord(potentialToken);

    }

    public void printToken(Token MyToken) {
        System.out.println("@   "+MyToken.getLineNumber()+",   "+MyToken.getCharPosition()+"\t"+MyToken.getType()+"\t\""+MyToken.getText()+"\"");
    }

    public static void main(String[] args) throws IOException {
        String[] keywords = new String[] {"unsigned", "char", "short", "int", "long", "float", "double", "while", "if", "return", "void", "main"};

        Scanner sc = new Scanner(new PeekableCharacterFileStream(args[0]),
                                    Arrays.asList(keywords.clone()));

        while (true) {
            Token t = sc.getNextToken();
            if (t.getType() == Token.TokenType.NONE) {
                sc.printToken(t);
                break;
            }
            sc.printToken(t);
        }


    }
}
