/*
 * Pokemon stores the data of a Pokemon from the API
 * */

package com.webster.pokequiz.dataClasses;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.webster.pokequiz.GameManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.StringJoiner;

public class Pokemon {

    //Vars for API requests
    private Boolean imageRequired;
    private int urlID;

    //Vars for data storage
    private int id = 0;
    private String name = null;
    private transient Bitmap image = null;
    private ArrayList<String> allTypes;
    private ArrayList<String> allAbilities;

    public static final String ERROR = "ERROR";

    public Pokemon(Boolean imageRequired) {
        allTypes = new ArrayList<>();
        allAbilities = new ArrayList<>();
        this.imageRequired = imageRequired;
    }

    public String getName() {
        //Validate name and return it
        if (name == null) {
            return ERROR;
        }

        switch (name) {
            case "nidoran-m":
                name = "nidoran♂";
                break;
            case "nidoran-f":
                name = "nidoran♀";
                break;
            case "mr-mime":
                name = "mr. mime";
                break;
        }

        return name;
    }

    public int getId() {
        return id;
    }

    public String getTypes() {
        //Return concatenated types sorted alphabetically
        Collections.sort(allTypes);
        StringJoiner j = new StringJoiner(" & ");

        for (String s : allTypes) {
            j.add(s);
        }
        return j.toString();
    }

    public String getAbilities() {
        //Return concatenated abilities sorted alphabetically
        Collections.sort(allAbilities);
        StringJoiner j = new StringJoiner(" & ");

        for (String s : allAbilities) {
            j.add(s);
        }
        return j.toString();
    }

    public Bitmap getImageBitmap() {
        return image;
    }


    public boolean initialisePokemon(int idNum) {
        if (idNum == -1) {
            //idNum of -1 acts as a flag for no saved value
            Random random = new Random();

            if (GameManager.HARDMODE) {
                //URL id to a random value/pokemon between 1-808 inc
                urlID = random.nextInt(808) + 1;

            } else {
                //URL id to a random value/pokemon between 1-151 inc
                urlID = random.nextInt(151) + 1;

            }
        } else {
            //else use a pokemon from an incomplete question
            urlID = idNum;
        }

        StringBuilder data = new StringBuilder();

        try {
            //Connect to the random/loaded pokemon from the poke api
            URL url = new URL("https://pokeapi.co/api/v2/pokemon/" + urlID);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            //Read JSON from connection, write data in String Buffer
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
            reader.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.getCause();
        }

        //Parse JSON
        try {
            JSONObject pokemonJSON = new JSONObject(data.toString());

            //Assign name value
            name = pokemonJSON.getString("name");

            //Assign id value
            this.id = pokemonJSON.getInt("id");

            //Assign type values
            JSONArray types = pokemonJSON.getJSONArray("types");
            for (int i = 0; i < types.length(); i++) {
                JSONObject t = types.getJSONObject(i).getJSONObject("type");
                allTypes.add(t.getString("name"));
            }

            //Assign ability values
            JSONArray abilities = pokemonJSON.getJSONArray("abilities");
            for (int i = 0; i < abilities.length(); i++) {
                JSONObject a = abilities.getJSONObject(i).getJSONObject("ability");
                allAbilities.add(a.getString("name"));

            }

            //Get ImageBitmap
            //Only get an image for 1/4 of the pokemon generated for each question
            if (imageRequired) {
                //Attempt to load image from internal storage
                if (!loadFromStorage(getImageDir())) {
                    //Image file not present - retrieve from online source
                    JSONObject sprites = pokemonJSON.getJSONObject("sprites");
                    URL frontImage = new URL(sprites.getString("front_default"));
                    InputStream in = frontImage.openStream();
                    image = BitmapFactory.decodeStream(in);

                    //Save the image for future use
                    saveToStorage(image, getImageDir());
                }
                Log.d("Test" , String.valueOf(GameManager.folderSize(getImageDir()) / 1000));
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private File getImageDir() {
        //getter for image storage directory
        ContextWrapper cw = new ContextWrapper(GameManager.getContext());
        return cw.getDir("pokemon_images", Context.MODE_PRIVATE);
    }

    private void saveToStorage(Bitmap bitmapImage, File directory) throws IOException {
        //save image
        File path = new File(directory, "pokemon" + urlID + ".png");
        FileOutputStream fos;
        fos = new FileOutputStream(path);
        bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
    }

    private boolean loadFromStorage(File path) throws FileNotFoundException {
        //load image
        File file = new File(path.getAbsolutePath(), "pokemon" + urlID + ".png");
        if (file.exists()) {
            image = BitmapFactory.decodeStream(new FileInputStream(file));
            return true;
        } else {
            return false;
        }
    }
}