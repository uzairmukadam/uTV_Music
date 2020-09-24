package utv.uzitech.umusic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    ArrayList<JSONObject> allMusic;
    ArrayList<String> allAlbum, allArtist;
    ArrayList<ArrayList<Integer>> albumTracks, artistTracks;
    ArrayList<Integer> favTracks;

    ImageView tracks_list;
    LinearLayout music_list;

    DatabaseHelper database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new DatabaseHelper(getApplicationContext());

        tracks_list = findViewById(R.id.bg_art);
        music_list = findViewById(R.id.music_list);

        getAllFiles();

        makeList();
    }

    void getAllFiles(){
        allMusic = new ArrayList<>();
        allAlbum = new ArrayList<>();
        allArtist = new ArrayList<>();
        albumTracks = new ArrayList<>();
        artistTracks = new ArrayList<>();
        favTracks = new ArrayList<>();
        String sortOrder = MediaStore.MediaColumns.TITLE;

        Cursor cursor = getApplicationContext().getContentResolver().
                query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder);

        ArrayList<String> allFavs = database.getAllFav();

        int i=0;
        Log.d(TAG, "Start_getRaw"); //
        assert cursor != null;
        while (cursor.moveToNext()) {
            try {
                JSONObject object = new JSONObject();
                object.put("title", cursor.getString(cursor.getColumnIndex("title")));
                object.put("album", cursor.getString(cursor.getColumnIndex("album")));
                if (!allAlbum.contains(object.getString("album"))) {
                    allAlbum.add(object.getString("album"));
                    ArrayList<Integer> temp = new ArrayList<>();
                    temp.add(i);
                    albumTracks.add(temp);
                }else {
                    int index = allAlbum.indexOf(object.getString("album"));
                    ArrayList<Integer> temp = albumTracks.get(index);
                    temp.add(i);
                    albumTracks.set(index, temp);
                }
                object.put("artist", cursor.getString(cursor.getColumnIndex("artist")));
                if (!allArtist.contains(object.getString("artist"))) {
                    allArtist.add(object.getString("artist"));
                    ArrayList<Integer> temp = new ArrayList<>();
                    temp.add(i);
                    artistTracks.add(temp);
                }else {
                    int index = allArtist.indexOf(object.getString("artist"));
                    ArrayList<Integer> temp = artistTracks.get(index);
                    temp.add(i);
                    artistTracks.set(index, temp);
                }
                object.put("duration", cursor.getInt(cursor.getColumnIndex("duration")));
                object.put("source", cursor.getString(cursor.getColumnIndex("_data")));
                if(allFavs.contains(object.getString("source"))){
                    allFavs.remove(object.getString("source"));
                    favTracks.add(i);
                }
                allMusic.add(object);
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "End_getRaw"); //

        for(String path: allFavs){
            database.removeFav(path);
        }

        cursor.close();
    }

    void makeList(){
        Log.d(TAG, "List_Start");
        for (int i = 0; i < allMusic.size(); i++) {
            final LinearLayout view = createCard(allMusic.get(i), i);
            music_list.addView(view);
        }
        Log.d(TAG, "List_Done");
    }

    private LinearLayout createCard(final JSONObject object, final int i) {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.track_list_layout, null);

        TextView title = layout.findViewById(R.id.track_name);
        ImageView art = layout.findViewById(R.id.track_art);
        try {
            title.setText(object.getString("title"));
            Glide.with(getApplicationContext()).load(getMetadata(object.getString("source")).getEmbeddedPicture()).
                    apply(new RequestOptions().override(500, 500)).
                    placeholder(ContextCompat.getDrawable(getApplicationContext(), R.drawable.art_placeholder)).into(art);
        }catch (Exception e){
            e.printStackTrace();
        }

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), String.valueOf(i), Toast.LENGTH_SHORT).show();
                try {
                    Glide.with(getApplicationContext()).load(getMetadata(object.getString("source")).getEmbeddedPicture()).into(tracks_list);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        return layout;
    }

    private Bitmap getBitmap(String path) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                ParcelFileDescriptor fileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(Uri.fromFile(new File(path)), "r");
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

    private MediaMetadataRetriever getMetadata(String path) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                ParcelFileDescriptor fileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(Uri.fromFile(new File(path)), "r");
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