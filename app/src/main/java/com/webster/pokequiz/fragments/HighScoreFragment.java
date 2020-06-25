/*
 * HighScoreFragment handles the game over game state when a new high score is achieved
 * */

package com.webster.pokequiz.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.webster.pokequiz.R;
import com.webster.pokequiz.dataClasses.LeaderboardEntry;


public class HighScoreFragment extends Fragment implements View.OnClickListener {

    //onClick values
    public static int TO_MENU = 0;

    private HighScoreListener gameManager;

    private Button mainMenuButton, addLeaderboardEntry, shareButton;
    private EditText name;

    private int score;
    private int numberOfQuestions;
    private boolean hasSubmitted = false;

    //Interface is used to communicate fragment actions with the game manager
    public interface HighScoreListener {
        void highScoreFragAction(int input);

        void addLeaderboardEntry(LeaderboardEntry e);
    }

    public HighScoreFragment(int score, int numberOfQuestions) {
        this.score = score;
        this.numberOfQuestions = numberOfQuestions;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Assign the game manager
        if (context instanceof HighScoreListener) {
            gameManager = (HighScoreListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement HighScoreListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.high_score_fragment, container, false);

        TextView numberOfQuestionsText = v.findViewById(R.id.high_score_questions);
        TextView scoreText = v.findViewById(R.id.high_score_score);
        mainMenuButton = v.findViewById(R.id.high_score_menu);
        addLeaderboardEntry = v.findViewById(R.id.high_score_add);
        shareButton = v.findViewById(R.id.high_score_share);
        name = v.findViewById(R.id.high_score_name);

        //Display the final score and the number of questions answered correctly
        numberOfQuestionsText.setText(String.valueOf(numberOfQuestions - 1));
        scoreText.setText(String.valueOf(score));

        //Set onClick listeners for buttons
        mainMenuButton.setOnClickListener(HighScoreFragment.this);
        addLeaderboardEntry.setOnClickListener(HighScoreFragment.this);
        shareButton.setOnClickListener(HighScoreFragment.this);

        //Set onEditorAction listeners for clicking enter when the keyboard is open
        name.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addLeaderboardEntry.performClick();
                    return true;
                }
                return false;
            }
        });
        return v;
    }

    @Override
    public void onClick(View v) {
        //Inform the game manager of the fragment action
        if (v == mainMenuButton) {
            gameManager.highScoreFragAction(TO_MENU);

        } else if (v == addLeaderboardEntry && validateName()) {
            //Save new high score
            if (!hasSubmitted) {
                hasSubmitted = true;
                gameManager.addLeaderboardEntry(new LeaderboardEntry(name.getText().toString().trim(), score));
                addLeaderboardEntry.setEnabled(false);
                name.setVisibility(View.INVISIBLE);
            }

        } else if (v == shareButton) {
            gameManager.highScoreFragAction(score);

        }
    }

    private boolean validateName() {
        //Validate user name input
        String input = name.getText().toString().trim();
        if (input.isEmpty()) {
            name.setError("Enter Your Name!");
            return false;
        } else if (input.length() < 3 || input.length() > 5) {
            name.setError("Name must be 3 to 5 characters!");
            return false;
        }
        return true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        gameManager = null;
    }
}
