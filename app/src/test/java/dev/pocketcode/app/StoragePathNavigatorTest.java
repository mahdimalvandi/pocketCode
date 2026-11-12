package dev.pocketcode.app;

import org.junit.Test;
import java.io.File;
import java.util.Arrays;
import static org.junit.Assert.*;

public class StoragePathNavigatorTest {
    @Test public void choosesExactRemovableVolumeRoot() throws Exception {
        File primary=new File("/storage/emulated/0");
        File removable=new File("/storage/ABCD-1234");
        File target=new File(removable,"DCIM/Camera");
        File root=StoragePathNavigator.findRoot(target,Arrays.asList(primary,removable));
        assertEquals(removable.getCanonicalPath(),root.getCanonicalPath());
    }

    @Test public void prefersLongestMatchingRootWhenRootsOverlap() throws Exception {
        File broad=new File("/storage/emulated");
        File exact=new File("/storage/emulated/0");
        File target=new File(exact,"Download");
        File root=StoragePathNavigator.findRoot(target,Arrays.asList(broad,exact));
        assertEquals(exact.getCanonicalPath(),root.getCanonicalPath());
    }

    @Test public void returnsExactParentAndStopsAtVolumeRoot() throws Exception {
        File root=new File("/storage/ABCD-1234");
        File folder=new File(root,"DCIM/Camera");
        assertEquals(new File(root,"DCIM").getCanonicalPath(),
                StoragePathNavigator.parentWithinRoot(folder,root).getCanonicalPath());
        assertNull(StoragePathNavigator.parentWithinRoot(root,root));
    }

    @Test public void rejectsSiblingPathThatOnlySharesRootPrefix() throws Exception {
        File root=new File("/storage/emulated/0");
        File sibling=new File("/storage/emulated/0_backup/DCIM");
        assertNull(StoragePathNavigator.findRoot(sibling,Arrays.asList(root)));
    }

    @Test(expected=IllegalArgumentException.class)
    public void refusesParentNavigationOutsideSelectedVolume() throws Exception {
        StoragePathNavigator.parentWithinRoot(new File("/storage/ABCD-1234/../other"),new File("/storage/ABCD-1234"));
    }
}
