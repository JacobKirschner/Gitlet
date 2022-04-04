package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else {
            String firstArg = args[0];
            switch (firstArg) {

                case "add-remote":
                    Remote.newRemote(args[1], args[2]);
                    break;
                case "rm-remote":
                    Remote.rmRemote(args[1]);
                    break;
                case "fetch":
                    Remote.fetch(args[1], args[2]);
                    break;

                default:

                    if (Repository.GITLET_DIR.exists()) {
                        switch (firstArg) {
                            case "add":
                                if (args.length == 2 && args[1] != null)
                                    Repository.add(args[1]);
                                else {
                                    System.out.println("Incorrect operands.");
                                }
                                break;
                            case "commit":
                                if (args.length == 2 && args[1] != null && !args[1].equals("")) {
                                    Repository.commit(args[1], null);
                                } else {
                                    System.out.println("Please enter a commit message.");
                                }
                                break;
                            case "log":
                                Repository.log();
                                break;
                            case "checkout":
                                if (args.length == 2) {
                                    Repository.checkoutBranch(args[1]);
                                } else if (args.length == 3) {
                                    Repository.headCheckout(args[2]);
                                } else if (args.length == 4 && args[2].equals("--")) {
                                    Repository.checkoutByCommit(args[1], args[3]);
                                } else {
                                    System.out.println("Incorrect operands.");
                                }
                                break;
                            case "find":
                                if (args.length == 2 && args[1] != null) {
                                    Repository.find(args[1]);
                                } else {
                                    System.out.println("Incorrect operands.");
                                }
                                break;
                            case "rm":
                                if (args.length == 2 && args[1] != null)
                                    Repository.rm(args[1]);
                                else {
                                    System.out.println("Incorrect operands.");
                                }
                                break;
                            case "global-log":
                                Repository.globalLog();
                                break;
                            case "status":
                                Repository.status();
                                break;
                            case "branch":
                                if (args.length == 2 && args[1] != null)
                                    Repository.newBranch(args[1]);
                                else {
                                    System.out.println("Incorrect operands.");
                                }
                                break;
                            case "rm-branch":
                                if (args.length == 2 && args[1] != null)
                                    Repository.removeBranch(args[1]);
                                else {
                                    System.out.println("Incorrect operands.");
                                }
                                break;
                            case "reset":
                                if (args.length == 2 && args[1] != null) {
                                    Repository.reset(args[1]);
                                }  else {
                                    System.out.println("Incorrect operands."); }
                                break;
                            case "merge":
                                Repository.merge(args[1]);
                                break;
                            case "test":
                                break;
                            default:
                                System.out.println("No command with that name exists.");
                        }
                    } else {
                        System.out.println("Not in an initialized Gitlet directory.");
                    }
            }
        }
    }
}
