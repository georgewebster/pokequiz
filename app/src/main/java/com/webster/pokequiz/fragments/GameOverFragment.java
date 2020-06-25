/*
 * GameOverFragment handles the game over game state
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

import com.webster.pokequiz.R;


public class GameOverFragment extends Fragment implements View.OnClickListener {

    //onClick values
    public static final int TO_MENU = 1;
    private GameOverListener gameManager;

    private Button mainMenuButton;

    private int numberOfQuestions;
    private int score;

    //Interface is used to communicate fragment actions with the game manager
    public interface GameOverListener {
        void gameOverFragment(int action);
    }


    public GameOverFragment(int score, int numberOfQuestions) {
        this.score = score;
        this.numberOfQuestions = numberOfQuestions;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Assign the game manager
        if (context instanceof GameOverListener) {
            gameManager = (GameOverListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement gameOverListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.game_over_fragment, container, false);

        //Display the final score and the number of questions answered correctly
        TextView numberOfQuestionsText = v.findViewById(R.id.game_over_questions);
        TextView scoreText = v.findViewById(R.id.game_over_score);
        numberOfQuestionsText.setText(String.valueOf(numberOfQuestions - 1));
        scoreText.setText(String.valueOf(score));

        //Set onClick for the menu button
        mainMenuButton = v.findViewById(R.id.game_over_menu);
        mainMenuButton.setOnClickListener(GameOverFragment.this);


        return v;
    }

    @Override
    public void onClick(View v) {
        if (v == mainMenuButton) {
            //Inform the game manager of the fragment action
            gameManager.gameOverFragment(TO_MENU);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        gameManager = null;
    }
}
