import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XHTMLSettings {

    private CSVParser parser;

    private Map<String, Map<String,String>> formatSettings = new HashMap<>();

    public XHTMLSettings(String fileURL) throws IOException {
        parser = new CSVParser(new PeekableCharacterFileStream(fileURL));
        while (!parser.reachedEOF()) {
            Map<String, String> nextMap = parser.getNextRow();
            formatSettings.put(nextMap.get("ELEMENT_TYPE"), nextMap);
        }
    }

    public String getAttribute(String elementType, String attributeName) {
        if (formatSettings.containsKey(elementType)) {
            if (!formatSettings.get(elementType).get(attributeName).equals("null")) {
                return formatSettings.get(elementType).get(attributeName);
            }
        }
        return formatSettings.get("DEFAULT").get(attributeName);
    }

    public Map<String, String> getAttributes(String elementType) {
        if (formatSettings.containsKey(elementType)) {
            return formatSettings.get(elementType);
        }
        else {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        XHTMLSettings settings = new XHTMLSettings("./format.csv");
        String[] a = new String[]{"DEFAULT", "FUNCTION", "VARIABLE", "FLOAT_CONSTANT", "INT_CONSTANT", "OPERATOR", "KEYWORD"};
        for (String s : a) {
            System.out.println(settings.getAttributes(s).toString());
        }
    }
}
