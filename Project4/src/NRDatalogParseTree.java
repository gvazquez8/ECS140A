import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class NRDatalogParseTree extends NRDatalogParser{

    protected Node Tree;
    private Node CurrentNode;
    public NRDatalogParseTree(PeekableCharacterStream stream) {
        super(stream);
    }

    // Constant := IntConstant | FloatConstant | StringConstant
    @Override
    public boolean parseQuery() {
        return super.parseQuery();
    }

    @Override
    public void setError(Exception e) {
        super.setError(e);
    }

    @Override
    protected void Query() throws ParsingException {
        // Query := Rule { Rule }
        Tree = new Node("QUERY", null, null, null);
        CurrentNode = Tree;
        super.Query();
    }

    @Override
    protected void Rule() throws ParsingException {
        // Rule := [ RuleHead [ := RuleBody ] ] NewLine
        if (super.currentToken.getText() == "\n") {
            super.Rule();
            return;
        }

        Node n = new Node("RULE", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.Rule();
        CurrentNode = n.Parent;
    }

    @Override
    protected void RuleHead() throws ParsingException {
        // RuleHead := RuleName ( HeadVariableList )
        Node n = new Node("RULE_HEAD", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.RuleHead();
        CurrentNode = n.Parent;
    }

    @Override
    protected void RuleBody() throws ParsingException {
        // RuleBody := SubGoal { AND SubGoal }
        Node n = new Node("RULE_BODY", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.RuleBody();
        CurrentNode = n.Parent;
    }

    @Override
    protected void RuleName() throws ParsingException {
        // RuleName := Identifier
        Node n = new Node("RULE_NAME", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.RuleName();
        CurrentNode = n.Parent;
    }

    @Override
    protected void HeadVariableList() throws ParsingException {
        // HeadVariableList := Identifier { , Identifier }
        Node n  = new Node("HEAD_VARIABLE_LIST", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.HeadVariableList();
        CurrentNode = n.Parent;
    }

    @Override
    protected void SubGoal() throws ParsingException {
        // SubGoal := RuleInvocation | NegatedRuleInvocation | EqualityRelation
        Node n = new Node("SUBGOAL", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.SubGoal();
        CurrentNode = n.Parent;
    }

    @Override
    protected void RuleInvocation() throws ParsingException {
        // RuleInvocation := RuleName ( BodyVariableList )
        Node n = new Node("RULE_INVOCATION", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.RuleInvocation();
        CurrentNode = n.Parent;
    }

    @Override
    protected void NegatedRuleInvocation() throws ParsingException {
        // NegatedRuleInvocation := NOT RuleInvocation
        Node n = new Node("NEGATED_RULE_INVOCATION", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.NegatedRuleInvocation();
        CurrentNode = n.Parent;
    }

    @Override
    protected void EqualityRelation() throws ParsingException {
        // EqualityRelation := InequalityRelation { EQOperator InequalityRelation }
        Node n = new Node("EQUALITY_RELATION", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.EqualityRelation();
        CurrentNode = n.Parent;
    }

    @Override
    protected void BodyVariableList() throws ParsingException {
        // BodyVariableList := InvocationVariable { , InvocationVariable }
        Node n = new Node("BODY_VARIABLE_LIST", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.BodyVariableList();
        CurrentNode = n.Parent;
    }

    @Override
    protected void InequalityRelation() throws ParsingException {
        // InequalityRelation := Term { IEQOperator Term }
        Node n = new Node("INEQUALITY_RELATION", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.InequalityRelation();
        CurrentNode = n.Parent;
    }

    @Override
    protected void InvocationVariable() throws ParsingException {
        // InvocationVariable := Identifier | EmptyIdentifier
        Node n = new Node("INVOCATION_VARIABLE", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.InvocationVariable();
        CurrentNode = n.Parent;
    }

    @Override
    protected void Term() throws ParsingException {
        // Term := SimpleTerm { AddOperator SimpleTerm }
        Node n = new Node("TERM", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.Term();
        CurrentNode = n.Parent;
    }

    @Override
    protected void SimpleTerm() throws ParsingException {
        // SimpleTerm := UnaryExpression { MultOperator UnaryExpression } AddOperator := + | -
        Node n = new Node("SIMPLE_TERM", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.SimpleTerm();
        CurrentNode = n.Parent;
    }

    @Override
    protected void UnaryExpression() throws ParsingException {
        // UnaryExpression := ( UnaryOperator UnaryExpression ) | PrimaryExpression
        Node n = new Node("UNARY_EXPRESSION", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.UnaryExpression();
        CurrentNode = n.Parent;
    }

    @Override
    protected void PrimaryExpression() throws ParsingException {
        // PrimaryExpression:= ( EqualityRelation ) | Constant | Identifier
        Node n = new Node("PRIMARY_EXPRESSION", null, CurrentNode, null);
        CurrentNode.AddChild(n);
        CurrentNode = n;
        super.PrimaryExpression();
        CurrentNode = n.Parent;
    }

    @Override
    protected void Constant() throws ParsingException {
        CurrentNode.AddChild(new Node(null, super.currentToken, CurrentNode, null));
        super.Constant();
    }

    @Override
    protected void Identifier() {
        CurrentNode.AddChild(new Node(null, super.currentToken, CurrentNode, null));
        super.Identifier();
    }

    @Override
    protected void Operator() {
        CurrentNode.AddChild(new Node(null, super.currentToken, CurrentNode, null));
        super.Operator();
    }

    @Override
    protected void Keyword() {
        CurrentNode.AddChild(new Node(null, super.currentToken, CurrentNode, null));
        super.Keyword();
    }

    @Override
    public void printError(PrintStream ostream) {
        super.printError(ostream);
    }

    public void outputParseTree(PrintStream ostream) {
        printRule(ostream, "", Tree);
    }

    private void printRule(PrintStream ostream, String indent, Node root) {
        if (root.RuleType != null) {
            ostream.println(indent+"("+root.RuleType);

            for (Node child : root.Children) {
                printRule(ostream, indent+"\t", child);
            }
            ostream.println(indent+")");
        }
        else {
            ostream.println(indent+root.token.getText());
        }
    }

    public static void main(String[] args) throws IOException {
        NRDatalogParseTree parseTree = new NRDatalogParseTree(new PeekableCharacterFileStream("./queries/query_1.nrdl"));

        parseTree.parseQuery();
        parseTree.outputParseTree(System.out);
    }

}
