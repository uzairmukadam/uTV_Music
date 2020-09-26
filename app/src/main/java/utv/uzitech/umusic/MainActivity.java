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
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    ArrayList<JSONObject> allMusic;
    ArrayList<CardView> allCards;
    int[] tracks_tab;

    FrameLayout fullScreen;
    LinearLayout allTracks_list;
    ScrollView allTracks_scroll;

    ImageButton play_pause, f_play_pause;

    ImageView curr_artwork, full_artwork;
    TextView curr_title, curr_artist, curr_album, full_title, full_details, full_curr, full_duration;
    SeekBar seekbar, f_seekbar;

    BroadcastReceiver receiver, mediaplayback;

    MediaPlayer player;

    int track = 0, now_playing = -1;

    Runnable runnable;

    boolean loop = true, shuffle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        allTracks_list = findViewById(R.id.tracks_parent);
        allTracks_scroll = findViewById(R.id.tracks_scroll);

        fullScreen = findViewById(R.id.full_screen_playback);

        curr_artwork = findViewById(R.id.curr_artwork);
        curr_title = findViewById(R.id.curr_title);
        curr_artist = findViewById(R.id.curr_artist);
        curr_album = findViewById(R.id.curr_album);
        seekbar = findViewById(R.id.curr_seekbar);
        play_pause = findViewById(R.id.play_pause);

        full_artwork = findViewById(R.id.full_artwork);
        full_title = findViewById(R.id.full_title);
        full_details = findViewById(R.id.full_details);
        full_curr = findViewById(R.id.full_curr_time);
        full_duration = findViewById(R.id.full_duration);
        f_seekbar = findViewById(R.id.full_seekbar);
        f_play_pause = findViewById(R.id.full_play_pause);

        getAllFiles();

        loadTracksTab();

        highlightCard();

        player = new MediaPlayer();

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (loop) {
                    if (shuffle) {
                        Play(new Random().nextInt(allMusic.size()));
                    } else {
                        Next();
                    }
                }
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra("Remote_Input");
                assert input != null;
                switch (input) {
                    case "BTN_MENU":
                        if (fullScreen.getVisibility() == View.GONE) {
                            activateFullscreen();
                        } else {
                            deactivateFullscreen();
                        }
                        break;
                    case "BTN_BACK":
                        if (fullScreen.getVisibility() == View.GONE) {
                            onBackPressed();
                        } else {
                            deactivateFullscreen();
                        }
                        break;
                    case "D_DOWN":
                        if (fullScreen.getVisibility() == View.GONE) {
                            track += 4;
                            if (track > allMusic.size() - 1) {
                                if (track > allMusic.size() + tracks_tab[1] - 1) {
                                    track = 0;
                                } else {
                                    track = allMusic.size() - 1;
                                }
                            }
                        }
                        break;
                    case "D_UP":
                        if (fullScreen.getVisibility() == View.GONE) {
                            track -= 4;
                            if (track < 0) {
                                track = allMusic.size() - 1;
                            }
                        }
                        break;
                    case "D_RIGHT":
                        if (fullScreen.getVisibility() == View.GONE) {
                            if (track != allMusic.size() - 1) {
                                track += 1;
                            } else {
                                track = 0;
                            }
                        }
                        break;
                    case "D_LEFT":
                        if (fullScreen.getVisibility() == View.GONE) {
                            if (track != 0) {
                                track -= 1;
                            } else {
                                track = allMusic.size() - 1;
                            }
                        }
                        break;
                    case "D_ENTER":
                        if (fullScreen.getVisibility() == View.GONE) {
                            Play(track);
                        }
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

        new Thread(runnable);

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

    void activateFullscreen() {
        findViewById(R.id.now_playing_panel).setVisibility(View.GONE);
        findViewById(R.id.all_tracks_panel).setVisibility(View.GONE);
        fullScreen.setVisibility(View.VISIBLE);
    }

    void deactivateFullscreen() {
        fullScreen.setVisibility(View.GONE);
        findViewById(R.id.now_playing_panel).setVisibility(View.VISIBLE);
        findViewById(R.id.all_tracks_panel).setVisibility(View.VISIBLE);
    }

    private void setSource(int i) {
        seekbar.setProgress(0);
        player.reset();
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
            setNowPlaying(i);
            setAudioProgress();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAudioProgress() {
        seekbar.setMax(player.getDuration());
        f_seekbar.setMax(player.getDuration());
        final Handler handler = new Handler();

        Runnable runnable = new Runnable() {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            public void run() {
                try {
                    int mins = (player.getCurrentPosition() / 1000) / 60;
                    int secs = (player.getCurrentPosition() / 1000) % 60;
                    full_curr.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
                    seekbar.setProgress(player.getCurrentPosition());
                    f_seekbar.setProgress(player.getCurrentPosition());
                    handler.postDelayed(this, 1000);
                } catch (IllegalStateException ed) {
                    ed.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    private void Play(int pos) {
        if (!player.isPlaying()) {
            if (pos != now_playing) {
                now_playing = pos;
                setSource(pos);
            }
            player.start();
            play_pause.setImageResource(R.drawable.pause);
            f_play_pause.setImageResource(R.drawable.pause);
        } else {
            if (pos != now_playing) {
                player.stop();
                play_pause.setImageResource(R.drawable.play);
                f_play_pause.setImageResource(R.drawable.play);
                player.reset();
                now_playing = pos;
                setSource(pos);
                player.start();
                play_pause.setImageResource(R.drawable.pause);
                f_play_pause.setImageResource(R.drawable.pause);
            } else {
                player.pause();
                play_pause.setImageResource(R.drawable.play);
                f_play_pause.setImageResource(R.drawable.play);
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

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void setNowPlaying(int pos) {
        try {
            curr_title.setText(allMusic.get(pos).getString("title"));
            full_title.setText(allMusic.get(pos).getString("title"));
            curr_artist.setText(allMusic.get(pos).getString("artist"));
            curr_album.setText(allMusic.get(pos).getString("album"));
            String details = allMusic.get(pos).getString("album") + " - " + allMusic.get(pos).getString("artist");
            full_details.setText(details);
            int mins = (player.getDuration() / 1000) / 60;
            int secs = (player.getDuration() / 1000) % 60;
            full_duration.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
            Glide.with(getApplicationContext()).load(getMetadata(allMusic.get(pos).getString("source")).getEmbeddedPicture()).
                    placeholder(ContextCompat.getDrawable(getApplicationContext(), R.drawable.art_placeholder)).into(curr_artwork);
            Glide.with(getApplicationContext()).load(getMetadata(allMusic.get(pos).getString("source")).getEmbeddedPicture()).
                    placeholder(ContextCompat.getDrawable(getApplicationContext(), R.drawable.art_placeholder)).into(full_artwork);
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

    @Override
    public void onBackPressed() {
        player.release();
        super.onBackPressed();
    }
}