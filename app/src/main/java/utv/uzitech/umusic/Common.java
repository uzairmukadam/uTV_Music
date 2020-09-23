package utv.uzitech.umusic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;

public class Common {

    Context context;

    Common(Context context){
        this.context = context;
    }

    public Bitmap getArtwork(String path){
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(Uri.fromFile(new File(path)), "r");
                assert fileDescriptor != null;
                FileDescriptor fileDescriptor1 = fileDescriptor.getFileDescriptor();
                metadataRetriever.setDataSource(fileDescriptor1);
            } else {
                metadataRetriever.setDataSource(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (metadataRetriever.getEmbeddedPicture() != null) {
            InputStream inputStream = new ByteArrayInputStream(metadataRetriever.getEmbeddedPicture());
            bitmap = BitmapFactory.decodeStream(inputStream);
        }
        return bitmap;
    }

    public Bitmap compressedBitmap(String path){
        return Bitmap.createScaledBitmap((Bitmap) getArtwork(path), 500, 500, false);
    }

    public MediaMetadataRetriever getMetadata(String path){
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(Uri.fromFile(new File(path)), "r");
                assert fileDescriptor != null;
                FileDescriptor fileDescriptor1 = fileDescriptor.getFileDescriptor();
                metadataRetriever.setDataSource(fileDescriptor1);
            } else {
                metadataRetriever.setDataSource(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadataRetriever;
    }
}
