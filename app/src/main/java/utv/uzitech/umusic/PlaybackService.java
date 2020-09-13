package utv.uzitech.umusic;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;

class PlaybackService {
    Activity activity;
    ArrayList<JSONObject> allMusic;
    MediaPlayer mediaPlayer;
    int curr;
    Runnable seekUpdate;
    Handler seekHandler;

    ImageView curr_art;
    TextView curr_title, curr_artist, curr_album;
    ImageButton play_pause;
    SeekBar seekBar;
    MediaMetadataRetriever retriever;

    PlaybackService(Activity activity, ArrayList<JSONObject> allMusic, ImageView curr_art, TextView curr_title, TextView curr_artist, TextView curr_album, ImageButton play_pause, SeekBar seekBar) {
        this.activity = activity;
        mediaPlayer = new MediaPlayer();
        this.allMusic = allMusic;
        this.curr_art = curr_art;
        this.curr_title = curr_title;
        this.curr_artist = curr_artist;
        this.curr_album = curr_album;
        this.play_pause = play_pause;
        this.seekBar = seekBar;
        curr = -1;
        retriever = new MediaMetadataRetriever();
    }

    void setMediaPlayer(int curr) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                ParcelFileDescriptor fileDescriptor = activity.getContentResolver().openFileDescriptor(Uri.fromFile(new File(allMusic.get(curr).getString("source"))), "r");
                assert fileDescriptor != null;
                FileDescriptor fileDescriptor1 = fileDescriptor.getFileDescriptor();
                mediaPlayer.setDataSource(fileDescriptor1);
                retriever.setDataSource(fileDescriptor1);
            } else {
                mediaPlayer.setDataSource(allMusic.get(curr).getString("source"));
                retriever.setDataSource(allMusic.get(curr).getString("source"));
            }
            setCurrData();
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void play(int curr) {
        if (!mediaPlayer.isPlaying()) {
            if (curr != this.curr) {
                this.curr = curr;
                setMediaPlayer(curr);
                mediaPlayer.start();
                play_pause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                mediaPlayer.start();
                play_pause.setImageResource(android.R.drawable.ic_media_pause);
            }
        } else {
            if (curr != this.curr) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                this.curr = curr;
                setMediaPlayer(curr);
                mediaPlayer.start();
                play_pause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                mediaPlayer.pause();
                play_pause.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    void playNext() {
        if (curr != allMusic.size()-1) {
            curr += 1;
            mediaPlayer.stop();
            mediaPlayer.reset();
            setMediaPlayer(curr);
            mediaPlayer.start();
            play_pause.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    void playPrev() {
        if (curr != 0) {
            curr -= 1;
            mediaPlayer.stop();
            mediaPlayer.reset();
            setMediaPlayer(curr);
            mediaPlayer.start();
            play_pause.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    void setCurrData() {
        try {
            curr_title.setText(allMusic.get(curr).getString("title"));
            curr_artist.setText(allMusic.get(curr).getString("artist"));
            curr_album.setText(allMusic.get(curr).getString("album"));
            if (allMusic.get(curr).has("artwork")) {
                curr_art.setImageBitmap((Bitmap) allMusic.get(curr).get("artwork"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void destroy() {
        mediaPlayer.stop();
    }
}
