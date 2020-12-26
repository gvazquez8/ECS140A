import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DataSet {

    public HashMap<String, Integer> headers;
    public ArrayList<String> orderedHeaders;

    public HashSet<ArrayList<Object>> data;

    public DataSet(CSVParser parser) {
        headers = new HashMap<>();
        orderedHeaders = new ArrayList<>();
        data = new HashSet<>();
        Integer index = 0;
        for (String header : parser.getHeaders()) {
            headers.put(header, index++);
            orderedHeaders.add(header);
        }
        while(!parser.reachedEOF()) {
            HashMap<String, String> row = (HashMap<String, String>) parser.getNextRow();
            ArrayList<Object> dataRow = new ArrayList<Object>(index);
            for(String header : parser.getHeaders()) {
                dataRow.add(parseString(row.get(header)));
            }
            data.add(dataRow);
        }
    }

    public DataSet(HashMap<String, Integer> headers, DataSet set) {
        this.headers = headers;

        data = new HashSet<>();
        for (ArrayList<Object> row : set.data) {
            ArrayList<Object> items = new ArrayList<>();
            for (Integer index : headers.values()) {
                items.add(row.get(index));
            }
            data.add(items);
        }
    }

    public DataSet(HashMap<String, Integer> headers, HashSet<ArrayList<Object>> data) {
        this.headers = headers;
        this.data = data;
    }

    public DataSet(ArrayList<String> headers, HashSet<ArrayList<Object>> data) {
        this.data = data;
        this.orderedHeaders = headers;
        Integer index = 0;
        this.headers = new HashMap<>();
        for (String header : headers) {
            this.headers.put(header, index++);
        }
    }

    public DataSet() {
        this.headers = new HashMap<>();
        this.data = new HashSet<>();
        this.orderedHeaders = new ArrayList<>();
    }

    public static Object parseString(String value) {
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


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (String header : orderedHeaders) {
            builder.append(header + " ");
        }
        builder.append("\n");

        for (ArrayList<Object> row : data) {
            for (Object item : row) {
                builder.append(item + " ");
            }
            builder.append("\n");
        }

        return builder.toString();
    }
}
