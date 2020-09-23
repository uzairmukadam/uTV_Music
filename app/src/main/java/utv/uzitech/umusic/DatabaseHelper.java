package utv.uzitech.umusic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    static String DATABASE_NAME = "uMusic.db";

    String fav_table = "favourite_tracks";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + fav_table + "(ID TEXT, PATH TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("drop table if exists " + fav_table);
        onCreate(sqLiteDatabase);
    }

    public void addFave(String title, String path) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("ID", title);
        contentValues.put("PATH", path);
        database.insert(fav_table, null, contentValues);
    }

    public void removeFav(String path) {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(fav_table, "PATH = \"" + path + "\"", null);
    }

    public ArrayList<String> getAllFav() {
        ArrayList<String> temp = new ArrayList<>();
        SQLiteDatabase database = this.getReadableDatabase();
        Cursor res = database.rawQuery("select PATH from " + fav_table, null);

        while (res.moveToNext()) {
            try {
                temp.add(res.getString(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        res.close();
        return temp;
    }
}
