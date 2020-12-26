
public interface PeekableCharacterStream{
    public boolean moreAvailable();
    public int peekNextChar();
    public int peekAheadChar(int ahead);
    public int getNextChar();
    public void close();
}