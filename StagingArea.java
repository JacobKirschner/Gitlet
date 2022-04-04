package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static gitlet.Utils.*;

public class StagingArea implements Serializable {

    protected HashMap<String, Blob> files = new HashMap<String, Blob>();
    protected HashMap<String, String> fileHash = new HashMap<String, String>();
    protected ArrayList<String> removeFiles = new ArrayList<>();
    protected Commit associatedCommit;
    protected ArrayList<String> untracked = new ArrayList<String>();

    public StagingArea() { }

    public void stageAddition(String fileTxt) {

        File f = new File(Repository.CWD, fileTxt);
        Blob b = new Blob(fileTxt, f);

        if (removeFiles.contains(fileTxt)) {

            Commit parentCommit = Repository.commitHEAD();
            StagingArea parStage = Repository.stageHEAD();

            if (parStage.fileHash.get(fileTxt).equals(sha1(serialize(b)))) {
                removeFiles.remove(fileTxt);
                return;
            }
            removeFiles.remove(fileTxt);

        }

        files.put(sha1(serialize(b)), b);
        fileHash.put(fileTxt, sha1(serialize(b)));

    }

    public void stageRemove(String fileTxt) {
        if (!removeFiles.contains(fileTxt)) {
            removeFiles.add(fileTxt);
        }
    }

    public void addCommit(Commit c) {
        associatedCommit = c;
    }
}
