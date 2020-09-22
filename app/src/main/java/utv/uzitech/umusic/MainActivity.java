package utv.uzitech.umusic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    static ArrayList<JSONObject> allMusic;
    ArrayList<String> allAlbum, allArtist;
    static ArrayList<CardView> allCards;

    HorizontalScrollView allTracks_view;

    static int curr_pos = 0;

    PlaybackService service;

    static boolean inBackground = false;

    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        allTracks_view = findViewById(R.id.tracks_scrollView);

        getPermission();
        getMusic();

        ImageView curr_art = findViewById(R.id.curr_artwork);
        TextView curr_title = findViewById(R.id.curr_title);
        TextView curr_artist = findViewById(R.id.curr_artist);
        TextView curr_album = findViewById(R.id.curr_album);
        ImageButton play_pause = findViewById(R.id.play_pause);
        SeekBar seekBar = findViewById(R.id.curr_seekbar);

        service = new PlaybackService(this, allMusic, curr_art, curr_title, curr_artist, curr_album, play_pause, seekBar);

        setTracksList();

        ImageButton playPrev = findViewById(R.id.play_prev);
        ImageButton playNext = findViewById(R.id.play_next);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra("Remote_Input");
                assert input != null;
                switch (input) {
                    case "BTN_PLAY":
                        service.play(curr_pos);
                        break;
                    case "BTN_NEXT":
                        service.playNext();
                        actionRight(context);
                        break;
                    case "BTN_PREV":
                        service.playPrev();
                        actionLeft(context);
                        break;
                }
                if(!inBackground){
                    switch (input){
                        case "D_RIGHT":
                            actionRight(context);
                            break;
                        case "D_LEFT":
                            actionLeft(context);
                            break;
                    }
                }
            }
        };

        playPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.playPrev();
                actionLeft(getApplicationContext());
            }
        });

        playNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.playNext();
                actionRight(getApplicationContext());
            }
        });

        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.play(curr_pos);
            }
        });

        highlightCard(getApplicationContext());

        this.registerReceiver(receiver, new IntentFilter("utv.uzitech.remote_input"));
    }

    void actionRight(Context context) {
        if (curr_pos != allMusic.size() - 1) {
            curr_pos += 1;
        } else {
            curr_pos = 0;
        }
        highlightCard(context);
    }

    void actionLeft(Context context) {
        if (curr_pos != 0) {
            curr_pos -= 1;
        } else {
            curr_pos = allMusic.size() - 1;
        }
        highlightCard(context);
    }

    private void highlightCard(Context context) {
        allTracks_view.smoothScrollTo(allCards.get(curr_pos).getLeft(), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < allCards.size(); i++) {
                CardView view = allCards.get(i);
                if (i == curr_pos) {
                    view.setCardBackgroundColor(context.getColor(R.color.colorAccent));
                } else {
                    view.setCardBackgroundColor(context.getColor(android.R.color.background_dark));
                }
            }
        }
    }

    private void setTracksList() {
        allCards = new ArrayList<>();
        LinearLayout tracks_list = findViewById(R.id.tracks_list);
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < allMusic.size(); i++) {
            CardView view = createCard(allMusic.get(i), density, i);
            allCards.add(view);
            tracks_list.addView(view);
        }
    }

    private CardView createCard(JSONObject object, float density, final int pos) {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") CardView layout = (CardView) inflater.inflate(R.layout.track_card, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        ImageView artwork = layout.findViewById(R.id.track_art);
        TextView title = layout.findViewById(R.id.track_title);

        try {
            title.setText(object.getString("title"));
            if (object.has("artwork")) {
                Bitmap bitmap = Bitmap.createScaledBitmap((Bitmap) object.get("artwork"), 500, 500, false);
                artwork.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        params.setMargins((int) density * 8, 0, (int) density * 8, 0);
        layout.setLayoutParams(params);

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                curr_pos = pos;
                service.play(curr_pos);
                highlightCard(getApplicationContext());
            }
        });

        return layout;
    }

    private void getMusic() {
        allMusic = new ArrayList<>();
        allAlbum = new ArrayList<>();
        allArtist = new ArrayList<>();

        String sortOrder = MediaStore.MediaColumns.TITLE;

        Cursor cursor = getApplicationContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder);

        Log.d("Time", "Start"); //
        assert cursor != null;
        while (cursor.moveToNext()) {
            try {
                JSONObject object = new JSONObject();
                object.put("title", cursor.getString(cursor.getColumnIndex("title")));
                object.put("album", cursor.getString(cursor.getColumnIndex("album")));
                if (!allAlbum.contains(cursor.getString(cursor.getColumnIndex("album")))) {
                    allAlbum.add(cursor.getString(cursor.getColumnIndex("album")));
                }
                object.put("artist", cursor.getString(cursor.getColumnIndex("artist")));
                if (!allArtist.contains(cursor.getString(cursor.getColumnIndex("artist")))) {
                    allArtist.add(cursor.getString(cursor.getColumnIndex("artist")));
                }
                object.put("source", cursor.getString(cursor.getColumnIndex("_data")));
                object.put("artwork", getBitmap(cursor.getString(cursor.getColumnIndex("_data"))));
                allMusic.add(object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d("Time", "End"); //

        cursor.close();

        Collections.sort(allAlbum);
        Collections.sort(allArtist);
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

    private void getPermission() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    protected void onPause() {
        inBackground = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        inBackground = false;
        super.onResume();
    }
}