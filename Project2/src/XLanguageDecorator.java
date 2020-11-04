import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class XLanguageDecorator extends XLanguageParser{
    XHTMLSettings htmlSettings;
    StringBuilder htmlPage = new StringBuilder();

    Token currentToken;
    Token decorator_nextToken;
    int currentScope = 0;

    // scope based on index. Global scope = 0
    ArrayList<HashMap<String, String>> symbolTable = new ArrayList<>();
    int variableCounter = 0;
    int functionCounter = 0;
    String hyperLinkVar = null;
    boolean addHREF = false;
    boolean inParameterBlock = false;

    XLanguageDecorator(String xfile, String XHTMLSettingsFile) throws IOException, InvalidFileException {
        super(xfile);
        htmlSettings = new XHTMLSettings(XHTMLSettingsFile);
        SetUpHTML();
        symbolTable.add(new HashMap<>());
    }

    private void SetUpHTML() {
        htmlPage.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1- transitional.dtd\">\n");
        htmlPage.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n");
        htmlPage.append("<head>\n<title>Gabriel's X Formatted File</title>\n</head>\n");
        AddHTMLElement("body", "DEFAULT", "bgcolor", "BACKGROUND", "text", "FOREGROUND", "link", "FOREGROUND", "vlink", "FOREGROUND");
        htmlPage.append("\n");
        AddHTMLElement("font", "DEFAULT", "face", "FONT");
        htmlPage.append("\n");
    }

    private void AddHTMLElement(String tag, String elementType, String... attrs) {
        htmlPage.append("<" + tag);
        for (int i = 0; i < attrs.length; i += 2) {
            htmlPage.append(" " + attrs[i] + "=\"" + htmlSettings.getAttribute(elementType, attrs[i+1]) + "\"");
        }
        htmlPage.append(">");
    }

    private void CloseHTMLElement(String tag) {
        htmlPage.append("</" + tag + ">");
    }

    private void AddHTMLLiteral(String literal) {
        htmlPage.append(literal);
    }

    private void AddLineBreak() { htmlPage.append("<br />"); }

    private void AddTabs() {
        for (int i = 0; i < currentScope; i++) {
            htmlPage.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
    }

    @Override
    protected void getNextToken() {
        super.getNextToken();
        currentToken = super.nextToken;
        decorator_nextToken = super.nextPeekedToken;
    }

    @Override
    public void Run() throws ParsingException {
        super.Run();
    }

    @Override
    protected void Program() throws ParsingException {
        // Program := {Declaration} MainDeclaration {FunctionDefinition}
        super.Program();
    }

    @Override
    protected void Declaration() throws ParsingException {
        // Declaration := DeclarationType (VariableDeclaration | FunctionDeclaration)
        super.Declaration();
    }

    @Override
    protected void MainDeclaration() throws ParsingException {
        // MainDeclaration := void main ( ) Block
        super.MainDeclaration();
    }

    @Override
    protected void FunctionDefinition() throws ParsingException {
        // FunctionDefinition := DeclarationType ParameterBlock Block
        super.FunctionDefinition();
    }

    @Override
    protected void DeclarationType() throws ParsingException {
        // DeclarationType := DataType Identifier
        super.DeclarationType();
    }

    @Override
    protected void VariableDeclaration() throws ParsingException {
        // VariableDeclaration := [= Constant] ;
        super.VariableDeclaration();
    }

    @Override
    protected void FunctionDeclaration() throws ParsingException {
        // FunctionDeclaration := ParameterBlock ;
        inParameterBlock = true;
        super.FunctionDeclaration();
        inParameterBlock = false;
    }

    @Override
    protected void Block() throws ParsingException {
        // Block := { {Declaration} [(Statement {Statement} {FunctionDefinition})] }
        currentScope++;
        symbolTable.add(new HashMap<>());
        super.Block();
    }

    @Override
    protected void ParameterBlock() throws ParsingException {
        // ParameterBlock := ( [Parameter {, Parameter}] )
        super.ParameterBlock();
    }

    @Override
    protected void DataType() throws ParsingException {
        // DataType := IntegerType | FloatType
        super.DataType();
    }

    @Override
    protected void Constant() throws ParsingException {
        // Constant := IntConstant | FloatConstant
        super.Constant();
    }

    @Override
    protected void Statement() throws ParsingException {
        // Statement := Assignment | WhileLoop | IfStatement | ReturnStatement | (Expression ;)
        super.Statement();
    }

    @Override
    protected void Parameter() throws ParsingException {
        // Parameter := DataType Identifier
        super.Parameter();
    }

    @Override
    protected void IntegerType() throws ParsingException {
        // IntegerType := [unsigned] ( char | short | int | long )
        super.IntegerType();
    }

    @Override
    protected void FloatType() throws ParsingException {
        // FloatType := float | double
        super.FloatType();
    }

    @Override
    protected void Assignment() throws ParsingException {
        // Assignment := Identifier = {Identifier =} Expression ;
        super.Assignment();
    }

    @Override
    protected void WhileLoop() throws ParsingException {
        // WhileLoop := while ( Expression ) Block
        super.WhileLoop();
    }

    @Override
    protected void IfStatement() throws ParsingException {
        // IfStatement := if ( Expression ) Block
        super.IfStatement();
    }

    @Override
    protected void ReturnStatement() throws ParsingException {
        // ReturnStatement := return Expression ;
        super.ReturnStatement();
    }

    @Override
    protected void Expression() throws ParsingException {
        // Expression := SimpleExpression [ RelationOperator SimpleExpression ]
        super.Expression();
    }

    @Override
    protected void SimpleExpression() throws ParsingException {
        // SimpleExpression := Term { AddOperator Term }
        super.SimpleExpression();
    }

    @Override
    protected void Term() throws ParsingException {
        // Term := Factor { MultOperator Term }
        super.Term();
    }

    @Override
    protected void Factor() throws ParsingException {
        // Factor := ( ( Expression ) ) | Constant | (Identifier [ ( [ Expression {, Expression }] ) ] )
        super.Factor();
    }

    @Override
    protected void RelationOperator() throws ParsingException {
        super.RelationOperator();
    }

    @Override
    protected void AddOperator() throws ParsingException {
        // AddOperator := + | -
        super.AddOperator();
    }

    @Override
    protected void MultOperator() throws ParsingException {
        // MultOperator := * | /
        super.MultOperator();
    }

    @Override
    protected void Identifier() {

        String symbolName = currentToken.getText();
        int scope = currentScope;

        while (scope >= 0) {
            if (symbolTable.get(scope).containsKey(symbolName) && !inParameterBlock) {
                addHREF = true;
                hyperLinkVar = symbolTable.get(scope).get(symbolName);
                break;
            }
            scope--;
        }

        if (decorator_nextToken.getText().equals("(")) {
            Function();
        }
        else {
            Variable();
        }
        super.Identifier();
    }

    @Override
    protected void IntConstant() {
        boolean closeFont = false, closeStyle = false;
        if (!htmlSettings.getAttribute("INT_CONSTANT", "FOREGROUND").equals("null")) {
            AddHTMLElement("font", "INT_CONSTANT", "color", "FOREGROUND");
            closeFont = true;
        }

        if (!htmlSettings.getAttribute("INT_CONSTANT", "STYLE").equals("null")) {
            AddHTMLElement(htmlSettings.getAttribute("INT_CONSTANT", "STYLE"), "INT_CONSTANT");
            closeStyle = true;
        }

        AddHTMLLiteral(currentToken.getText());

        if (closeStyle) {
            CloseHTMLElement(htmlSettings.getAttribute("INT_CONSTANT", "STYLE"));
        }

        if (closeFont) {
            CloseHTMLElement("font");
        }

    }

    @Override
    protected void FloatConstant() {
        boolean closeFont = false, closeStyle = false;
        if (!htmlSettings.getAttribute("FLOAT_CONSTANT", "FOREGROUND").equals("null")) {
            AddHTMLElement("font", "FLOAT_CONSTANT", "color", "FOREGROUND");
            closeFont = true;
        }

        if (!htmlSettings.getAttribute("FLOAT_CONSTANT", "STYLE").equals("null")) {
            AddHTMLElement(htmlSettings.getAttribute("FLOAT_CONSTANT", "STYLE"), "FLOAT_CONSTANT");
            closeStyle = true;
        }

        AddHTMLLiteral(currentToken.getText());

        if (closeStyle) {
            CloseHTMLElement(htmlSettings.getAttribute("FLOAT_CONSTANT", "STYLE"));
        }

        if (closeFont) {
            CloseHTMLElement("font");
        }

    }

    @Override
    protected void Function() {
        boolean closeFont = false, closeStyle = false;

        if (addHREF) {
            htmlPage.append("<a href=\"#" + hyperLinkVar + "\">");
        }
        else {
            String name = "f" + functionCounter++ + currentToken.getText();
            htmlPage.append("<a name=\"" + name + "\"/>");
            symbolTable.get(currentScope).put(currentToken.getText(), name);
        }

        if (!htmlSettings.getAttribute("FUNCTION", "FOREGROUND").equals("null")) {
            AddHTMLElement("font", "FUNCTION", "color", "FOREGROUND");
            closeFont = true;
        }

        if (!htmlSettings.getAttribute("FUNCTION", "STYLE").equals("null")) {
            AddHTMLElement(htmlSettings.getAttribute("FUNCTION", "STYLE"), "FUNCTION");
            closeStyle = true;
        }

        AddHTMLLiteral(currentToken.getText());

        if (closeStyle) {
            CloseHTMLElement(htmlSettings.getAttribute("FUNCTION", "STYLE"));
        }

        if (closeFont) {
            CloseHTMLElement("font");
        }

        if (addHREF) {
            htmlPage.append("</a>");
            addHREF = false;
        }

    }

    @Override
    protected void Variable() {
        boolean closeFont = false, closeStyle = false;

        if (addHREF) {
            htmlPage.append("<a href=\"#" + hyperLinkVar + "\">");
        }
        else if (!inParameterBlock){
            String name = "v" + variableCounter++ + currentToken.getText();
            htmlPage.append("<a name=\"" + name + "\"" + "/>");
            symbolTable.get(currentScope).put(currentToken.getText(), name);
        }

        if (!htmlSettings.getAttribute("VARIABLE", "FOREGROUND").equals("null")) {
            AddHTMLElement("font", "VARIABLE", "color", "FOREGROUND");
            closeFont = true;
        }

        if (!htmlSettings.getAttribute("VARIABLE", "STYLE").equals("null")) {
            AddHTMLElement(htmlSettings.getAttribute("VARIABLE", "STYLE"), "VARIABLE");
            closeStyle = true;
        }

        AddHTMLLiteral(currentToken.getText());

        if (closeStyle) {
            CloseHTMLElement(htmlSettings.getAttribute("VARIABLE", "STYLE"));
        }

        if (closeFont) {
            CloseHTMLElement("font");
        }

        if (addHREF) {
            htmlPage.append("</a>");
            addHREF = false;
        }

        if (!(decorator_nextToken.getText().equals(";") || decorator_nextToken.getText().equals(")")
            || decorator_nextToken.getText().equals(","))) {
            htmlPage.append(" ");
        }
    }

    @Override
    protected void Operator() {
        boolean closeFont = false, closeStyle = false;

        if (decorator_nextToken.getText().equals("}")) {
            symbolTable.remove(currentScope);
            currentScope--;
        }

        if (!htmlSettings.getAttribute("OPERATOR", "FOREGROUND").equals("null")) {
            AddHTMLElement("font", "OPERATOR", "color", "FOREGROUND");
            closeFont = true;
        }

        if (!htmlSettings.getAttribute("OPERATOR", "STYLE").equals("null")) {
            AddHTMLElement(htmlSettings.getAttribute("OPERATOR", "STYLE"), "OPERATOR");
            closeStyle = true;
        }

        if (currentToken.getText().equals(">")) {
            AddHTMLLiteral("&gt");
        }
        else if (currentToken.getText().equals("<")) {
            AddHTMLLiteral("&lt");
        }
        else {
            AddHTMLLiteral(currentToken.getText());
        }

        if (closeStyle) {
            CloseHTMLElement(htmlSettings.getAttribute("OPERATOR", "STYLE"));
        }

        if (closeFont) {
            CloseHTMLElement("font");
        }

        if (!(currentToken.getText().equals("(") || currentToken.getText().equals(")"))) {
            htmlPage.append(" ");
        }
        else if (currentToken.getText().equals(")") &&
                (decorator_nextToken.getType() == Token.TokenType.OPERATOR &&
                !(decorator_nextToken.getText().equals(";") || decorator_nextToken.getText().equals("{") ||
                  decorator_nextToken.getText().equals(",")))) {
            htmlPage.append(" ");
        }

        if (currentToken.getText().equals(";")) {
            inParameterBlock = false;
        }

        if (currentToken.getText().equals(";") || currentToken.getText().equals("{") || currentToken.getText().equals("}")) {
            AddLineBreak();
            htmlPage.append("\n");
            AddTabs();
        }
    }

    @Override
    protected void Keyword() {
        boolean closeFont = false, closeStyle = false;
        if (!htmlSettings.getAttribute("KEYWORD", "FOREGROUND").equals("null")) {
            AddHTMLElement("font", "KEYWORD", "color", "FOREGROUND");
            closeFont = true;
        }

        if (!htmlSettings.getAttribute("KEYWORD", "STYLE").equals("null")) {
            AddHTMLElement(htmlSettings.getAttribute("KEYWORD", "STYLE"), "KEYWORD");
            closeStyle = true;
        }

        AddHTMLLiteral(currentToken.getText());

        if (closeStyle) {
            CloseHTMLElement(htmlSettings.getAttribute("KEYWORD", "STYLE"));
        }

        if (closeFont) {
            CloseHTMLElement("font");
        }

        if (!(currentToken.getText().equals("main") || currentToken.getText().equals("while")
              || currentToken.getText().equals("if"))) {
            htmlPage.append(" ");
        }
    }


    public static void main(String[] args) throws IOException, InvalidFileException, ParsingException {
        XLanguageDecorator p = new XLanguageDecorator(args[1], args[0]);
        p.Run();
        System.out.println(p.htmlPage.toString());
    }

}
