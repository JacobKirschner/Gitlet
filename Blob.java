package gitlet;
import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

public class Blob implements Serializable {

    protected String name;
    protected File file;
    protected byte[] serializedFile;

    public Blob(String n, File f) {
        name = n;
        file = new File(n);
        serializedFile = readContents(file);
    }
}

