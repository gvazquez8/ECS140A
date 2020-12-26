import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class NRDatalogExecutionTree extends NRDatalogParseTree {

    private static List<String> validRules = Arrays.asList(
            "QUERY", "RULE", "RULE_HEAD", "RULE_BODY", "RULE_INVOCATION", "NEGATED_RULE_INVOCATION",
            "EQUALITY_RELATION","INEQUALITY_RELATION", "TERM", "SIMPLE_TERM", "UNARY_EXPRESSION");

    private class SemanticError extends Exception {
        public SemanticError(String prefix) {
            super(prefix);
        }
    }
    private class ExecutionNode {
        public String rule;
        public String name;
        public ArrayList<ExecutionNode> Children = new ArrayList<>();
        public ArrayList<Object> LeafChildren = new ArrayList<>();

        public ExecutionNode() {
            rule = "INVALID";
            this.name = "";
        }

        public void AddNode(ExecutionNode n) {
            Children.add(n);
        }

        public void AddLeafChild(Object item) {
            LeafChildren.add(item);
        }

        public void AddLeafChild(Token token) {
            LeafChildren.add(parseString(token.getText()));
        }

        public Object parseString(String value) {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException e) {
                try {
                    return Double.parseDouble(value);
                }
                catch (NumberFormatException e2) {
                    return value;
                }
            }
        }
    }

    private boolean verbose = false;
    private int threadCount = 1;
    private NRDatalogDataLoader loader;
    private ExecutionNode executionTree = new ExecutionNode();
    private SemanticError e;

    public NRDatalogExecutionTree(PeekableCharacterStream stream) {
        super(stream);
        loader = new NRDatalogDataLoader();
    }

    @Override
    public void setError(Exception e) {
        super.setError(e);
    }

    public boolean parseQuery() {
        if (super.parseQuery()) {
            Node parseTreeNode = super.Tree;
            convertParseNodeToExecutionNode(executionTree, parseTreeNode);
            sortSubGoals();
            try {
                return validateExecutionTree();
            }
            catch (SemanticError e) {
                this.e = e;
                return false;
            }
        }
        else {
            return false;
        }
    }

    private void convertParseNodeToExecutionNode(ExecutionNode executionRoot, Node parseRoot) {
        boolean stopRecursing = false;
        String currentRule = parseRoot.RuleType;

        if (validRules.contains(currentRule)) {
            executionRoot.rule = currentRule;
            switch (currentRule) {
                case "RULE":
                    if (parseRoot.Children.get(0).token != null) {
                        executionRoot.rule = "INVALID";
                    }
                    break;
                case "RULE_HEAD":
                    stopRecursing = true;
                    executionRoot.name = parseRoot.Children.get(0).Children.get(0).token.getText();

                    for (Node child : parseRoot.Children.get(2).Children) {
                        if (child.token.getText().equals(",")) { continue; }
                        executionRoot.AddLeafChild(child.token);
                    }
                    break;
                case "RULE_BODY":
                    stopRecursing = true;
                    for (Node child : parseRoot.Children) {
                        if (child.RuleType != null) {
                            ExecutionNode childExecNode = new ExecutionNode();
                            convertParseNodeToExecutionNode(childExecNode, child.Children.get(0));
                            executionRoot.AddNode(childExecNode);
                        }
                    }
                    break;
                case "RULE_INVOCATION":
                    stopRecursing = true;
                    executionRoot.name = parseRoot.Children.get(0).Children.get(0).token.getText();

                    for (Node invocationVar : parseRoot.Children.get(2).Children) {
                        if (invocationVar.RuleType != null) {
                            executionRoot.AddLeafChild(invocationVar.Children.get(0).token);
                        }
                    }
                    break;
                case "NEGATED_RULE_INVOCATION":
                    stopRecursing = true;
                    convertParseNodeToExecutionNode(executionRoot, parseRoot.Children.get(1));
                    executionRoot.rule = currentRule;
                    break;
                case "EQUALITY_RELATION":
                case "INEQUALITY_RELATION":
                case "TERM":
                case "SIMPLE_TERM":
                    stopRecursing = true;
                    ArrayList<ExecutionNode> resultingChildren = new ArrayList<>();
                    ArrayList<String> Operators = new ArrayList<>();

                    // For every child, reduce it down to its most basic execution tree form. If its an operator append to op list.
                    for (Node child : parseRoot.Children) {
                        if (child.RuleType != null) {
                            ExecutionNode execChild = new ExecutionNode();
                            resultingChildren.add(execChild);
                            convertParseNodeToExecutionNode(execChild, child);
                        }
                        else {
                            Operators.add(child.token.getText());
                        }
                    }

                    // if there is only one child that was generated, add it to the root of iteration.
                    // If it is a single leaf, just propogate it up.
                    // if more than a leaf, connect leafs into one.
                    if (resultingChildren.size() == 1 && Operators.size() == 0) {
                        if (resultingChildren.get(0).LeafChildren.size() > 1) {
                            executionRoot.rule = resultingChildren.get(0).rule;
                            for (Object child : resultingChildren.get(0).LeafChildren) {
                                executionRoot.AddLeafChild(child);
                            }
                        }
                        else {
                            executionRoot.rule = resultingChildren.get(0).rule;
                            executionRoot.AddLeafChild(resultingChildren.get(0).LeafChildren.get(0));
                        }
                    }
                    else {
                        // there are multiple children and multiple operations at current level.
                        // For every operater, connect children to form roots. (left -> right)
                        // the final node created will hold the actual execution tree root.
                        for (String op : Operators) {
                            ExecutionNode newNode = new ExecutionNode();
                            newNode.rule = op;

                            if (validRules.contains(resultingChildren.get(0).rule)) {
                                newNode.AddLeafChild(resultingChildren.get(0).LeafChildren.get(0));
                            }
                            else {
                                newNode.AddLeafChild(resultingChildren.get(0));
                            }

                            if (validRules.contains(resultingChildren.get(1).rule)) {
                                newNode.AddLeafChild(resultingChildren.get(1).LeafChildren.get(0));
                            }
                            else {
                                newNode.AddLeafChild(resultingChildren.get(1));
                            }

                            resultingChildren.remove(1);
                            resultingChildren.remove(0);
                            resultingChildren.add(0, newNode);
                        }
                        executionRoot.rule = resultingChildren.get(0).rule;
                        for (Object leaf : resultingChildren.get(0).LeafChildren) {
                            executionRoot.AddLeafChild(leaf);
                        }
                    }
                    break;
                case "UNARY_EXPRESSION":
                    stopRecursing = true;
                    Node firstElm = parseRoot.Children.get(0);
                    if (firstElm.RuleType != null) {
                        if (firstElm.Children.size() > 1) {
                            ExecutionNode execChild = new ExecutionNode();
                            convertParseNodeToExecutionNode(execChild, firstElm.Children.get(1));
                            executionRoot.rule = execChild.rule;
                            for (Object leaf : execChild.LeafChildren) {
                                executionRoot.AddLeafChild(leaf);
                            }

                        }
                        else {
                            executionRoot.AddLeafChild(DataSet.parseString(firstElm.Children.get(0).token.getText()));
                        }
                    }
                    else {
                        String operator = firstElm.token.getText();
                        ExecutionNode execChild = new ExecutionNode();
                        convertParseNodeToExecutionNode(execChild, parseRoot.Children.get(1));
                        executionRoot.rule = operator;
                        if (execChild.LeafChildren.size() == 1) {
                            executionRoot.AddLeafChild(execChild.LeafChildren.get(0));
                        }
                        else {
                            executionRoot.AddLeafChild(execChild);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        if (!stopRecursing) {
            for (Node child : parseRoot.Children) {
                ExecutionNode childExecNode = new ExecutionNode();
                executionRoot.AddNode(childExecNode);
                convertParseNodeToExecutionNode(childExecNode, child);
                if (childExecNode.rule.equals("INVALID")) {
                    executionRoot.Children.remove(childExecNode);
                }
            }
        }


    }

    private void sortSubGoals() {
        ArrayList<ExecutionNode> rules = executionTree.Children;

        for (ExecutionNode rule : rules) {
            if (rule.Children.size() > 1) {
                ExecutionNode body = rule.Children.get(1);
                ArrayList<ExecutionNode> subGoals = body.Children;
                ArrayList<ExecutionNode> invocations = new ArrayList<>();
                ArrayList<ExecutionNode> negatedInvocations = new ArrayList<>();
                ArrayList<ExecutionNode> eqRelations = new ArrayList<>();
                for (ExecutionNode subGoal : subGoals) {
                    if (subGoal.rule.equals("RULE_INVOCATION")) {
                        invocations.add(subGoal);
                    }
                    else if (subGoal.rule.equals("NEGATED_RULE_INVOCATION")) {
                        negatedInvocations.add(subGoal);
                    }
                    else {
                        eqRelations.add(subGoal);
                    }
                }

                subGoals.clear();

                subGoals.addAll(invocations);
                subGoals.addAll(negatedInvocations);
                subGoals.addAll(eqRelations);
            }
        }
    }

    private boolean validateExecutionTree() throws SemanticError {
        ArrayList<String> ruleHistory = new ArrayList<>();
        ArrayList<ArrayList<Object>> ruleArgs = new ArrayList<>();

        ArrayList<ExecutionNode> rules = executionTree.Children;
        /*
            • All subgoals must be defined prior to their use (done)
            • All repeated rule names must be defined contiguously (done)
            • All repeated rule names must have the same number of variables (done)
            • Any variable in the rule head or rule body must appear in a non-negated rule invocation (done)
            • A rule invocation may not appear in its own rule definition (i.e. recursive definition) (done)
            • Fact rules may only appear once (done)
            • Every rule invocation must provide the same number of variables as its definition (done)
        */
        for (ExecutionNode rule : rules) {
            String ruleName = rule.Children.get(0).name;
            if (rule.Children.size() > 1) {
                // used for checking safe/unsafe variables
                HashSet<String> unsafeVars= new HashSet<>();
                HashSet<String> safeVars = new HashSet<>();
                for (Object arg : rule.Children.get(0).LeafChildren) {
                    unsafeVars.add((String)arg);
                }

                // make sure rule is defined contiguously
                if (ruleHistory.contains(ruleName) && !ruleHistory.get(ruleHistory.size()-1).equals(ruleName)) {
                    throw new SemanticError("Duplicate Non-sequential Rule \""+ruleName+"\"");
                }

                // make sure head var list are matching sizes if it exists already.
                if (ruleHistory.contains(ruleName)) {
                    int numVarList = ruleArgs.get(ruleHistory.indexOf(ruleName)).size();
                    if (numVarList != rule.Children.get(0).LeafChildren.size()) {
                        throw new SemanticError("Redeclared Rule Variable Count Mismatch \"" + ruleName + "\"");
                    }
                }

                for (ExecutionNode subgoal : rule.Children.get(1).Children) {
                    String subGoalName = subgoal.name;
                    // Make sure there is no recursive call.
                    if (subGoalName.equals(ruleName)) {
                        throw new SemanticError("Recursive Rule Invocation \""+subGoalName+"\"");
                    }

                    // Make sure rule exists
                    if (subgoal.rule.equals("RULE_INVOCATION") || subgoal.rule.equals("NEGATED_RULE_INVOCATION")) {
                        if (!ruleHistory.contains(subGoalName)) {
                            throw new SemanticError("Undeclared Rule \""+subGoalName+"\"");
                        }
                    }

                    // make sure head var list are matching sizes.
                    if (ruleHistory.contains(subGoalName)) {
                        int numVarList = ruleArgs.get(ruleHistory.indexOf(subGoalName)).size();
                        if (numVarList != subgoal.LeafChildren.size()) {
                            throw new SemanticError("Rule Invocation Variable Count Mismatch \"" + subGoalName + "\"");
                        }
                    }

                    // check if all variables are in a non-negated rule invocation
                    if (subgoal.rule.equals("RULE_INVOCATION")) {
                        for (Object arg : subgoal.LeafChildren) {
                            if (arg.equals("_")) { continue; }
                            if (unsafeVars.contains(arg)) {
                                unsafeVars.remove(arg);
                            }
                            safeVars.add((String)arg);
                        }
                    }
                    else if (subgoal.rule.equals("NEGATED_RULE_INVOCATION")){
                        for (Object arg : subgoal.LeafChildren) {
                            if (arg.equals("_")) { continue; }
                            if (!safeVars.contains(arg)) {
                                throw new SemanticError("Unsafe Variable \""+arg.toString()+"\"");
                            }
                        }
                    }
                    else {
                        checkIfUnsafeVarPresent(subgoal, safeVars);
                    }
                }
                ruleHistory.add(ruleName);
                ruleArgs.add(rule.Children.get(0).LeafChildren);

            }
            else {
                // current rule is a fact, check if it gets called more than once.
                if (ruleHistory.contains(ruleName)) {
                    throw new SemanticError("Redeclared External Rule \"" + ruleName + "\"");
                }
                else {
                    ruleHistory.add(ruleName);
                    ruleArgs.add(rule.Children.get(0).LeafChildren);
                }
            }
        }
        return true;
    }

    private void checkIfUnsafeVarPresent(ExecutionNode root, HashSet<String> safeVars) throws SemanticError {

        for (Object leaf : root.LeafChildren) {
            if (leaf instanceof ExecutionNode) {
                checkIfUnsafeVarPresent((ExecutionNode)leaf, safeVars);
            }
            else if (leaf instanceof String) {
                if (leaf instanceof String) {
                    if (((String)leaf).charAt(0) == '\"') {
                        String s = (String) leaf;
                        s = s.substring(1, s.length()-1);
                        root.LeafChildren.set(root.LeafChildren.indexOf(leaf), s);
                        continue;
                    }
                }
                if (!safeVars.contains(leaf)) {
                    throw new SemanticError("Unsafe Variable \""+leaf.toString()+"\"");
                }
            }
        }
    }

    public void printError(PrintStream ostream) {
        if (e == null) {
            super.printError(ostream);
        }
        else {
            ostream.println(e);
        }
    }

    public void outputExecutionTree(PrintStream ostream) { printRule(ostream, "", executionTree); }

    public void setVerbose(boolean verb) { verbose = verb; }

    public void setThreadCount(int threadcount) { threadCount = threadcount; }

    public void setDataPath(String datapath) { loader.setDataPath(datapath); }

    public boolean executeQuery() throws IOException {
        ArrayList<ExecutionNode> rules = executionTree.Children;
        DataSet last = null;
        String lastRuleName = "";
        for (ExecutionNode rule : rules) {

            lastRuleName = rule.Children.get(0).name;
            if (rule.Children.size() == 1) { // rule is a fact
                last = executeFact(rule);
            }
            else {
                last = executeBody(rule);

                // if the rule already exists, union with it
                if (loader.contains(lastRuleName)) {
                    last = loader.union(loader.getSet(lastRuleName), last);
                    loader.addSet(lastRuleName, last);
                }
                else {
                    loader.addSet(lastRuleName, last);
                }
            }
        }

        if (last != null) {
            System.out.println("Results \""+lastRuleName+"\"");
            System.out.println(last);
        }
        return true;
    }

    public DataSet executeBody(ExecutionNode rule) {
        ArrayList<Object> finalColumns = rule.Children.get(0).LeafChildren;

        DataSet temporarySet = null;
        for (ExecutionNode subGoal : rule.Children.get(1).Children) {
            ArrayList<Object> columns = subGoal.LeafChildren;
            String setName = subGoal.name;
            if (temporarySet == null) {
                temporarySet = loader.select(setName, columns);
            }
            else {
                if (subGoal.rule.equals("NEGATED_RULE_INVOCATION")) {
                    DataSet tempSet2 = loader.select(setName, columns);
                    temporarySet = loader.difference(temporarySet, tempSet2);
                }
                else if (subGoal.rule.equals("RULE_INVOCATION")) {
                    DataSet tempSet2 = loader.select(setName, columns);

                    if (loader.hasColumnsInCommon(temporarySet.orderedHeaders, tempSet2.orderedHeaders)) {
                        temporarySet = loader.naturalJoin(temporarySet, tempSet2);
                    }
                    else {
                        temporarySet = loader.cartesianProd(temporarySet, tempSet2);
                    }
                }
                else {
                    // do something with equality relations
                    temporarySet = filter(temporarySet, subGoal);
                }
            }
        }

        ArrayList<String> bodyHeaders = new ArrayList<>(temporarySet.orderedHeaders);

        for (int i = 0; i < bodyHeaders.size(); i++) {
            if (!finalColumns.contains(bodyHeaders.get(i))) {
                bodyHeaders.set(i, "_");
            }
        }

        temporarySet = loader.select(temporarySet, bodyHeaders);

        return temporarySet;
    }

    public DataSet filter(DataSet set, ExecutionNode relation) {
        DataSet filteredData = new DataSet();
        filteredData.orderedHeaders = set.orderedHeaders;
        filteredData.data = new HashSet<>();

        for (ArrayList<Object> row : set.data) {
            if ((boolean)computeFilter(row, set.orderedHeaders, relation)) {
                filteredData.data.add(row);
            }
        }

        return filteredData;
    }

    public Object computeFilter(ArrayList<Object> row, ArrayList<String> headers, ExecutionNode relation) {
        Operator op = new Operator();

        Object leftResult = null;
        Object rightResult = null;

        if (relation.LeafChildren.size() == 1) {
            leftResult = relation.LeafChildren.get(0);
            String operator = relation.rule;
            if (leftResult instanceof Integer) {
                return op.computeUnary(operator, (Integer)leftResult);
            }
            else if (leftResult instanceof Double) {
                return op.computeUnary(operator, (Double)leftResult);
            }
            else if (leftResult instanceof Boolean) {
                return op.computeUnary(operator, (Boolean)leftResult == true ? 1 : 0);
            }
            else {
                if (headers.indexOf(leftResult) == -1) {
                    throw new ArithmeticException("INVALID UNARY VALUE");
                }
                else {
                    Object value = row.get(headers.indexOf(leftResult));
                    return op.computeUnary(operator, value);
                }
            }

        }

        Object leftChild = relation.LeafChildren.get(0);
        Object rightChild = relation.LeafChildren.get(1);

        if (leftChild instanceof ExecutionNode) {
            leftResult = computeFilter(row, headers, (ExecutionNode)leftChild);
        }
        else {
            leftResult = leftChild;
        }

        if (rightChild instanceof ExecutionNode) {
            rightResult = computeFilter(row, headers, (ExecutionNode)rightChild);
        }
        else {
            rightResult = rightChild;
        }

        if (headers.indexOf(leftResult) != -1) {
            leftResult = row.get(headers.indexOf(leftResult));
        }

        if (headers.indexOf(rightResult) != -1) {
            rightResult = row.get(headers.indexOf(rightResult));
        }

        // Convert booleans to 1 or 0.
        if (leftResult instanceof Boolean) {
            leftResult = (Boolean)leftResult == true ? 1 : 0;
        }

        if (rightResult instanceof Boolean) {
            rightResult = (Boolean)rightResult == true ? 1 : 0;
        }

        // Now that we dealt with booleans, compute values
        if (leftResult instanceof Double || rightResult instanceof Double) {
            return op.compute(relation.rule, Double.valueOf(leftResult.toString()), Double.valueOf(rightResult.toString()));
        }
        else if (leftResult instanceof Integer && rightResult instanceof Integer) {
            return op.compute(relation.rule, (Integer)leftResult, (Integer)rightResult);
        }
        else if ((leftResult instanceof String && !(rightResult instanceof String)) || (!(leftResult instanceof String) && (rightResult instanceof String))){
            throw new ArithmeticException("Can't operate on a string and non-string");
        }
        else {
            return op.compute(relation.rule, (String)leftResult, (String)rightResult);
        }
    }

    public DataSet executeFact(ExecutionNode rule) throws IOException {
        String fact = rule.Children.get(0).name;
        ArrayList<Object> columns = rule.Children.get(0).LeafChildren;

        loader.loadData(fact);

        ArrayList<String> sourceColumns = loader.getSetColumns(fact);

        for (Object col : columns) {
            if (!sourceColumns.contains(col)) {
                throw new IllegalArgumentException("Unknown column \""+col.toString()+"\" while loading data.");
            }
        }

        return loader.getSet(fact);
    }

    private void printRule(PrintStream ostream, String indent, ExecutionNode root) {
        if (root.rule != null) {
            ostream.println(indent+"("+root.rule+" "+root.name);

            for (ExecutionNode child : root.Children) {
                printRule(ostream, indent+"\t", child);
            }
            for (Object leaf : root.LeafChildren) {
                if (leaf instanceof ExecutionNode) {
                    printRule(ostream, indent+"\t", (ExecutionNode)leaf);
                }
                else {
                    ostream.println(indent + "\t" + leaf.toString());
                }
            }
            ostream.println(indent+")");
        }
    }

    public static void main(String[] args) throws IOException {
        NRDatalogExecutionTree execTree = new NRDatalogExecutionTree(new PeekableCharacterFileStream("./queries/query_1.nrdl"));
        execTree.setDataPath("./data");
        if (execTree.parseQuery()) {
            execTree.outputExecutionTree(System.out);
            execTree.executeQuery();
        }
        else {
            execTree.printError(System.out);
        }
    }
}
