/*
 * LeaderboardFragment handles displaying of saved leaderboard
 * */

package com.webster.pokequiz.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.webster.pokequiz.dataClasses.LeaderboardEntry;
import com.webster.pokequiz.R;

import java.util.ArrayList;

public class LeaderboardFragment extends Fragment implements View.OnClickListener {

    //onClick values
    public static final int TO_MENU = 1;

    private LeaderboardListener gameManager;

    //Vars used to display the leaderboard
    private ArrayList<LeaderboardEntry> leaderboard;
    private TextView name1, score1, name2, score2, name3, score3, name4, score4, name5, score5;
    private Button mainMenuButton;

    //Interface is used to communicate fragment actions with the game manager
    public interface LeaderboardListener {
        void leaderboardFragment(int input);
        ArrayList<LeaderboardEntry> leaderboardFragmentGetLeaderboard();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Assign the game manager
        if (context instanceof LeaderboardListener) {
            gameManager = (LeaderboardListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement gameOverListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.leaderboard_fragment, container, false);

        name1 = v.findViewById(R.id.leaderboard_row1_name);
        name2 = v.findViewById(R.id.leaderboard_row2_name);
        name3 = v.findViewById(R.id.leaderboard_row3_name);
        name4 = v.findViewById(R.id.leaderboard_row4_name);
        name5 = v.findViewById(R.id.leaderboard_row5_name);
        score1 = v.findViewById(R.id.leaderboard_row1_score);
        score2 = v.findViewById(R.id.leaderboard_row2_score);
        score3 = v.findViewById(R.id.leaderboard_row3_score);
        score4 = v.findViewById(R.id.leaderboard_row4_score);
        score5 = v.findViewById(R.id.leaderboard_row5_score);

        //Set onClick for the menu button
        mainMenuButton = v.findViewById(R.id.leaderboard_menu);
        mainMenuButton.setOnClickListener(LeaderboardFragment.this);

        //Get and display the leaderboard
        leaderboard = gameManager.leaderboardFragmentGetLeaderboard();
        displayLeaderboard();

        return v;
    }

    private void displayLeaderboard() {
        //Correctly displays the leaderboard from the ArrayList
        TextView[] names = {name1, name2, name3, name4, name5};
        TextView[] scores = {score1, score2, score3, score4, score5};
        for (int i = 0; i < leaderboard.size(); i++) {
            names[i].setText(leaderboard.get(i).getName());
            scores[i].setText(String.valueOf(leaderboard.get(i).getScore()));
        }
    }

    @Override
    public void onClick(View v) {
        //Inform the game manager of the fragment action
        if (v == mainMenuButton) {
            gameManager.leaderboardFragment(TO_MENU);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        gameManager = null;
    }
}
