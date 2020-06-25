/*
 * MainMenuFragment handles main menu of the game
 * */

package com.webster.pokequiz.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.webster.pokequiz.GameManager;
import com.webster.pokequiz.R;

public class MainMenuFragment extends Fragment implements View.OnClickListener {

    //onClick values
    public static final int TO_QUIZ = 1, TO_HELP = 2, TO_LEADERBOARD = 3, TOGGLE_HARD = 4;

    private MainMenuListener gameManager;

    private Button start_button, tutorial, leaderboard, hardMode;

    //Interface is used to communicate fragment actions with the game manager
    public interface MainMenuListener {
        void mainMenuFragment(int input);

        int mainMenuGetScore();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Assign the game manager
        if (context instanceof MainMenuListener) {
            gameManager = (MainMenuListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement mainMenuListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.main_menu_fragment, container, false);

        tutorial = v.findViewById(R.id.how_to_button);
        leaderboard = v.findViewById(R.id.leaderboard_button);
        start_button = v.findViewById(R.id.start_button);
        hardMode = v.findViewById(R.id.hard_mode_button);

        //Set onClick on the menu buttons
        start_button.setOnClickListener(MainMenuFragment.this);
        tutorial.setOnClickListener(MainMenuFragment.this);
        leaderboard.setOnClickListener(MainMenuFragment.this);
        hardMode.setOnClickListener(MainMenuFragment.this);

        //Update the button text
        updateButtons(gameManager.mainMenuGetScore(), GameManager.HARDMODE);

        return v;
    }

    @Override
    public void onClick(View v) {
        //Inform the game manager of the fragment action
        if (v == start_button) {
            gameManager.mainMenuFragment(TO_QUIZ);

        } else if (v == tutorial) {
            gameManager.mainMenuFragment(TO_HELP);

        } else if (v == leaderboard) {
            gameManager.mainMenuFragment(TO_LEADERBOARD);

        } else if (v == hardMode) {
            gameManager.mainMenuFragment(TOGGLE_HARD);

        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        gameManager = null;
    }

    public void updateButtons(int score, Boolean hardMode) {
        //Update the button text to reflect the game state
        if (hardMode) {
            this.hardMode.setText(R.string.hard_enabled);
        } else {
            this.hardMode.setText(R.string.hard_disabled);
        }

        if (score >= 0 && start_button != null) {
            start_button.setText(R.string.menu_continue);
            this.hardMode.setEnabled(false);
        } else {
            this.hardMode.setEnabled(true);
        }
    }
}
