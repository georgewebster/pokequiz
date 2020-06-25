/*
 * HelpFragment handles the application help screen
 * */

package com.webster.pokequiz.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.webster.pokequiz.R;

public class HelpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.help_fragment, container, false);

        //Set onClick for the PokeAPI image
        ImageView apiLogo = v.findViewById(R.id.pokeAPI_image);
        apiLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intentPokeAPI();
            }
        });
        return v;
    }

    private void intentPokeAPI() {
        //Open _api website in a browser
        String URL = "http://www.pokeapi.co";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(URL));
        startActivity(browserIntent);
    }
}
