import java.io.IOException;

public interface Registro {
    public void setId(long i);
    public long getId();
    public byte[] toByteArray() throws IOException;
    public void fromByteArray(byte[] b) throws IOException;
}
