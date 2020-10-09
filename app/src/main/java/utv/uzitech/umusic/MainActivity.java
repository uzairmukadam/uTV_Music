package utv.uzitech.umusic;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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

    ImageView curr_artwork, full_artwork, play_pause, f_play_pause, shuffle_toggle;
    TextView curr_title, curr_artist, curr_album, full_title, full_details, full_curr, full_duration;
    SeekBar seekbar, full_seekbar;
    Handler handler;

    BroadcastReceiver receiver, mediaplayback;

    MediaPlayer player;

    int track = 0, now_playing = -1;

    boolean shuffle = false, backpressed = false;

    Toast customToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);


        fullScreen = findViewById(R.id.full_screen_playback);

        play_pause = findViewById(R.id.play_pause);
        curr_artwork = findViewById(R.id.curr_artwork);
        curr_title = findViewById(R.id.curr_title);
        curr_artist = findViewById(R.id.curr_artist);
        curr_album = findViewById(R.id.curr_album);
        seekbar = findViewById(R.id.curr_seekbar);

        f_play_pause = findViewById(R.id.full_play_pause);
        full_artwork = findViewById(R.id.full_artwork);
        full_title = findViewById(R.id.full_title);
        full_details = findViewById(R.id.full_details);
        full_curr = findViewById(R.id.full_curr_time);
        full_duration = findViewById(R.id.full_duration);
        full_seekbar = findViewById(R.id.full_seekbar);
        shuffle_toggle = findViewById(R.id.shuffle_view);

        getAllFiles();

        loadTracksTab();

        highlightCard();

        player = new MediaPlayer();

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (shuffle) {
                    Play(new Random().nextInt(allMusic.size()));
                } else {
                    Next();
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
                        onBackPressed();
                        break;
                    case "D_DOWN":
                        if (fullScreen.getVisibility() == View.GONE) {
                            track += 4;
                            if (track > allMusic.size() - 1) {
                                if (track < tracks_tab[0] * 4) {
                                    track = allMusic.size() - 1;
                                } else {
                                    track = 0;
                                }
                            }
                            highlightCard();
                        }
                        break;
                    case "D_UP":
                        if (fullScreen.getVisibility() == View.GONE) {
                            track -= 4;
                            if (track < 0) {
                                track = allMusic.size() - 1;
                            }
                            highlightCard();
                        } else {
                            shuffle = !shuffle;
                            setShuffleView();
                        }
                        break;
                    case "D_RIGHT":
                        if (fullScreen.getVisibility() == View.GONE) {
                            if (track != allMusic.size() - 1) {
                                track += 1;
                            } else {
                                track = 0;
                            }
                            highlightCard();
                        } else {
                            int seek = player.getCurrentPosition() + 10000;
                            player.seekTo(Math.min(seek, player.getDuration()));
                        }
                        break;
                    case "D_LEFT":
                        if (fullScreen.getVisibility() == View.GONE) {
                            if (track != 0) {
                                track -= 1;
                            } else {
                                track = allMusic.size() - 1;
                            }
                            highlightCard();
                        } else {
                            int seek = player.getCurrentPosition() - 10000;
                            player.seekTo(Math.max(seek, 0));
                        }
                        break;
                    case "D_ENTER":
                        allCards.get(track).performClick();
                        break;
                }
            }
        };

        mediaplayback = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra("Remote_Input");
                assert input != null;
                switch (input) {
                    case "BTN_PLAY":
                        if (now_playing >= 0) {
                            Play(now_playing);
                        } else {
                            Play(track);
                        }
                        cleanHiglight();
                        break;
                    case "BTN_PREV":
                        Prev();
                        cleanHiglight();
                        break;
                    case "BTN_NEXT":
                        Next();
                        cleanHiglight();
                        break;
                }
            }
        };

        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
            }
        };
        audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        registerReceiver(receiver, new IntentFilter("utv.uzitech.remote_input"));
        registerReceiver(mediaplayback, new IntentFilter("utv.uzitech.remote_input"));
    }

    private void setShuffleView() {
        if (shuffle) {
            shuffle_toggle.setBackgroundResource(R.drawable.shuffle_toggle);
        } else {
            shuffle_toggle.setBackgroundResource(0);
        }
    }

    private void loadTracksTab() {
        tracks_tab = new int[2];
        allCards = new ArrayList<>();
        float density = getResources().getDisplayMetrics().density;
        LinearLayout allTracks_list = findViewById(R.id.tracks_parent);
        initiateCustomExit(density);
        Log.d(TAG, "List_Start");
        int i = 0;
        boolean end = false;
        while (!end) {
            LinearLayout temp = new LinearLayout(this);
            for (int j = 0; j < 4; j++) {
                if (i < allMusic.size()) {
                    final CardView view = createCard(allMusic.get(i), density, i);
                    temp.addView(view);
                    allCards.add(view);
                    i++;
                } else {
                    end = true;
                    tracks_tab[0] = allTracks_list.getChildCount();
                    tracks_tab[1] = temp.getChildCount();
                    break;
                }
            }
            allTracks_list.addView(temp);
        }
        Log.d(TAG, "List_Done");
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
                object.put("source", cursor.getString(cursor.getColumnIndex("_data")));
                allMusic.add(object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "End_getRaw"); //

        cursor.close();
    }

    private CardView createCard(final JSONObject object, float density, final int pos) {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") CardView layout = (CardView) inflater.inflate(R.layout.track_card, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView title = layout.findViewById(R.id.track_title);
        ImageView art = layout.findViewById(R.id.track_artwork);

        try {
            title.setText(object.getString("title"));
            Glide.with(getApplicationContext()).load(getMetadata(object.getString("source")).getEmbeddedPicture()).
                    apply(new RequestOptions().override(500, 500)).
                    error(R.drawable.art_placeholder).into(art);
        } catch (Exception e) {
            e.printStackTrace();
        }

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fullScreen.getVisibility() == View.GONE) {
                    track = pos;
                    Play(pos);
                    highlightCard();
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
                ScrollView allTracks_scroll = findViewById(R.id.tracks_scroll);
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
        track = now_playing;
    }

    void activateFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition(fullScreen, new AutoTransition());
        }
        findViewById(R.id.now_playing_panel).setVisibility(View.GONE);
        findViewById(R.id.all_tracks_panel).setVisibility(View.GONE);
        fullScreen.setVisibility(View.VISIBLE);
    }

    void deactivateFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition(fullScreen, new AutoTransition());
        }
        fullScreen.setVisibility(View.GONE);
        findViewById(R.id.now_playing_panel).setVisibility(View.VISIBLE);
        findViewById(R.id.all_tracks_panel).setVisibility(View.VISIBLE);
        highlightCard();
    }

    private void setSource(int i) {
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
        seekbar.setProgress(0);
        full_seekbar.setProgress(0);
        seekbar.setMax(player.getDuration());
        full_seekbar.setMax(player.getDuration());
        handler = new Handler();

        Runnable runnable = new Runnable() {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            public void run() {
                try {
                    int mins = (player.getCurrentPosition() / 1000) / 60;
                    int secs = (player.getCurrentPosition() / 1000) % 60;
                    full_curr.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
                    seekbar.setProgress(player.getCurrentPosition());
                    full_seekbar.setProgress(player.getCurrentPosition());
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
        if (!shuffle) {
            if (now_playing != allMusic.size() - 1) {
                Play(now_playing + 1);
            } else {
                Play(0);
            }
        } else {
            Play(new Random().nextInt(allMusic.size()));
        }
    }

    private void Prev() {
        if (now_playing != 0) {
            Play(now_playing - 1);
        } else {
            Play(allMusic.size() - 1);
        }
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
            Glide.with(getApplicationContext()).load(getMetadata(allMusic.get(pos).getString("source")).
                    getEmbeddedPicture()).error(R.drawable.art_placeholder).
                    placeholder(R.drawable.art_placeholder).into(curr_artwork);
            Glide.with(getApplicationContext()).load(getMetadata(allMusic.get(pos).getString("source")).
                    getEmbeddedPicture()).error(R.drawable.art_placeholder).
                    into(full_artwork);
            dashboardNoti(allMusic.get(pos).getString("title"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dashboardNoti(String song) {
        Intent intent = new Intent();
        intent.setAction("utv.uzitech.dash_notif");
        String[] data = new String[]{"Now Playing", song, "music_notif"};
        intent.putExtra("Notification", data);
        sendBroadcast(intent);
    }

    private void initiateCustomExit(float density) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast,
                (ViewGroup) findViewById(R.id.toast_root));

        TextView msg = layout.findViewById(R.id.toast_text);
        msg.setText(R.string.exit_warning);
        customToast = new Toast(getApplicationContext());
        customToast.setGravity(Gravity.BOTTOM | Gravity.END, (int) (density * 8), (int) (density * 8));
        customToast.setDuration(Toast.LENGTH_LONG);
        customToast.setView(layout);
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
        if (fullScreen.getVisibility() == View.GONE) {
            if (backpressed) {
                player.release();
                customToast.cancel();
                super.onBackPressed();
            }
            backpressed = true;
            customToast.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    backpressed = false;
                }
            }, 3500);
        } else {
            deactivateFullscreen();
        }
    }
}