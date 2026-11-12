package dev.pocketcode.app;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Resolves a shared-storage path to its exact volume root and never walks above that root. */
final class StoragePathNavigator {
    private StoragePathNavigator() { }

    static File findRoot(File target,List<File> roots) throws IOException {
        if(target==null||roots==null)return null;
        File canonicalTarget=target.getCanonicalFile();
        File best=null;
        for(File candidate:roots) {
            if(candidate==null)continue;
            File root=candidate.getCanonicalFile();
            if(contains(root,canonicalTarget)
                    &&(best==null||root.getPath().length()>best.getPath().length()))best=root;
        }
        return best;
    }

    static File parentWithinRoot(File directory,File root) throws IOException {
        if(directory==null||root==null)throw new IllegalArgumentException("Directory and storage root are required.");
        File current=directory.getCanonicalFile();
        File canonicalRoot=root.getCanonicalFile();
        if(!contains(canonicalRoot,current))throw new IllegalArgumentException("Directory is outside the selected storage root.");
        if(current.equals(canonicalRoot))return null;
        File parent=current.getParentFile();
        if(parent==null)throw new IllegalArgumentException("Directory has no parent inside its storage root.");
        File canonicalParent=parent.getCanonicalFile();
        if(!contains(canonicalRoot,canonicalParent))throw new IllegalArgumentException("Parent would leave the selected storage root.");
        return canonicalParent;
    }

    private static boolean contains(File root,File candidate) {
        String base=root.getPath();
        String path=candidate.getPath();
        if(path.equals(base))return true;
        String prefix=base.endsWith(File.separator)?base:base+File.separator;
        return path.startsWith(prefix);
    }
}
