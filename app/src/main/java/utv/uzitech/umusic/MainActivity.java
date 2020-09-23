package utv.uzitech.umusic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<JSONObject> allMusic;
    ArrayList<String> allAlbums, allArtists, allFav;

    DatabaseHelper database;
    Common common;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new DatabaseHelper(this);
        common = new Common(getApplicationContext());

        getData();

        loadView();
    }

    private void loadView() {
        float density = getResources().getDisplayMetrics().density;
        allTrackList(density);
        allAlbumList(density);
        allArtistList(density);
    }

    private void allArtistList(float density) {
        LinearLayout tracks_list = findViewById(R.id.artists_list);

        for (int i = 0; i < allArtists.size(); i++) {
            CardView view = createCardA(allArtists.get(i), density, i);
            tracks_list.addView(view);
        }
    }

    private void allAlbumList(float density) {
        LinearLayout tracks_list = findViewById(R.id.albums_list);

        for (int i = 0; i < allAlbums.size(); i++) {
            CardView view = createCardA(allAlbums.get(i), density, i);
            tracks_list.addView(view);
        }
    }

    private void allTrackList(float density){
        LinearLayout tracks_list = findViewById(R.id.tracks_list);

        for (int i = 0; i < allMusic.size(); i++) {
            CardView view = createCard(allMusic.get(i), density, i);
            tracks_list.addView(view);
        }
    }

    private CardView createCardA(String name, float density, final int i) {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") CardView layout = (CardView) inflater.inflate(R.layout.album_artist_card, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        TextView title = layout.findViewById(R.id.track_title);

        title.setText(name);

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), String.valueOf(i), Toast.LENGTH_SHORT).show();
            }
        });

        params.setMargins((int) density * 8, 0, (int) density * 8, 0);
        layout.setLayoutParams(params);

        layout.setCardBackgroundColor(getResources().getColor(R.color.darkBackground));

        return layout;
    }

    private CardView createCard(JSONObject object, float density, final int i) {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") CardView layout = (CardView) inflater.inflate(R.layout.track_card, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        ImageView artwork = layout.findViewById(R.id.track_art);
        TextView title = layout.findViewById(R.id.track_title);

        try {
            title.setText(object.getString("title"));
            Glide.with(getApplicationContext()).load(common.getMetadata(object.getString("source")).getEmbeddedPicture()).
                    apply(new RequestOptions().override(500, 500)).
                    placeholder(ContextCompat.getDrawable(getApplicationContext(), R.drawable.art_placeholder)).into(artwork);

        } catch (Exception e) {
            e.printStackTrace();
        }

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNowPlaying(i);
            }
        });

        params.setMargins((int) density * 8, 0, (int) density * 8, 0);
        layout.setLayoutParams(params);

        layout.setCardBackgroundColor(getResources().getColor(R.color.darkBackground));

        return layout;
    }

    private void getData() {
        allMusic = new ArrayList<>();
        allAlbums = getIntent().getStringArrayListExtra("allAlbums");
        allArtists = getIntent().getStringArrayListExtra("allArtists");
        ArrayList<String> temp = getIntent().getStringArrayListExtra("allMusic");
        assert temp != null;
        for (String str : temp) {
            try {
                allMusic.add(new JSONObject(str));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        checkFav();
        Log.d("MAIN", "done");//
    }

    private void checkFav() {
        allFav = new ArrayList<>();
        ArrayList<String> temp = database.getAllFav();
        if(!temp.isEmpty()){
            for(JSONObject object: allMusic){
                try {
                    if (temp.contains(object.getString("path"))) {
                        temp.remove(object.getString("path"));
                        allFav.add(object.getString("title"));
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(!temp.isEmpty()){
                for(String path: temp){
                    database.removeFav(path);
                }
            }
        }else {
            LinearLayout fav_sec = findViewById(R.id.fav_section);
            fav_sec.setVisibility(View.GONE);
        }
    }

    private void setNowPlaying(int pos){
        ImageView artwork = findViewById(R.id.curr_artwork);
        TextView title = findViewById(R.id.curr_title);
        TextView artist = findViewById(R.id.curr_artist);
        TextView album = findViewById(R.id.curr_album);

        JSONObject object = allMusic.get(pos);

        try {
            Glide.with(getApplicationContext()).load(common.getMetadata(object.getString("source")).getEmbeddedPicture())
                    .into(artwork);
            title.setText(object.getString("title"));
            artist.setText(object.getString("artist"));
            album.setText(object.getString("album"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}