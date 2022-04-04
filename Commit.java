package gitlet;


import java.io.Serializable;
import java.util.HashMap;

/** Represents a gitlet commit object.
 *
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     *
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    protected HashMap<String, String> files;
    protected String date;
    protected String message;
    protected Commit parentOne;
    protected Commit parentTwo;

    public Commit(HashMap<String, String> f, String d, String m, Commit parent1, Commit parent2) {
        files = f;
        date = d;
        message = m;
        parentOne = parent1;
        parentTwo = parent2;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public HashMap<String, String> getFiles() {
        return files;
    }

    public void printFile() {
        System.out.println(files);
    }

}
