/*
 * ScoreSplash displays the amount of score the player has earned for any correctly answered question
 * */

package com.webster.pokequiz.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.webster.pokequiz.R;

public class ScoreSplash extends Fragment {

    private int score;

    public ScoreSplash(int score) {
        this.score = score;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.score_popup, container, false);

        //Set the score text
        TextView scoreText = v.findViewById(R.id.score_splash_text);
        scoreText.setText(getString(R.string.splash, String.valueOf(score)));
        return v;
    }
}
