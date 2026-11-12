package dev.pocketcode.app;

import android.content.Context;
import android.os.Environment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;

/** Broad storage adapter is deliberately read-only: no write/delete/rename API. */
final class PhoneFiles {
    private final ArrayList<File> roots=new ArrayList<>();
    PhoneFiles(Context context) throws Exception {
        addRoot(Environment.getExternalStorageDirectory());
        for(File external:context.getExternalFilesDirs(null)) {
            if(external==null)continue;
            String path=external.getCanonicalPath();int marker=path.indexOf("/Android/");
            if(marker>0)addRoot(new File(path.substring(0,marker)));
        }
    }
    private void addRoot(File value) throws Exception {
        File root=value.getCanonicalFile();if(!roots.contains(root))roots.add(root);
    }
    JSONArray roots() throws Exception {
        JSONArray result=new JSONArray();for(File root:roots)result.put(new JSONObject().put("path",root.getPath()).put("directory",true));return result;
    }
    private File resolve(String path) throws Exception {
        if(path.isEmpty()||path.indexOf('\0')>=0||path.length()>4096)throw new Exception("Invalid storage path.");
        File file=new File(path).getCanonicalFile();
        if(StoragePathNavigator.findRoot(file,roots)==null)
            throw new Exception("Only shared storage volumes are accessible, not system or app-private storage.");
        return file;
    }
    JSONObject list(String path,int offset) throws Exception {
        File folder=resolve(path);if(!folder.isDirectory())throw new Exception("Folder unavailable or protected by Android.");
        File[] files=folder.listFiles();if(files==null)throw new Exception("Android denied reading this folder.");
        Arrays.sort(files,(a,b)->a.getName().compareToIgnoreCase(b.getName()));
        if(offset<0||offset>files.length)throw new Exception("Invalid page offset.");
        JSONArray items=new JSONArray();int end=Math.min(files.length,offset+200);
        for(int i=offset;i<end;i++) {
            File f=files[i];
            try { resolve(f.getPath()); } catch(Exception denied){continue;}
            items.put(new JSONObject().put("name",f.getName()).put("path",f.getPath()).put("directory",f.isDirectory()).put("size",f.length()));
        }
        File root=StoragePathNavigator.findRoot(folder,roots);
        File parent=StoragePathNavigator.parentWithinRoot(folder,root);
        return new JSONObject().put("entries",items).put("total",files.length).put("nextOffset",end<files.length?end:-1)
                .put("currentPath",folder.getPath()).put("rootPath",root.getPath())
                .put("isRoot",parent==null).put("parentPath",parent==null?JSONObject.NULL:parent.getPath());
    }
    JSONObject read(String path) throws Exception {
        File file=resolve(path);if(!file.isFile())throw new Exception("File unavailable or protected by Android.");
        try(InputStream in=new FileInputStream(file)){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int count;
            while((count=in.read(b))!=-1){if(out.size()+count>512*1024)throw new Exception("Text reader supports files up to 512 KB; binary/PDF/image parsing is not available.");out.write(b,0,count);}
            byte[] bytes=out.toByteArray();for(byte value:bytes)if(value==0)throw new Exception("Binary content cannot be read as text.");
            return new JSONObject().put("content",StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString());
        }
    }
}
