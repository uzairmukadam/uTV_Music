package utv.uzitech.umusic;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    ArrayList<JSONObject> allMusic;
    ArrayList<String> allAlbum, allArtist;
    ArrayList<ArrayList<Integer>> albumTracks, artistTracks;
    ArrayList<Integer> favTracks;

    LinearLayout allTracks_list;

    DatabaseHelper database;

    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        allTracks_list = findViewById(R.id.tracks_parent);

        database = new DatabaseHelper(getApplicationContext());

        getAllFiles();

        makeList();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra("Remote_Input");
                assert input != null;
                if(input.equals("BTN_BACK")){
                    MainActivity.super.onBackPressed();
                }
            }
        };

        registerReceiver(receiver, new IntentFilter("utv.uzitech.remote_input"));
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
        float density = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        int i=0;
        boolean end = false;
        while(!end){
            LinearLayout temp = new LinearLayout(this);
            temp.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=0; j<4; j++){
                if(i<allMusic.size()){
                    final CardView view = createCard(allMusic.get(i), i, density);
                    temp.addView(view);
                    i++;
                }else {
                    end = true;
                    break;
                }
            }
            temp.setGravity(Gravity.CENTER);
            temp.setLayoutParams(params);
            allTracks_list.addView(temp);
        }
        Log.d(TAG, "List_Done");
    }

    private CardView createCard(final JSONObject object, final int i, float density) {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") CardView layout = (CardView) inflater.inflate(R.layout.track_card, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView title = layout.findViewById(R.id.track_title);
        ImageView art = layout.findViewById(R.id.track_artwork);
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
                try {
                    Toast.makeText(getApplicationContext(), object.getString("title")+i, Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        params.setMargins((int) density * 8, (int) density * 8, (int) density * 8, (int) density * 8);
        layout.setLayoutParams(params);
        layout.setCardBackgroundColor(getResources().getColor(R.color.darkBackground));

        return layout;
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

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        registerReceiver(receiver, new IntentFilter("utv.uzitech.remote_input"));
        super.onPostResume();
    }
}