package dev.pocketcode.app;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.provider.DocumentsContract;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Only resolves relative paths inside the exact tree granted by Android's picker. */
final class SharedFolder {
    private final Context context;
    private final Uri tree;
    private static final String[] COLUMNS={"document_id","_display_name","mime_type","_size"};
    SharedFolder(Context context,String uri) throws Exception {
        this.context=context;
        if(uri.isEmpty())throw new Exception("ابتدا یک پوشه را از بخش دسترسی‌ها انتخاب کن.");
        tree=Uri.parse(uri);
        if(!DocumentsContract.isTreeUri(tree))throw new Exception("Invalid granted folder.");
    }
    private String[] parts(String path) throws Exception {
        if(path.length()>500 || path.startsWith("/") || path.contains("\\") || path.indexOf('\0')>=0)throw new Exception("Invalid relative path.");
        if(path.isEmpty())return new String[0];
        String[] result=path.split("/",-1);
        if(result.length>16)throw new Exception("Path too deeply nested.");
        for(String p:result)if(p.isEmpty()||p.equals(".")||p.equals(".."))throw new Exception("Invalid relative path.");
        return result;
    }
    private Uri root(){return DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));}
    private Uri findChild(Uri parent,String name) throws Exception {
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(parent));
        try(Cursor c=context.getContentResolver().query(children,COLUMNS,null,null,null)){
            if(c==null)throw new Exception("Folder provider unavailable.");
            while(c.moveToNext())if(name.equals(c.getString(1)))return DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0));
        }
        return null;
    }
    private Uri resolve(String path) throws Exception {
        Uri current=root();
        for(String part:parts(path)){current=findChild(current,part);if(current==null)throw new Exception("File or folder not found.");}
        return current;
    }
    JSONArray list(String path) throws Exception {
        Uri folder=resolve(path);JSONArray result=new JSONArray();
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(folder));
        try(Cursor c=context.getContentResolver().query(children,COLUMNS,null,null,null)){
            if(c==null)throw new Exception("Folder could not be listed.");
            while(c.moveToNext() && result.length()<500)result.put(new JSONObject().put("name",c.getString(1))
                .put("path",path.isEmpty()?c.getString(1):path+"/"+c.getString(1))
                .put("directory",DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2))).put("size",c.getLong(3)));
        }
        return result;
    }
    JSONObject read(String path) throws Exception {
        parts(path);if(path.isEmpty())throw new Exception("Choose a file.");
        try(InputStream in=context.getContentResolver().openInputStream(resolve(path))){
            if(in==null)throw new Exception("Could not open file.");
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
            while((count=in.read(buffer))!=-1){if(out.size()+count>512*1024)throw new Exception("Only text files up to 512 KB are supported.");out.write(buffer,0,count);}
            byte[] bytes=out.toByteArray();
            for(byte b:bytes)if(b==0)throw new Exception("Binary files are not supported.");
            String text=StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            return new JSONObject().put("content",text);
        }
    }
    JSONObject write(String path,String content) throws Exception {
        String[] names=parts(path);if(names.length==0)throw new Exception("Choose a file name.");
        byte[] bytes=content.getBytes(StandardCharsets.UTF_8);if(bytes.length>512*1024)throw new Exception("File exceeds 512 KB.");
        int slash=path.lastIndexOf('/');Uri parent=resolve(slash<0?"":path.substring(0,slash));
        Uri target=findChild(parent,names[names.length-1]);
        if(target==null)target=DocumentsContract.createDocument(context.getContentResolver(),parent,"text/plain",names[names.length-1]);
        if(target==null)throw new Exception("Could not create file.");
        // Document providers do not guarantee atomic writes. This is disclosed before approval.
        try(OutputStream out=context.getContentResolver().openOutputStream(target,"wt")){
            if(out==null)throw new Exception("Could not open file for writing.");out.write(bytes);
        }
        return new JSONObject().put("ok",true);
    }
}
