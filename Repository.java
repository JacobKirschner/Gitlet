package gitlet;

import java.io.File;
import static gitlet.Utils.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/** Represents a gitlet repository.
 *  [TODO]: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     *
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File COMMITS = join(GITLET_DIR, "Commits");
    public static final File BRANCHES = join(GITLET_DIR, "branches");

    public static void init() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            COMMITS.mkdir();
            BRANCHES.mkdir();

            File master = join(BRANCHES, "master");
            createFile(master);

            /* the file that will store the current commit */
            File f = join(GITLET_DIR, "files");
            createFile(f);

            File cB = join(GITLET_DIR, "Current Branch");
            createFile(cB);
            writeContents(cB, "master");

            Commit zero = new Commit(null, getDateTime(new Date(0)), "initial commit", null, null);
            StagingArea stage = new  StagingArea();
            writeObject(master, zero);
            writeObject(f, stage);
            commitToMasterInit(zero, sha1(serialize(zero)));

        } else {
            String a = "A Gitlet version-control system already exists in the current directory.";
            System.out.println(a);
        }
    }

    /* Creates a commit Object*/
    public static void commit(String message, Commit p2) {

        if (message == null) {
            System.out.println("Please enter a commit message.");
        } else {

            StagingArea currentStage = curStage();
            currentStage.untracked = ufiles();
            writeToCurrentStage(currentStage);

            if (currentStage.fileHash.size() == 0 && currentStage.removeFiles.size() == 0) {
                System.out.println("No changes added to the commit.");
            } else {

                Commit head = commitHEAD();
                head = new Commit(getFileHash(), getDateTime(new Date()), message, head, p2);
                commitToMaster(head, sha1(serialize(head)));
            }
        }
    }

    private static void commitToMaster(Commit h, String sha1) {

        StagingArea oldFiles = stageHEAD();
        oldFiles.associatedCommit = h;

        StagingArea currentFiles = curStage();
        HashMap<String, String> getKeys = currentFiles.fileHash;
        HashMap<String, Blob> getBlobs = currentFiles.files;

        for (HashMap.Entry<String, String> entry : getKeys.entrySet()) {
            oldFiles.fileHash.put(entry.getKey(), entry.getValue());
        }

        for (HashMap.Entry<String, Blob> entry : getBlobs.entrySet()) {
            oldFiles.files.put(entry.getKey(), entry.getValue());
        }

        for (int i = 0; i < currentFiles.removeFiles.size(); i++) {
            oldFiles.files.remove(oldFiles.fileHash.get(currentFiles.removeFiles.get(i)));
            oldFiles.fileHash.remove(currentFiles.removeFiles.get(i));
        }

        oldFiles.untracked = currentFiles.untracked;

        File f = new File(COMMITS, sha1);
        createFile(f);
        writeObject(f, oldFiles);

        StagingArea A = new StagingArea();
        A.untracked = oldFiles.untracked;
        writeToCurrentStage(A);

        writeToHEADcommit(h);

    }

    private static void commitToMasterInit(Commit h, String sha1) {

        File f = join(COMMITS, sha1);
        File files = new File(GITLET_DIR, "files");
        createFile(f);
        StagingArea stage = curStage();
        stage.associatedCommit =  h;
        ArrayList<String> initUntracked = new ArrayList<>();
        for (int i = 0; i < plainFilenamesIn(CWD).size(); i++) {
            initUntracked.add(plainFilenamesIn(CWD).get(i));
        }
        stage.untracked = initUntracked;
        writeObject(f, stage);
        writeObject(files, new StagingArea());

    }

    public static void add(String fileTxt) {

        File fileInCWD = new File(CWD, fileTxt);

        if (fileInCWD.exists()) {

            StagingArea prevStage = stageHEAD();
            StagingArea A = curStage();
            Blob b = new Blob(fileTxt, fileInCWD);

            Boolean fileInPrevCommit = prevStage.fileHash.containsKey(fileTxt);
            Boolean sameHash = sha1(serialize(b)).equals(prevStage.fileHash.get(fileTxt));
            Boolean notInRemove = !A.removeFiles.contains(fileTxt);

            if (fileInPrevCommit && sameHash && notInRemove) {
                return;
            }

            if (A.untracked.contains(fileTxt)) {
                A.untracked.remove(fileTxt);
            }

            A.stageAddition(fileTxt);
            writeToCurrentStage(A);

        } else {

            System.out.println("File does not exist.");
        }
    }

    public static void log() {
        Commit printHead = commitHEAD();
        while (printHead != null) {
            System.out.println("===");
            System.out.println("commit " + sha1(serialize(printHead)));

            if (printHead.parentTwo != null) {
                String p1 = sha1(serialize(printHead.parentOne));
                String p2 = sha1(serialize(printHead.parentTwo));
                System.out.print("Merge: ");
                System.out.println(p1.substring(0, 7) + " " + p2.substring(0, 7));
            }

            System.out.println("Date: " + printHead.getDate());
            System.out.println(printHead.getMessage());
            System.out.println();
            printHead = printHead.parentOne;
        }
    }

    public static void globalLog() {
        List<String> commits = plainFilenamesIn(COMMITS);
        for (int i = 0; i < commits.size(); i++) {
            File f = join(COMMITS, commits.get(i));
            Commit c = readObject(f, StagingArea.class).associatedCommit;
            System.out.println("===");
            System.out.println("commit " + sha1(serialize(c)));

            if (c.parentTwo != null) {
                String p1 = sha1(serialize(c.parentOne));
                String p2 = sha1(serialize(c.parentTwo));
                System.out.print("Merge: ");
                System.out.println(p1.substring(0, 7) + " " + p2.substring(0, 7));
            }

            System.out.println("Date: " + c.getDate());
            System.out.println(c.getMessage());
            System.out.println();
        }
    }

    public static void headCheckout(String file) {

        StagingArea files = stageHEAD();
        HashMap<String, String> keys = files.fileHash;
        HashMap<String, Blob> blobs = files.files;


        if (keys.containsKey(file)) {
            byte[] sFile = blobs.get(keys.get(file)).serializedFile;
            File newFile = new File(CWD, file);
            createFile(newFile);
            writeContents(newFile, sFile);

        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    public static void checkoutByCommit(String commitSHA1, String file) {

        String fullSHA1 = containsSHA1(commitSHA1);

        if (fullSHA1 != null) {

            File f = new File(COMMITS, fullSHA1);
            File newFile = new File(CWD, file);
            StagingArea sA = readObject(f, StagingArea.class);
            HashMap<String, String> getKeys = sA.fileHash;
            HashMap<String, Blob> getBlobs = sA.files;

            if (getKeys.get(file) != null && getBlobs.get(getKeys.get(file)) != null) {

                if (!newFile.exists()) {
                    createFile(newFile);
                }

                byte[] serializedFile = getBlobs.get(getKeys.get(file)).serializedFile;
                writeContents(newFile, serializedFile);

            } else {
                System.out.println("File does not exist in that commit.");
            }
        } else {
            System.out.println("No commit with that id exists");
        }
    }

    public static void checkoutBranch(String fileTxt) {

        String curBranch = currentBranch();
        ArrayList<String> unntrackedCWD = ufiles();
        StagingArea oldFiles = stageHEAD();
        File f = new File(BRANCHES, fileTxt);

        if (f.exists() && !currentBranch().equals(fileTxt)) {

            changeBranch(fileTxt);
            StagingArea newHeadFiles = stageHEAD();
            for (int i = 0; i < unntrackedCWD.size(); i++) {
                if (newHeadFiles.fileHash.containsKey(unntrackedCWD.get(i))) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    changeBranch(curBranch);
                    return;
                }
            }

            for (HashMap.Entry<String, String> entry : oldFiles.fileHash.entrySet()) {
                if (!newHeadFiles.fileHash.containsKey(entry.getKey())) {
                    File fDel = new File(CWD, entry.getKey());
                    fDel.delete();
                }
            }

            HashMap<String, String> keys = newHeadFiles.fileHash;
            HashMap<String, Blob> blobs = newHeadFiles.files;

            writeToCurrentStage(new StagingArea());

            for (HashMap.Entry<String, String> entry : keys.entrySet()) {
                File cwdFile = new File(CWD, entry.getKey());
                byte[] serializedFile = blobs.get(entry.getValue()).serializedFile;
                createFile(cwdFile);
                writeContents(cwdFile, serializedFile);
            }


        } else if (currentBranch().equals(fileTxt)) {
            System.out.println("No need to checkout the current branch.");
        } else if (!f.exists()) {
            System.out.println("No such branch exists.");
        }
    }

    public static void rm(String fileTxt) {

        /** see if fileTxt is in staging Add
         * if yes, just remove from files staging area
         * if no, see if in prev commit
         * if yes, add to area stage for removval */

        StagingArea curStage = curStage();
        StagingArea prevStage = stageHEAD();

        if (prevStage.fileHash.containsKey(fileTxt)) {
            File f = new File(CWD, fileTxt);
            curStage.stageRemove(fileTxt);
            writeToCurrentStage(curStage);
            f.delete();
        } else if (curStage.fileHash.containsKey(fileTxt)) {
            curStage.files.remove(curStage.fileHash.get(fileTxt));
            curStage.fileHash.remove(fileTxt);
            writeToCurrentStage(curStage);
            return;
        } else {
            System.out.println("No reason to remove the file");
        }
    }

    public static void removeBranch(String branchTxt) {
        if (branchTxt.equals(currentBranch())) {
            System.out.print("Cannot remove the current branch.");
            return;
        }

        File f = new File(BRANCHES, branchTxt);
        if (!f.exists()) {
            System.out.println("A branch with that name does not exist.");
        } else {
            f.delete();
        }
    }

    public static void newBranch(String fileTxt) {
        File f = new File(BRANCHES, fileTxt);
        if (!f.exists()) {

            createFile(f);
            Commit c = commitHEAD();
            StagingArea A = stageHEAD();
            writeObject(f, c);
        } else {
            System.out.println("A branch with that name already exists.");
        }
    }

    public static void status() {
        List<String> brnchs = plainFilenamesIn(BRANCHES);
        System.out.println("=== Branches ===");
        for (int i = 0; i < brnchs.size(); i++) {
            if (brnchs.get(i).equals(currentBranch())) {
                System.out.println("*" + brnchs.get(i));
            } else {
                System.out.println(brnchs.get(i));
            }
        }
        System.out.println("\n=== Staged Files ===");
        StagingArea stage = curStage();
        for (HashMap.Entry<String, String> entry : stage.fileHash.entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println("\n=== Removed Files ===");
        for (int i = 0; i < stage.removeFiles.size(); i++) {
            System.out.println(stage.removeFiles.get(i));
        }

        System.out.println("\n=== Modifications Not Staged For Commit ===");

        for (HashMap.Entry<String, String> entry : stageHEAD().fileHash.entrySet()) {
            if (curStage().fileHash.containsKey(entry.getKey())) {
                continue;
            } else if (plainFilenamesIn(CWD).contains(entry.getKey())) {
                File mod = new File(CWD, entry.getKey());
                Blob b = new Blob(entry.getKey(), mod);
                String modSHA = sha1(serialize(b));
                String shaHead = entry.getValue();
                if (!modSHA.equals(shaHead)) {
                    System.out.println(entry.getKey() + " (modified)");
                }
            } else if (!curStage().removeFiles.contains(entry.getKey())) {
                System.out.println(entry.getKey() + " (deleted)");
            }
        }


        System.out.println("\n=== Untracked Files ===");

        List<String> filesinCWD = ufiles();
        for (int i = 0; i < filesinCWD.size(); i++) {
            System.out.println(filesinCWD.get(i));
        }
        System.out.println();

    }

    public static void find(String fileTxt) {
        boolean oneFound = false;
        List<String> commits = plainFilenamesIn(COMMITS);
        for (int i = 0; i < commits.size(); i++) {
            File allCommits = new File(COMMITS, commits.get(i));
            Commit c = readObject(allCommits, StagingArea.class).associatedCommit;
            if (c.getMessage().equals(fileTxt)) {
                oneFound = true;
                System.out.println(sha1(serialize(c)));
            }
        }
        if (!oneFound) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void reset(String fileTxt) {

        List<String> cwdFiles = plainFilenamesIn(CWD);
        String fullSha1 = containsSHA1(fileTxt);

        if (fullSha1 != null) {
            File f = new File(COMMITS, fullSha1);
            StagingArea newHead = readObject(f, StagingArea.class);
            ArrayList<String> untrackedCWD = ufiles();
            String s = "There is an untracked "
                    + "file in the way; delete it, or add and commit it first.";
            for (int i = 0; i < untrackedCWD.size(); i++) {
                if (newHead.fileHash.containsKey(untrackedCWD.get(i))) {
                    System.out.println(s);
                    return;
                }
            }

            Commit c = newHead.associatedCommit;
            writeToHEADcommit(c);

            for (int i = 0; i < cwdFiles.size(); i++) {
                String fname = cwdFiles.get(i);
                if (!(newHead.untracked.contains(fname))) {
                    File del = new File(CWD, fname);
                    del.delete();
                }
            }

            for (HashMap.Entry<String, String> entry : newHead.fileHash.entrySet()) {
                String fname = entry.getKey();
                checkoutByCommit(fileTxt, fname);
            }

            writeToCurrentStage(new StagingArea());
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    public static void merge(String givenBranch) {
        if (!new File(BRANCHES, givenBranch).exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        StagingArea curStage = curStage();
        if (!(curStage.fileHash.size() == 0 & curStage.removeFiles.size() == 0)) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (currentBranch().equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        String splitSha = returnSplitSHA1(givenBranch);
        if (splitSha.equals(sha1(serialize(commitHEAD())))) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(givenBranch);
            return;
        }
        if (splitSha.equals(sha1(serialize(getBranch(givenBranch))))) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (checkUFiles(givenBranch)) {
            File stage = new File(COMMITS, splitSha);
            StagingArea splitStage = readObject(stage, StagingArea.class);
            File givenStageFile = new File(COMMITS, sha1(serialize(getBranch(givenBranch))));
            StagingArea givenStage = readObject(givenStageFile, StagingArea.class);
            ArrayList<String> allFiles = arrayFiles(givenBranch);

            boolean mergeConflict = false;
            for (int i = 0; i < allFiles.size(); i++) {
                String name = allFiles.get(i);
                boolean fileInSplit = fileInSplit(name, splitStage);
                boolean fileInGiven = fileInGiven(name, givenStage);
                boolean fileInHEAD = fileInCurrent(name, stageHEAD());
                boolean modifiedInHEAD = modInCur(name, splitStage, stageHEAD());
                boolean modifiedInGiven = modInCur(name, splitStage, givenStage);
                boolean givenEqHead = curEqGiven(name, stageHEAD(), givenStage);
                boolean inSplitAndCur = fileInSplit && fileInHEAD;
                boolean inSplitAndGiv = fileInSplit && fileInGiven;
                File writeOver = new File(CWD, name);
                createFile(writeOver);
                if (inSplitAndGiv && !modifiedInHEAD && modifiedInGiven) {
                    writeContents(writeOver,
                            givenStage.files.get(givenStage.fileHash.get(name)).serializedFile);
                    add(name);
                } else if (inSplitAndCur && modifiedInHEAD && !modifiedInGiven) {
                    continue;
                } else if ((!fileInSplit && !fileInGiven && fileInHEAD) || givenEqHead) {
                    continue;
                } else if (!fileInSplit && fileInGiven && !fileInHEAD) {
                    String temp = givenStage.fileHash.get(name);
                    writeContents(writeOver,
                            givenStage.files.get(temp).serializedFile);
                    add(name);
                } else if (fileInSplit && fileInHEAD && !fileInGiven && !modifiedInHEAD) {
                    rm(name);
                } else if (fileInSplit && !fileInHEAD && fileInGiven && !modifiedInGiven) {
                    writeOver.delete();
                } else {
                    skeleton(name, givenBranch, writeOver);
                    mergeConflict = true;
                    add(name);
                }
            }
            if (mergeConflict) {
                System.out.println("Encountered a merge conflict.");
            }
            commit("Merged " + givenBranch + " into "
                    + currentBranch() + ".", getBranch(givenBranch));
        }
    }

    /**************************************************************************************/
    /***************************** HELPER METHODS FOR MERGE *******************************/
    /**************************************************************************************/

    public static boolean fileInSplit(String fileName, StagingArea split) {
        return split.fileHash.containsKey(fileName);
    }

    public static ArrayList<String> arrayFiles(String givenBranch) {

        File givenStageFile = new File(COMMITS, sha1(serialize(getBranch(givenBranch))));
        StagingArea givenStage = readObject(givenStageFile, StagingArea.class);
        ArrayList<String> allFiles = new ArrayList<>();
        for (HashMap.Entry<String, String> entry : stageHEAD().fileHash.entrySet()) {
            allFiles.add(entry.getKey());
        }
        for (HashMap.Entry<String, String> entry : givenStage.fileHash.entrySet()) {
            if (!allFiles.contains(entry.getKey())) {
                allFiles.add(entry.getKey());
            }
        }

        return allFiles;
    }

    public static boolean fileInGiven(String fileName, StagingArea given) {
        return given.fileHash.containsKey(fileName);
    }

    public static boolean fileInCurrent(String fileName, StagingArea current) {
        return current.fileHash.containsKey(fileName);
    }

    public static boolean fileInAllThree(String fileName,
                                         StagingArea split,
                                         StagingArea given, StagingArea current) {

        boolean inSplit = fileInSplit(fileName, split);
        boolean inGiven = fileInGiven(fileName, given);
        boolean inCurrent = fileInCurrent(fileName, current);
        return inSplit && inGiven && inCurrent;
    }

    public static boolean modInGiven(String fileName, StagingArea split, StagingArea given) {
        if (!fileInGiven(fileName, given) && fileInSplit(fileName, split)) {
            return true;
        }
        if (fileInGiven(fileName, given) && !fileInSplit(fileName, split)) {
            return true;
        }
        if (fileInGiven(fileName, given) && fileInSplit(fileName, split)) {
            return !(split.fileHash.get(fileName).equals(given.fileHash.get(fileName)));
        }
        return false;
    }

    public static boolean modInCur(String fileName, StagingArea split, StagingArea cur) {
        if (fileInCurrent(fileName, cur) && !fileInSplit(fileName, split)) {
            return true;
        }
        if (!fileInCurrent(fileName, cur) && fileInSplit(fileName, split)) {
            return true;
        }
        if (fileInCurrent(fileName, cur) && fileInSplit(fileName, split)) {
            return !(split.fileHash.get(fileName).equals(cur.fileHash.get(fileName)));
        }
        return false;
    }

    public static boolean curEqGiven(String fileName, StagingArea cur, StagingArea given) {
        if (fileInCurrent(fileName, cur) && fileInGiven(fileName, given)) {
            return (cur.fileHash.get(fileName).equals(given.fileHash.get(fileName)));
        }
        return false;
    }

    public static String splitCommit(String givenBranch) {

        Commit given = getBranch(givenBranch);
        Commit curHead = getBranch(currentBranch());

        ArrayList<String> givenShas = new ArrayList<>();
        while (given != null) {
            givenShas.add(sha1(serialize(given)));
            given = given.parentOne;
        }

        while (curHead != null) {
            if (givenShas.contains(sha1(serialize(curHead)))) {
                return sha1(serialize(curHead));
            }
            curHead = curHead.parentOne;

        }

        return "";
    }

    public static void skeleton(String file, String branch, File curFile) {

        File givenFile = new File(COMMITS, sha1(serialize(getBranch(branch))));
        StagingArea giv = readObject(givenFile, StagingArea.class);

        File tempFile = new File(CWD, "temp");
        if (giv.fileHash.containsKey(file)) {
            createFile(tempFile);
            writeContents(tempFile, giv.files.get(giv.fileHash.get(file)).serializedFile);
        }

        File headTemp = new File(COMMITS, "headTemp");
        StagingArea headFile = stageHEAD();
        if (headFile.fileHash.containsKey(file)) {
            createFile(headTemp);
            writeContents(headTemp, headFile.files.get(headFile.fileHash.get(file)).serializedFile);
        }



        String newFile = "<<<<<<< HEAD\n";
        if (headTemp.exists()) {
            String ht = readContentsAsString(headTemp);
            newFile += ht;
        }
        newFile += "=======\n";
        if (tempFile.exists()) {
            String tf = readContentsAsString(tempFile);
            newFile += tf;
        }
        newFile += ">>>>>>>\n";
        writeContents(curFile, newFile);
        tempFile.delete();
        headTemp.delete();
    }

    /** END OF HELPER METHODS FOR MERGE */



    public static String currentBranch() {
        File f = new File(GITLET_DIR, "Current Branch");
        String fileName = readContentsAsString(f);
        return fileName;
    }

    private static void changeBranch(String fileTxt) {
        File f = new File(GITLET_DIR, "Current Branch");
        writeContents(f, fileTxt);
    }

    private static HashMap<String, String> getFileHash() {
        StagingArea stage = curStage();
        return stage.fileHash;
    }

    protected static String getDateTime(Date d) {
        SimpleDateFormat inForm = new SimpleDateFormat("EEE MMM d HH:mm:ss YYYY Z");
        return inForm.format(d);
    }

    protected static void createFile(File f) {
        try {
            f.createNewFile();
        } catch (IOException var2) {
            return;
        }
    }

    protected static StagingArea curStage() {
        File f = new File(GITLET_DIR, "files");
        StagingArea currentFiles = readObject(f, StagingArea.class);
        return currentFiles;
    }

    protected static void writeToCurrentStage(StagingArea A) {
        File f = new File(GITLET_DIR, "files");
        writeObject(f, A);
    }

    protected static Commit commitHEAD() {
        File f = new File(BRANCHES, currentBranch());
        Commit head = readObject(f, Commit.class);
        return head;
    }

    protected static void writeToHEADcommit(Commit h) {
        File f = new File(BRANCHES, currentBranch());
        writeObject(f, h);
    }

    protected static StagingArea stageHEAD() {
        Commit c = commitHEAD();
        File f = new File(COMMITS, sha1(serialize(c)));
        StagingArea headStage = readObject(f, StagingArea.class);
        return headStage;
    }

    protected static void writeToHEADstage(StagingArea A) {
        File f = new File(COMMITS, sha1(serialize(commitHEAD())));
        writeObject(f, A);
    }

    protected static Commit getBranch(String branch) {
        File f = new File(BRANCHES, branch);
        Commit c = readObject(f, Commit.class);
        return c;
    }



    protected static String containsSHA1(String commitSHA1) {
        List<String> fileNames = plainFilenamesIn(COMMITS);
        int len = commitSHA1.length();
        for (int i = 0; i < fileNames.size(); i++) {
            if (fileNames.get(i).substring(0, len).equals(commitSHA1)) {
                return fileNames.get(i);
            }
        }
        return null;
    }

    protected static String returnSplitSHA1(String newBranch) {

        ArrayList<String> sha1OfNewBranch = allPrevCommmits(getBranch(newBranch));
        ArrayList<String> old = allPrevCommmits(getBranch(currentBranch()));

        while (sha1OfNewBranch.size() > 0) {
            int index = sha1OfNewBranch.size() - 1;
            String node = sha1OfNewBranch.get(index);
            if (old.contains(node)) {
                return node;
            } else {
                sha1OfNewBranch.remove(index);
            }
        }
        return "";
    }

    protected static Commit firstCommitInBranch(String branchName) {
        Commit newBranch = getBranch(branchName);
        while (newBranch.parentOne != null) {
            newBranch = newBranch.parentOne;
        }
        return newBranch;
    }

    protected static ArrayList<String> allPrevCommmits(Commit c) {
        ArrayList<String> cur = new ArrayList<>();
        if (c.parentOne != null) {
            ArrayList<String> temp = allPrevCommmits(c.parentOne);
            for (int i = 0; i < temp.size(); i++) {
                if (!cur.contains(temp.get(i))) {
                    cur.add(temp.get(i));
                }
            }
        }
        if (c.parentTwo != null) {
            ArrayList<String> temp = allPrevCommmits(c.parentTwo);
            for (int i = 0; i < temp.size(); i++) {
                if (!cur.contains(temp.get(i))) {
                    cur.add(temp.get(i));
                }
            }
        }
        cur.add(sha1(serialize(c)));
        return cur;
    }

    public static boolean checkUFiles(String givenBranch) {
        ArrayList<String> untrackedCWD = ufiles();
        Commit commitHeadBranch = getBranch(givenBranch);
        File filesinCommitHead = new File(COMMITS, sha1(serialize(commitHeadBranch)));
        StagingArea newHeadFiles = readObject(filesinCommitHead, StagingArea.class);
        for (int i = 0; i < untrackedCWD.size(); i++) {
            if (newHeadFiles.fileHash.containsKey(untrackedCWD.get(i))) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return false;
            }
        }
        return true;
    }

    private static ArrayList<String> ufiles() {

        List<String> files = plainFilenamesIn(CWD);
        ArrayList<String> untracked = new ArrayList<>();
        StagingArea stage = curStage();
        StagingArea oldStage = stageHEAD();
        HashMap<String, String> newKeys = stage.fileHash;
        HashMap<String, String> oldKeys = oldStage.fileHash;

        for (int i = 0; i < files.size(); i++) {

            boolean notInOld = !oldKeys.containsKey(files.get(i));
            boolean notInNew = !newKeys.containsKey(files.get(i));
            boolean inRemove = stage.removeFiles.contains(files.get(i));

            if (inRemove || (notInOld && notInNew)) {
                untracked.add(files.get(i));
            }
        }
        return untracked;
    }
}
