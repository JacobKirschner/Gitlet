package gitlet;
import net.sf.saxon.trans.SymbolicName;

import java.io.File;
import static gitlet.Utils.*;
import java.io.IOException;
import java.util.*;

public class Remote {

    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File REMOTES = new File(CWD, "remotes");
    public static final File GITLET_DIR = join(CWD, ".gitlet");


    public static void newRemote(String name, String loc) {
        if(!REMOTES.exists()) {
        REMOTES.mkdir();
        }

        File f = new File(REMOTES, name);

        if (!f.exists()) {
            createFile(f);
            String locOfGitlet = System.getProperty("user.dir")
                    + File.separator + loc.substring(2);
            File remoteLoc = newDir(loc.substring(2));
            writeContents(f, locOfGitlet);
        } else {
            System.out.println("A remote with that name already exists.");
        }
    }

    public static void rmRemote(String name) {
        File f = new File(REMOTES, name);
        if(f.exists()) {
            String location = readContentsAsString(f);
            File remoteLoc = newDir(location);
            remoteLoc.delete();
            f.delete();
        } else {
            System.out.println("A remote with that name does not exist.");
        }
    }

    public static void fetch(String name, String branchName) {

        File f = new File(REMOTES, name);
        if (f.exists()) {
            String location = readContentsAsString(f);
            File localBranch = new File(GITLET_DIR, "branches");
            File remoteLoc = newDir(location);
            File branches = new File(remoteLoc, "branches");
            File branch = new File(branches, branchName);
            if (branch.exists()) {
                Commit head = readObject(branch, Commit.class);
                ArrayList<String>  commitsInBranch = allPrevCommmits(head);


            } else {
                System.out.println("That remote does not have that branch.");
            }
        } else {
            System.out.println("Remote directory not found.");
        }
    }

    protected static void createFile(File f) {
        try {
            f.createNewFile();
        } catch (IOException var2) {
            return;
        }
    }

    protected static File newDir(String loc) {
        System.out.println(loc);
        File newDir = new File(System.getProperty("user.dir"));
        while (loc.length() > 0) {
            int i = 0;
            String word = "";
            while (i < loc.length() && !(loc.charAt(i) == '/')) {
                word += loc.charAt(i);
                i++;
            }
            newDir = join(newDir, word);
            if (!newDir.exists()) {
                newDir.mkdir();
            }
            if (i == loc.length()) {
                loc = "";
            } else {
                loc = loc.substring(i + 1);
            }
        }

        File commits = new File(newDir, "Commits");
        commits.mkdir();
        transferCOMMITS(commits);

        File branch = new File(newDir, "branches");
        branch.mkdir();
        transferBranches(branch);

        File curBranch = new File(newDir, "Current Branch");
        createFile(curBranch);
        File branchFromCWD = new File(GITLET_DIR, "Current Branch");
        writeContents(curBranch, readContentsAsString(branchFromCWD));

        File files = new File(newDir, "files");
        createFile(files);
        writeObject(files, new StagingArea());

        return newDir;
    }

    public static void transferCOMMITS(File f) {
        File commits = new File(GITLET_DIR, "Commits");
        for (int i = 0; i < plainFilenamesIn(commits).size(); i++) {
            File c = new File(commits, plainFilenamesIn(commits).get(i));
            File newCommit = new File(f, plainFilenamesIn(commits).get(i));
            StagingArea temp = readObject(c, StagingArea.class);
            writeObject(newCommit, temp);
        }
    }

    public static void transferBranches(File f) {
        File branches = new File(GITLET_DIR, "branches");
        for (int i = 0; i < plainFilenamesIn(branches).size(); i++) {
            File b = new File(branches, plainFilenamesIn(branches).get(i));
            File newCommit = new File(f, plainFilenamesIn(branches).get(i));
            Commit temp = readObject(b, Commit.class);
            writeObject(newCommit, temp);
        }
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
}
