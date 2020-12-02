package org.reactome.release.qa.common;

import org.gk.model.GKInstance;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SkipList {

    public List<String> skipList = new ArrayList<>();

    /**
     * The SkipList class contains functions for reading in an assumed skiplist, and checking its contents.
     * The main assumption is that the skiplist file will be stored in a "resources" folder, which needs to be located
     * in the same directory as the executor, and be in the format "step_name_skip_list.txt".
     * @param displayName - String, displayName of class calling SkipList class.
     * @throws IOException, thrown when file does not exist. Note: Does not terminate program.
     */
    public SkipList(String displayName) throws IOException {
        if (Files.exists(getSkipListFilePath(displayName))) {
            skipList = readDbIdsFromSkipListFile(getSkipListFilePath(displayName));
        } else {
            FileNotFoundException e = new FileNotFoundException();
            e.printStackTrace();
        }
    }

    /**
     * This method gets file name for skiplist based on two assumptions. The first assumption is that it will
     * be a file located in the "resources" directory. The second is that the skip list, if it exists, will
     * have the displayName of the class calling it in lower case, and spaced by underscores, followed by "_skip_list.txt".
     * Eg: "resources/step_name_skip_list.txt"
     * @param displayName - String, name of class calling the method.
     * @return - Path, the assumed skip list file name.
     */
    private Path getSkipListFilePath(String displayName) {
        return Paths.get("resources/" + displayName.toLowerCase() + "_skip_list.txt");
    }

    /**
     * Parses file that is formatted as a list of Reactome DbIds.
     * @return - List<String>, Reactome database ids.
     * @throws IOException, thrown if value in file can't be parsed as an integer.
     */
    public List<String> readDbIdsFromSkipListFile(Path skipListFilePath) throws IOException {
        List<String> skipListFromFile = new ArrayList<>();
        for (String dbId : Files.readAllLines((skipListFilePath))) {
            // Ignore commenting lines
            if (!dbId.startsWith("#")) {
                try {
                    Integer.parseInt(dbId);
                    skipListFromFile.add(dbId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return skipListFromFile;
    }

    /**
     * Checks contents of skipList, which should be a list of skippable DbIds, to see if incoming instance should be skipped.
     * @param inst - GKInstance, instance being checked.
     * @return - boolean, true if inst DbId is in skipList, false if not.
     */
    public boolean containsInstanceDbId(GKInstance inst) {
        return skipList.contains(inst.getDBID().toString());
    }

    public List<String> getSkipListDbIds() {
        return skipList;
    }
}
