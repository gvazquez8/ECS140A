import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class NRDatalogParser {
    protected class ParsingException extends Exception {
        public ParsingException(RULES rule, Token token) {
            super("In parsing " + rule.toString() + " unexpected token \"" + token.getText() + "\" of type "
                    + token.getType().toString() + " on line " + token.getLineNumber());
        }
    }

    protected Scanner scanner;
    protected Token currentToken;
    private Token nextToken;

    protected Exception e;

    protected enum RULES {
        Query, Rule, RuleHead, RuleBody, RuleName, HeadVariableList, SubGoal, RuleInvocation, NegatedRuleInvocation,
        EqualityRelation, BodyVariableList, InequalityRelation, EQOperator, InvocationVariable, Term, IEQOperator,
        SimpleTerm, AddOperator, UnaryExpression, MultOperator, UnaryOperator, PrimaryExpression, Constant
    }

    public NRDatalogParser(PeekableCharacterStream stream) {
        String[] keywords = new String[] {"AND", "NOT"};
        scanner = new Scanner(stream, Arrays.asList(keywords));
        getNextToken();
    }

    private void getNextToken() {
        currentToken = scanner.getNextToken();
        nextToken = scanner.peekNextToken();
    }

    public boolean parseQuery() {
        try {
            Query();
            return true;
        }
        catch (ParsingException e1) {
            setError(e1);
            return false;
        }
    }

    public void setError(Exception e) {
        this.e = e;
    }

    public void printError(PrintStream ostream) {
        ostream.println(e.getMessage());
    }

    protected void Query() throws ParsingException {
        // Query := Rule { Rule }
        Rule();

        while(currentToken.getType() != Token.TokenType.NONE) {
            Rule();
        }
    }

    protected void Rule() throws ParsingException {
        // Rule := [ RuleHead [ := RuleBody ] ] NewLine

        if (currentToken.getType() == Token.TokenType.IDENTIFIER) {
            RuleHead();

            if (currentToken.getText().equals(":="))  {
                Operator();
                RuleBody();
            }
        }

        if (currentToken.getType() == Token.TokenType.NONE) {
            return;
        }

        if (!(currentToken.getType() == Token.TokenType.OPERATOR && currentToken.getText().equals("\n"))) {
            throw new ParsingException(RULES.Rule, currentToken);
        }

        Operator();
    }

    protected void RuleHead() throws ParsingException {
        // RuleHead := RuleName ( HeadVariableList )

        RuleName();

        if (currentToken.getText().equals("(")) {
            Operator();
            HeadVariableList();
            if (currentToken.getText().equals(")")) {
                Operator();
            }
            else {
                throw new ParsingException(RULES.RuleHead, currentToken);
            }
        }
        else {
            throw new ParsingException(RULES.RuleHead, currentToken);
        }
    }

    protected void RuleBody() throws ParsingException {
        // RuleBody := SubGoal { AND SubGoal }

        SubGoal();

        while(currentToken.getType() == Token.TokenType.KEYWORD && currentToken.getText().equals("AND")) {
            Keyword();
            SubGoal();
        }
    }

    protected void RuleName() throws ParsingException {
        // RuleName := Identifier

        if (currentToken.getType() == Token.TokenType.IDENTIFIER) {
            Identifier();
        }
        else {
            throw new ParsingException(RULES.RuleName, currentToken);
        }
    }

    protected void HeadVariableList() throws ParsingException {
        // HeadVariableList := Identifier { , Identifier }

        if (currentToken.getType() == Token.TokenType.IDENTIFIER) {
            Identifier();

            while(currentToken.getType() == Token.TokenType.OPERATOR && currentToken.getText().equals(",")) {
                Operator();

                if (currentToken.getType() == Token.TokenType.IDENTIFIER) {
                    Identifier();
                }
                else {
                    throw new ParsingException(RULES.HeadVariableList, currentToken);
                }
            }
        }
        else {
            throw new ParsingException(RULES.HeadVariableList, currentToken);
        }
    }

    protected void SubGoal() throws ParsingException {
        // SubGoal := RuleInvocation | NegatedRuleInvocation | EqualityRelation

        if (currentToken.getType() == Token.TokenType.KEYWORD) {
            NegatedRuleInvocation();
        }
        else if (currentToken.getType() == Token.TokenType.IDENTIFIER && nextToken.getText().equals("(")) {
            RuleInvocation();
        }
        else{
            EqualityRelation();
        }
    }

    protected void RuleInvocation() throws ParsingException {
        // RuleInvocation := RuleName ( BodyVariableList )

        RuleName();

        if (currentToken.getText().equals("(")) {
            Operator();
            BodyVariableList();
            if (currentToken.getText().equals(")")) {
                Operator();
            }
            else {
                throw new ParsingException(RULES.RuleInvocation, currentToken);
            }
        }
        else {
            throw new ParsingException(RULES.RuleInvocation, currentToken);
        }
    }

    protected void NegatedRuleInvocation() throws ParsingException {
        // NegatedRuleInvocation := NOT RuleInvocation

        if (currentToken.getText().equals("NOT")) {
            Keyword();
            RuleInvocation();
        }
        else {
            throw new ParsingException(RULES.NegatedRuleInvocation, currentToken);
        }
    }

    protected void EqualityRelation() throws ParsingException {
        // EqualityRelation := InequalityRelation { EQOperator InequalityRelation }

        InequalityRelation();

        while(currentToken.getType() == Token.TokenType.OPERATOR && !currentToken.getText().equals("\n") && !currentToken.getText().equals(")")) {
            EQOperator();
            InequalityRelation();
        }
    }

    protected void BodyVariableList() throws ParsingException {
        // BodyVariableList := InvocationVariable { , InvocationVariable }

        InvocationVariable();

        while (currentToken.getType() == Token.TokenType.OPERATOR && currentToken.getText().equals(",")) {
            Operator();
            InvocationVariable();
        }
    }

    protected void InequalityRelation() throws ParsingException {
        // InequalityRelation := Term { IEQOperator Term }

        Term();

        while(currentToken.getType() == Token.TokenType.OPERATOR && !currentToken.getText().equals("\n") &&
              !currentToken.getText().equals("=") && !currentToken.getText().equals("!=") && !currentToken.getText().equals(")")) {
            IEQOperator();
            Term();
        }
    }

    protected void EQOperator() throws ParsingException {
        // EQOperator := != | =

        if (currentToken.getText().equals("!=") || currentToken.getText().equals("=")) {
            Operator();
        }
        else {
            throw new ParsingException(RULES.EQOperator, currentToken);
        }
    }

    protected void InvocationVariable() throws ParsingException {
        // InvocationVariable := Identifier | EmptyIdentifier

        if (currentToken.getType() == Token.TokenType.IDENTIFIER || currentToken.getType() == Token.TokenType.EMPTY_IDENTIFIER) {
            Identifier();
        }
        else {
            throw new ParsingException(RULES.InvocationVariable, currentToken);
        }
    }

    protected void Term() throws ParsingException {
        // Term := SimpleTerm { AddOperator SimpleTerm }

        SimpleTerm();

        while(currentToken.getText().equals("-") || currentToken.getText().equals("+")) {
            AddOperator();
            SimpleTerm();
        }
    }

    protected void IEQOperator() throws ParsingException {
        // IEQOperator := < | > | <= | >=

        if (currentToken.getText().equals("<") ||
                currentToken.getText().equals(">") ||
                currentToken.getText().equals("<=") ||
                currentToken.getText().equals(">=")) {
            Operator();
        }
        else {
            throw new ParsingException(RULES.IEQOperator, currentToken);
        }
    }

    protected void SimpleTerm() throws ParsingException {
        // SimpleTerm := UnaryExpression { MultOperator UnaryExpression }

        UnaryExpression();

        while (currentToken.getText().equals("*") || currentToken.getText().equals("/") || currentToken.getText().equals("%")) {
            MultOperator();
            UnaryExpression();
        }
    }

    protected void AddOperator() throws ParsingException {
        // AddOperator := + | -

        if (currentToken.getText().equals("+") || currentToken.getText().equals("-")) {
            Operator();
        }
        else {
            throw new ParsingException(RULES.AddOperator, currentToken);
        }
    }

    protected void UnaryExpression() throws ParsingException {
        // UnaryExpression := ( UnaryOperator UnaryExpression ) | PrimaryExpression

        if (currentToken.getType() == Token.TokenType.OPERATOR && !currentToken.getText().equals("(")) {
            UnaryOperator();
            UnaryExpression();
        }
        else {
            PrimaryExpression();
        }
    }

    protected void MultOperator() throws ParsingException {
        // MultOperator := * | / | %

        if (currentToken.getText().equals("*") || currentToken.getText().equals("/") || currentToken.getText().equals("%")) {
            Operator();
        }
        else {
            throw new ParsingException(RULES.MultOperator, currentToken);
        }
    }

    protected void UnaryOperator() throws ParsingException {
        // UnaryOperator := ! | - | +

        if (currentToken.getText().equals("!") || currentToken.getText().equals("-") || currentToken.getText().equals("+")) {
            Operator();
        }
        else {
            throw new ParsingException(RULES.UnaryOperator, currentToken);
        }
    }

    protected void PrimaryExpression() throws ParsingException {
        // PrimaryExpression:= ( EqualityRelation ) | Constant | Identifier

        if (currentToken.getText().equals("(")) {
            Operator();
            EqualityRelation();
            if (currentToken.getText().equals(")")) {
                Operator();
            }
            else {
                throw new ParsingException(RULES.PrimaryExpression, currentToken);
            }
        }
        else if (currentToken.getType() == Token.TokenType.IDENTIFIER) {
            Identifier();
        }
        else {
            Constant();
        }
    }

    protected void Constant() throws ParsingException {
        // Constant := IntConstant | FloatConstant | StringConstant
        if (currentToken.getType() == Token.TokenType.FLOAT_CONSTANT ||
            currentToken.getType() == Token.TokenType.INT_CONSTANT ||
            currentToken.getType() == Token.TokenType.STRING_CONSTANT) {
            getNextToken();
        }
        else {
            throw new ParsingException(RULES.Constant, currentToken);
        }
    }

    protected void Identifier() {
        getNextToken();
    }

    protected void Operator() {
        getNextToken();
    }

    protected void Keyword() {
        getNextToken();
    }

    public static void main(String[] args) throws IOException {
        NRDatalogParser parser = new NRDatalogParser(new PeekableCharacterFileStream("./examples/name_query.nrdl"));

        if (parser.parseQuery()) {
            System.out.println("File is valid query");
        }
        else {
            System.out.println("File is not valid query");
            parser.printError(System.out);
        }
    }
}
