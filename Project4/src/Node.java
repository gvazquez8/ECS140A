import java.util.ArrayList;

public class Node {
    public String RuleType;
    public ArrayList<Node> Children;
    public Node Parent;
    public Token token;

    public Node(String ruleType, Token associatedToken, Node parent, ArrayList<Node> children) {
        if (ruleType == null) {
            RuleType = null;
        }
        else {
            RuleType = ruleType;
        }
        token = associatedToken;
        Parent = parent;
        if (children == null) {
            Children = new ArrayList<>();
        }
        else {
            Children = children;
        }
    }

    public void AddChild(Node Child) {
        Children.add(Child);
    }

    public void SetToken(Token token) {
        this.token = token;
    }
}
