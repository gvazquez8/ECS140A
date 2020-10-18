import java.util.*;

public class CSVParser {

    private static char NEWLINE = '\n'; // Row delimiter
    private static char COMMA = ','; // Column delimiter

    // These characters must be within quotes
    private static char TAB = '\t';
    private static char CR = '\r';
    private static char SPACE = ' ';

    private ArrayList<String> headers = new ArrayList<String>();
    private Map<String, String> peekedRow = null;

    private PeekableCharacterFileStream stream;

    public CSVParser(PeekableCharacterStream stream) throws RuntimeException {
        this.stream = (PeekableCharacterFileStream) stream;

        char nextCharPeeked = (char)this.stream.peekNextChar();
        String columnName;

        while (stream.moreAvailable() && nextCharPeeked != NEWLINE) {
            columnName = getNextColumnValue();

            if (columnName.isEmpty()) {
                throw new RuntimeException("Header contains empty column name");
            }
            else if (headers.contains(columnName)) {
                throw new RuntimeException("Duplicate column name found in header: " + columnName);
            }
            else {
                headers.add(columnName);
            }
            nextCharPeeked = (char)this.stream.peekNextChar();
        }

        if (headers.isEmpty()) {
            throw new RuntimeException("Empty CSV File");
        }

        if (nextCharPeeked == NEWLINE) {
            this.stream.getNextChar(); // Move to next line
        }
    }

    /**
     *
     * @return the value in the next column.
     * @throws RuntimeException if whitespace isn't in double quotes
     */
    private String getNextColumnValue() {
        StringBuilder builder = new StringBuilder();
        char nextCharPeeked = (char)stream.peekNextChar();
        boolean allowWhiteSpace = false;

        if (nextCharPeeked == '\"') {
            allowWhiteSpace = true;
            stream.getNextChar(); // consume the double quote
            nextCharPeeked = (char)stream.peekNextChar();
        }

        while (stream.moreAvailable()) {

            if (nextCharPeeked == COMMA) {
                stream.getNextChar(); // Consume the comma character and start at the new word.
                break;
            }
            else if (nextCharPeeked == NEWLINE && !allowWhiteSpace) {
                break;
            }
            else if ((nextCharPeeked == CR || nextCharPeeked == SPACE || nextCharPeeked == TAB) && !allowWhiteSpace) {
                String errorText = "Whitespace on line " + stream.getCurrentRowIndex() + " @ position " + stream.getCurrentColIndex() + " must be quoted.";
                throw new RuntimeException(errorText);
            }
            else if (nextCharPeeked == '\"') {
                allowWhiteSpace = false;
                stream.getNextChar();
            }
            else {
                builder.append((char)stream.getNextChar());
            }

            nextCharPeeked = (char)stream.peekNextChar();
        }

        return builder.toString();
    }

    /**
     *
     * @return the next row in the CSV File without consuming it.
     */
    public Map<String, String> peekNextRow() {
        peekedRow = getNextRow();
        return peekedRow;
    }

    /**
     *
     * @return true if CSVParser has reached EOF
     */
    public boolean reachedEOF() {
        if (peekedRow == null) {
            return !stream.moreAvailable();
        }

        return false;
    }

    /**
     *
     * @return the next row in the CSV File.
     */
    public Map<String, String> getNextRow() {
        if (reachedEOF()) {
            return null;
        }

        Map<String, String> nextRow = new HashMap<String, String>();

        if (peekedRow != null) {
            nextRow = peekedRow;
            peekedRow = null;
            return nextRow;
        }

        boolean reachEndOfRow = false;
        char nextCharPeeked = (char)stream.peekNextChar();

        String columnValue;
        for (int i = 0; i < headers.size(); i++) {

            if (nextCharPeeked == NEWLINE) { reachEndOfRow = true; }
            if (!reachEndOfRow) {
                columnValue = getNextColumnValue();
                nextRow.put(headers.get(i), columnValue.isEmpty() ? null : columnValue);
            } else {
                nextRow.put(headers.get(i), null);
            }

            nextCharPeeked = (char)stream.peekNextChar();
        }

        stream.getNextChar();

        return nextRow;
    }

    public static void main(String[] args) throws Exception {

        CSVParser reader = new CSVParser(new PeekableCharacterFileStream(args[0]));

        while (!reader.reachedEOF()) {
            Map<String, String> row = reader.getNextRow();
            System.out.print("{");
            for (Map.Entry e : row.entrySet()) {
                System.out.print(e.getKey() + "=" + e.getValue() + " ");
            }
            System.out.println("}");
        }


    }
}
