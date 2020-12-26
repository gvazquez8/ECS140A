import javax.xml.crypto.Data;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class NRDatalogDataLoader {

    private String dataPath;

    private HashMap<String, DataSet> dataSets = new HashMap<>();

    public NRDatalogDataLoader(String dataPath) {
        this.dataPath = dataPath;
    }

    public NRDatalogDataLoader() {
        this.dataPath = null;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public void loadData(String fact) throws IOException {
        CSVParser parser = new CSVParser(new PeekableCharacterFileStream(dataPath+"/"+fact+".csv"));
        dataSets.put(fact, new DataSet(parser));
    }

    public boolean contains(String ruleName) {
        return dataSets.keySet().contains(ruleName);
    }

    public ArrayList<String> getSetColumns(String rule) {
        return dataSets.get(rule).orderedHeaders;
    }

    public DataSet getSet(String rule) { return dataSets.get(rule); }

    public void addSet(String rule, DataSet set) {
        dataSets.put(rule, set);
    }

    public DataSet select(String rule, ArrayList<Object> columns) {
        DataSet sourceSet = dataSets.get(rule);
        ArrayList<String> newColumns = new ArrayList<>();
        for (Object col : columns) {
            if (col.toString().equals("_")) { continue; }
            newColumns.add(col.toString());
        }

        HashSet<ArrayList<Object>> modifiedSet = new HashSet<>();

        for (ArrayList<Object> row : sourceSet.data) {
            ArrayList<Object> newRow = new ArrayList<>();
            for (String col : newColumns) {
                if (!col.equals("_")) {
                    newRow.add(row.get(columns.indexOf(col)));
                }
            }
            modifiedSet.add(newRow);
        }

        return new DataSet(newColumns, modifiedSet);
    }

    public void setDataSet(String ruleName, HashSet<ArrayList<Object>> data) {
        dataSets.get(ruleName).data = data;
    }

    public DataSet select(DataSet set, ArrayList<String> columns) {
        ArrayList<String> newColumns = new ArrayList<>();
        for (Object col : columns) {
            if (col.toString().equals("_")) { continue; }
            newColumns.add(col.toString());
        }

        HashSet<ArrayList<Object>> modifiedSet = new HashSet<>();

        for (ArrayList<Object> row : set.data) {
            ArrayList<Object> newRow = new ArrayList<>();
            for (String col : newColumns) {
                if (!col.equals("_")) {
                    newRow.add(row.get(columns.indexOf(col)));
                }
            }
            modifiedSet.add(newRow);
        }

        return new DataSet(newColumns, modifiedSet);
    }

    public DataSet cartesianProd(DataSet A, DataSet B) {
        ArrayList<String> resultHeaders = new ArrayList<>(A.orderedHeaders);
        resultHeaders.addAll(B.orderedHeaders);

        HashSet<ArrayList<Object>> data = new HashSet<>();
        for (ArrayList<Object> rowA : A.data) {
            for (ArrayList<Object> rowB : B.data) {
                ArrayList<Object> newRow = new ArrayList<>(rowA);
                newRow.addAll(rowB);
                data.add(newRow);
            }
        }
        return new DataSet(resultHeaders, data);
    }

    public DataSet naturalJoin(DataSet A, DataSet B) {
        ArrayList<String> commonColumns = new ArrayList<>();
        HashMap<String, Integer> AHeaders = A.headers;
        HashMap<String, Integer> BHeaders = B.headers;
        ArrayList<String> ResultHeaders = new ArrayList<>(A.orderedHeaders);


        // Start collecting common columns.
        for (Map.Entry entry : AHeaders.entrySet()) {
            if (BHeaders.containsKey(entry.getKey())) {
                commonColumns.add((String)entry.getKey());
            }
        }

        for (String key : BHeaders.keySet()) {
            if (!ResultHeaders.contains(key)) {
                ResultHeaders.add(key);
            }
        }

        // Collect Valid Entries
        HashSet<ArrayList<Object>> validEntriesOfB = new HashSet<>();
        for (ArrayList<Object> rowB : B.data) {
            ArrayList<Object> newEntry = new ArrayList<>();
            for (String col : commonColumns) {
                newEntry.add(rowB.get(B.headers.get(col)));
            }
            validEntriesOfB.add(newEntry);
        }

        // Start appending valid rows to set.
        HashSet<ArrayList<Object>> newSetData = new HashSet<>();

        for (ArrayList<Object> rowA : A.data) {
            // Get entries of common columns
            ArrayList<Object> entriesOfCommonCol_A = new ArrayList<>();
            for (String col : commonColumns) {
                entriesOfCommonCol_A.add(rowA.get(A.headers.get(col)));
            }

            // if that entry is in valid entry
            if (validEntriesOfB.contains(entriesOfCommonCol_A)) {
                for (ArrayList<Object> rowB : B.data) {
                    ArrayList<Object> entriesOfCommonCol_B = new ArrayList<>();
                    for (String col : commonColumns) {
                        entriesOfCommonCol_B.add(rowB.get(B.headers.get(col)));
                    }
                    if (entriesOfCommonCol_B.equals(entriesOfCommonCol_A)) {
                        ArrayList<Object> newRow = new ArrayList<>(rowA);
                        for (String key : ResultHeaders) {
                            if (BHeaders.keySet().contains(key) && !AHeaders.keySet().contains(key)) {
                                newRow.add(rowB.get(BHeaders.get(key)));
                            }
                        }
                        newSetData.add(newRow);
                    }
                }
            }
        }

        return new DataSet(ResultHeaders, newSetData);
    }

    public DataSet union(DataSet A, DataSet B) {
        HashSet<ArrayList<Object>> unionedData = new HashSet<>(A.data);
        ArrayList<String> headers = new ArrayList<>(A.orderedHeaders);

        for (ArrayList<Object> arr : B.data) {
            unionedData.add(new ArrayList<>(arr));
        }

        return new DataSet(headers, unionedData);
    }

    public DataSet reorder(ArrayList<String> orderedHeaders, DataSet A) {
        HashMap<String, Integer> oldHeaders = A.headers;

        HashSet<ArrayList<Object>> updatedSet = new HashSet<>();

        for (ArrayList<Object> row : A.data) {
            ArrayList<Object> arr = new ArrayList<>();
            for (String header : orderedHeaders) {
                arr.add(row.get(oldHeaders.get(header)));
            }
            updatedSet.add(arr);
        }

        return new DataSet(orderedHeaders, updatedSet);
    }

    public DataSet difference(DataSet A, DataSet B) {
        HashSet<ArrayList<Object>> differenceData = new HashSet<>(A.data);
        ArrayList<String> BHeaders = B.orderedHeaders;

        for (ArrayList<Object> rowA : A.data) {
            for (ArrayList<Object> rowB : B.data) {
                boolean shouldRemove = true;
                for (String header : BHeaders) {
                    if (!rowA.get(A.headers.get(header)).equals(rowB.get(B.headers.get(header)))) {
                        shouldRemove = false;
                        break;
                    }
                }
                if (shouldRemove) {
                    differenceData.remove(rowA);
                }
            }
        }

        return new DataSet(A.orderedHeaders, differenceData);
    }

    public boolean hasColumnsInCommon(ArrayList<String> headers_A, ArrayList<String> headers_B) {
        for (String c : headers_A) {
            if (headers_B.contains(c)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        NRDatalogDataLoader loader = new NRDatalogDataLoader("./testData");

        loader.loadData("Names");
        loader.loadData("People");

        DataSet A = loader.dataSets.get("Names");
        DataSet B = loader.dataSets.get("People");
        ArrayList<String> arr = new ArrayList<>();
        arr.add("c");
        arr.add("a");
        arr.add("b");
//        System.out.println("Testing reordering...");
//        System.out.println(loader.reorder(arr, A));

//        System.out.println("Testing Cartesian product...");
//        System.out.println(loader.cartesianProd(A,B));

//        System.out.println("Testing Natural Join");
//        System.out.println(loader.naturalJoin(A,B));

//        System.out.println("Testing union");
//        System.out.println(loader.union(A,B));
/*
        System.out.println("Testing difference..");
        ArrayList<String> selectCols = new ArrayList<>();
        selectCols.add("name");
        selectCols.add("a");
        selectCols.add("_");
        ArrayList<String> selectCols2 = new ArrayList<>();
        selectCols2.add("_");
        selectCols2.add("a");
        selectCols2.add("_");
        System.out.println(loader.difference(loader.select(B,selectCols),loader.select(A, selectCols2)));
 */
    }
}
