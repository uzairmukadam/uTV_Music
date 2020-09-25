package utv.uzitech.umusic;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
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
    ArrayList<CardView> allCards;
    //ArrayList<Integer> musicQueue;
    int[] tracks_tab;

    LinearLayout allTracks_list;
    ScrollView allTracks_scroll;

    ImageView curr_artwork;
    TextView curr_title, curr_artist, curr_album;
    SeekBar seekbar;

    BroadcastReceiver receiver, mediaplayback;

    MediaPlayer player;

    int track = 0, now_playing = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        allTracks_list = findViewById(R.id.tracks_parent);
        allTracks_scroll = findViewById(R.id.tracks_scroll);

        curr_artwork = findViewById(R.id.curr_artwork);
        curr_title = findViewById(R.id.curr_title);
        curr_artist = findViewById(R.id.curr_artist);
        curr_album = findViewById(R.id.curr_album);
        seekbar = findViewById(R.id.curr_seekbar);

        getAllFiles();

        loadTracksTab();

        highlightCard();

        player = new MediaPlayer();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra("Remote_Input");
                assert input != null;
                switch (input) {
                    case "BTN_BACK":
                        MainActivity.super.onBackPressed();
                        break;
                    case "D_DOWN":
                        track += 4;
                        if (track > allMusic.size() - 1) {
                            if (track > allMusic.size() + tracks_tab[1] - 1) {
                                track = 0;
                            } else {
                                track = allMusic.size() - 1;
                            }
                        }
                        break;
                    case "D_UP":
                        track -= 4;
                        if (track < 0) {
                            track = allMusic.size() - 1;
                        }
                        break;
                    case "D_RIGHT":
                        if (track != allMusic.size() - 1) {
                            track += 1;
                        } else {
                            track = 0;
                        }
                        break;
                    case "D_LEFT":
                        if (track != 0) {
                            track -= 1;
                        } else {
                            track = allMusic.size() - 1;
                        }
                        break;
                    case "D_ENTER":
                        Play(track);
                        break;
                }
                highlightCard();
            }
        };

        mediaplayback = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra("Remote_Input");
                assert input != null;
                switch (input) {
                    case "BTN_PLAY":
                        Play(now_playing);
                        break;
                    case "BTN_PREV":
                        Prev();
                        break;
                    case "BTN_NEXT":
                        Next();
                        break;
                }
            }
        };

        registerReceiver(receiver, new IntentFilter("utv.uzitech.remote_input"));
        registerReceiver(mediaplayback, new IntentFilter("utv.uzitech.remote_input"));
    }

    private void loadTracksTab() {
        tracks_tab = new int[2];
        makeAllList();
    }

    void getAllFiles() {
        allMusic = new ArrayList<>();
        String sortOrder = MediaStore.MediaColumns.TITLE;

        Cursor cursor = getApplicationContext().getContentResolver().
                query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder);

        Log.d(TAG, "Start_getRaw"); //
        assert cursor != null;
        while (cursor.moveToNext()) {
            try {
                JSONObject object = new JSONObject();
                object.put("title", cursor.getString(cursor.getColumnIndex("title")));
                object.put("album", cursor.getString(cursor.getColumnIndex("album")));
                object.put("artist", cursor.getString(cursor.getColumnIndex("artist")));
                object.put("duration", cursor.getInt(cursor.getColumnIndex("duration")));
                object.put("source", cursor.getString(cursor.getColumnIndex("_data")));
                allMusic.add(object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "End_getRaw"); //

        cursor.close();
    }

    void makeAllList() {
        allCards = new ArrayList<>();
        Log.d(TAG, "List_Start");
        float density = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        int i = 0;
        boolean end = false;
        while (!end) {
            LinearLayout temp = new LinearLayout(this);
            temp.setOrientation(LinearLayout.HORIZONTAL);
            temp.setGravity(Gravity.CENTER);
            for (int j = 0; j < 4; j++) {
                if (i < allMusic.size()) {
                    final CardView view = createCard(allMusic.get(i), i, density);
                    temp.addView(view);
                    allCards.add(view);
                    tracks_tab[1] = temp.getChildCount();
                    i++;
                } else {
                    temp.setGravity(Gravity.START);
                    end = true;
                    tracks_tab[1] = temp.getChildCount();
                    break;
                }
            }
            temp.setLayoutParams(params);
            allTracks_list.addView(temp);
            tracks_tab[0] += 1;
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getApplicationContext(), object.getString("title") + i, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
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

    void highlightCard() {
        int pos = track;
        for (int i = 0; i < allCards.size(); i++) {
            if (i == pos) {
                allCards.get(i).setCardBackgroundColor(getResources().getColor(R.color.colorAccent));
                allTracks_scroll.smoothScrollTo(0, ((View) allCards.get(i).getParent()).getTop());
            } else {
                allCards.get(i).setCardBackgroundColor(getResources().getColor(R.color.darkBackground));
            }
        }
    }

    void cleanHiglight() {
        for (int i = 0; i < allCards.size(); i++) {
            allCards.get(i).setCardBackgroundColor(getResources().getColor(R.color.darkBackground));
        }
    }

    private void setSource(int i) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                ParcelFileDescriptor fileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(Uri.fromFile(new File(allMusic.get(i).getString("source"))), "r");
                assert fileDescriptor != null;
                FileDescriptor fileDescriptor1 = fileDescriptor.getFileDescriptor();
                player.setDataSource(fileDescriptor1);
            } else {
                player.setDataSource(allMusic.get(i).getString("source"));
            }
            player.prepare();
            seekbar.setMax(player.getDuration());
            setNowPlaying(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Play(int pos) {
        if (!player.isPlaying()) {
            if (pos != now_playing) {
                now_playing = pos;
                setSource(pos);
            }
            player.start();
        } else {
            if (pos != now_playing) {
                player.stop();
                player.reset();
                now_playing = pos;
                setSource(pos);
                player.start();
            } else {
                player.pause();
            }
        }
    }

    private void Next() {
        if (now_playing != allMusic.size() - 1) {
            Play(now_playing + 1);
        } else {
            //if loop
            Play(0);
        }
        cleanHiglight();
    }

    private void Prev() {
        if (now_playing != 0) {
            Play(now_playing - 1);
        } else {
            //if loop
            Play(allMusic.size() - 1);
        }
        cleanHiglight();
    }

    private void setNowPlaying(int pos) {
        try {
            curr_title.setText(allMusic.get(pos).getString("title"));
            curr_artist.setText(allMusic.get(pos).getString("artist"));
            curr_album.setText(allMusic.get(pos).getString("album"));
            Glide.with(getApplicationContext()).load(getMetadata(allMusic.get(pos).getString("source")).getEmbeddedPicture()).
                    placeholder(ContextCompat.getDrawable(getApplicationContext(), R.drawable.art_placeholder)).into(curr_artwork);
        } catch (Exception e) {
            e.printStackTrace();
        }
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