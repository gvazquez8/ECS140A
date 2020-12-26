import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PeekableCharacterFileStream implements PeekableCharacterStream{

    private FileInputStream fs;
    private int currentRowIndex = 1;
    private int currentColIndex = 1;

    public PeekableCharacterFileStream(String filePath) throws FileNotFoundException {
        fs = new FileInputStream(filePath);
    }

    /**
    * @return the row number of the PCFS
     */
    public int getCurrentRowIndex() {
        return currentRowIndex;
    }

    /**
     * @return the column number of the PCFS
     */
    public int getCurrentColIndex() {
        return currentColIndex;
    }

    /**
     * Checks to see if there are any characters left in the file stream
     * @return true if more bytes are available else false
     * @throws IOException
     */
    @Override
    public boolean moreAvailable() {
        try {
            return fs.available() != 0;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Peeks at the next character in the stream without consuming it.
     * @return the next byte of data, or -1 if EOF is reached.
     */
    @Override
    public int peekNextChar() {
        try {
            long oldPos = fs.getChannel().position();
            int c = fs.read();
            fs.getChannel().position(oldPos);
            return c;
        }
        catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Peek forward to the character that is 'ahead' characters from current character without consuming it.
     * @param ahead number of bytes to look ahead. Note: peekAheadChar(0) == peekNextChar)
     * @return the byte of data that is 'ahead' bytes from current byte
     */
    @Override
    public int peekAheadChar(int ahead) {
        try {
            long oldPos = fs.getChannel().position();
            fs.getChannel().position(oldPos + ahead);
            int c = fs.read();
            fs.getChannel().position(oldPos);
            return c;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Returns the next character in the stream and consumes it.
     * @return the next byte of data, or -1 if EOF is reached.
     */
    @Override
    public int getNextChar() {
        try {
            int c = fs.read();
            if ((char)c == '\n') {
                currentRowIndex++;
                currentColIndex = 1;
            }
            else {
                currentColIndex++;
            }
            return c;
        }
        catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Closes the PeekableCharacterFileStream.
     */
    @Override
    public void close() {
        try {
            fs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        PeekableCharacterFileStream fs = new PeekableCharacterFileStream(args[0]);

        System.out.print("PeekNextchar: ");
        System.out.println((char)fs.peekNextChar());
        System.out.print("PeekAheadChar: ");
        System.out.println((char)fs.peekAheadChar(0));
        System.out.print("GetNextChar: ");
        System.out.println((char)fs.getNextChar());

    }

}
