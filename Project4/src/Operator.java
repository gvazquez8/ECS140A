import java.util.ArrayList;

public class Operator {
    public Operator() {
    }

    private Integer Add(Integer a, Integer b) {
        return a+b;
    }

    private Double Add(Double a, Double b) {
        return a+b;
    }

    private String Add(String a, String b) {
        return a+b;
    }

    private Integer Subtract(Integer a, Integer b) {
        return a-b;
    }

    private Double Subtract(Double a, Double b) {
        return a-b;
    }

    private Integer Multiply(Integer a, Integer b) {
        return a*b;
    }

    private Double Multiply(Double a, Double b) {
        return a*b;
    }

    private Integer Divide(Integer a, Integer b) {
        return a/b;
    }

    private Double Divide(Double a, Double b) {
        return a/b;
    }

    private Integer Mod(Integer a, Integer b) {
        return a%b;
    }

    private Double Mod(Double a, Double b) {
        return a%b;
    }

    private boolean Equal(Object a, Object b) {
        return a.equals(b);
    }

    private boolean NotEqual(Object a, Object b) {
        return !Equal(a,b);
    }

    private boolean LessThan(Integer a, Integer b) {
        return a < b;
    }

    private boolean GreaterThan(Integer a, Integer b) {
        return a > b;
    }

    private boolean LessThan(Double a, Double b) {
        return a < b;
    }

    private boolean GreaterThan(Double a, Double b) {
        return a > b;
    }

    private boolean LessThan(String a, String b) {
        return a.compareTo(b) < 0 ? true : false;
    }

    private boolean GreaterThan(String a, String b) {
        return a.compareTo(b) > 0 ? true : false;
    }

    private boolean LessThanEQ(Integer a, Integer b) {
        return LessThan(a,b) || Equal(a,b);
    }

    private boolean GreaterThanEQ(Integer a, Integer b) {
        return GreaterThan(a,b) || Equal(a,b);
    }

    private boolean LessThanEQ(Double a, Double b) {
        return LessThan(a,b) || Equal(a,b);
    }

    private boolean GreaterThanEQ(Double a, Double b) {
        return GreaterThan(a,b) || Equal(a,b);
    }

    private boolean LessThanEQ(String a, String b) {
        return LessThan(a,b) || Equal(a,b);
    }

    private boolean GreaterThanEQ(String a, String b) {
        return GreaterThan(a,b) || Equal(a,b);
    }

    public Object compute(String operator, Double leftOperand, Double rightOperand) {
        Object result = null;

        switch (operator) {
            case "=":
                result = Equal(leftOperand, rightOperand);
                break;
            case "!=":
                result = NotEqual(leftOperand, rightOperand);
                break;
            case "<":
                result = LessThan(leftOperand, rightOperand);
                break;
            case "<=":
                result = LessThanEQ(leftOperand, rightOperand);
                break;
            case ">":
                result = GreaterThan(leftOperand, rightOperand);
                break;
            case ">=":
                result = GreaterThanEQ(leftOperand, rightOperand);
                break;
            case "+":
                result = Add(leftOperand, rightOperand);
                break;
            case "-":
                result = Subtract(leftOperand, rightOperand);
                break;
            case "*":
                result = Multiply(leftOperand, rightOperand);
                break;
            case "/":
                result = Divide(leftOperand, rightOperand);
                break;
            case "%":
                result = Mod(leftOperand, rightOperand);
                break;
        }
        return result;
    }

    public Object compute(String operator, Integer leftOp, Integer rightOp) {
        return compute(operator, Double.valueOf(leftOp.toString()), Double.valueOf(rightOp.toString()));
    }

    public Object compute(String operator, String leftOperand, String rightOperand) {
        Object result = null;

        switch (operator) {
            case "=":
                result = Equal(leftOperand, rightOperand);
                break;
            case "!=":
                result = NotEqual(leftOperand, rightOperand);
                break;
            case "<":
                result = LessThan(leftOperand, rightOperand);
                break;
            case "<=":
                result = LessThanEQ(leftOperand, rightOperand);
                break;
            case ">":
                result = GreaterThan(leftOperand, rightOperand);
                break;
            case ">=":
                result = GreaterThanEQ(leftOperand, rightOperand);
                break;
            case "+":
                result = Add(leftOperand, rightOperand);
                break;
        }
        return result;
    }

    public Object computeUnary(String operator, Object operand) {
        switch (operator) {
            case "!":
                return !(Boolean)operand;
            case "-":
                return -1 * (Double)operand;
            case "+":
                return (Double)operand > 0 ? operand : -1*(Double)operand;
        }
        return null;
    }














}
