/*
 * Question fragment handles the displaying of each question in the quiz
 * */

package com.webster.pokequiz.fragments;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.webster.pokequiz.GameManager;
import com.webster.pokequiz.dataClasses.Pokemon;
import com.webster.pokequiz.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;

public class QuestionFragment extends Fragment implements View.OnClickListener {
    //Number of pokemon required per question
    private static final int NUM_POKEMON = 4;

    private QuestionListener gameManager;

    //Flags for displaying question
    private Boolean isSaveState = false;
    private Boolean isQuestionAnswered = false;
    private Boolean isQuestionGenerated = false;
    private Boolean isFragmentVisible = false;

    //GUI variables
    private LinearLayout hud;
    private TextView questionNumberText, scoreText, questionText;
    private ImageView pokemonImage;
    private Button button0, button1, button2, button3, buttonLifeline;
    private Dialog loading = null;

    //Question data variables
    private ArrayList<Pokemon> pokemons;
    private String correctAnswer;
    private int questionNumber, questionScore, questionDifficulty;

    //Timer to stop multiple user inputs in short succession
    private long lastClickTime = 0;

    //Private constructor to stop use by other classes
    private QuestionFragment() {
    }

    //Acts as public class constructor
    public static QuestionFragment newInstance(int questionNumber) {
        QuestionFragment qF = new QuestionFragment();
        qF.questionNumber = questionNumber;
        return qF;
    }

    //Interface is used to communicate fragment actions and requests with the game manager
    public interface QuestionListener {
        void questionFragmentAnswer(Boolean answer, int questionScore, int qNum);

        int questionFragmentGetScore();

        void questionFragmentUseLifeline();

        Boolean questionFragmentGetLifelineStatus();

        void questionFragmentTimeout();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Assign the game manager
        if (context instanceof QuestionListener) {
            gameManager = (QuestionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement questionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.question_fragment, container, false);
        //Set Visible flag
        isFragmentVisible = true;

        //Assign GUI variables
        hud = v.findViewById(R.id.hud);
        questionNumberText = v.findViewById(R.id.question_number);
        scoreText = v.findViewById(R.id.score);
        questionText = v.findViewById(R.id.questionText);
        pokemonImage = v.findViewById(R.id.questionImage);
        buttonLifeline = v.findViewById(R.id.buttonLifeline);
        button0 = v.findViewById(R.id.button0);
        button1 = v.findViewById(R.id.button1);
        button2 = v.findViewById(R.id.button2);
        button3 = v.findViewById(R.id.button3);

        //Set onClick for the lifeline and answer buttons
        button0.setOnClickListener(QuestionFragment.this);
        button1.setOnClickListener(QuestionFragment.this);
        button2.setOnClickListener(QuestionFragment.this);
        button3.setOnClickListener(QuestionFragment.this);
        buttonLifeline.setOnClickListener(QuestionFragment.this);

        //Hide GUI
        setTextVisible(false);
        setButtonsVisible(false);

        /*
         * Update the question if the background processes are complete
         * Otherwise show the loading dialog box
         * */
        if (isQuestionGenerated) {
            updateQuestion();
        } else {
            showLoadingDialog();
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        //Timer to stop multiple inputs in short succession
        if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
            return;
        }
        lastClickTime = SystemClock.elapsedRealtime();

        //Handle the onClick of the lifeline and answer buttons
        if (!isQuestionAnswered && v == buttonLifeline) {
            useLifeline();
        } else if (!isQuestionAnswered) {
            answerClicked((Button) v);
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        gameManager = null;
    }

    public void newQuestionGeneration() {
        //Generate a new question
        GenerateQuestion generateQuestion = new GenerateQuestion(this);
        generateQuestion.execute(NUM_POKEMON);
    }

    public void oldQuestionGeneration(int[] pokemon, int difficultyMod) {
        //Generate a question from a saved state
        this.questionDifficulty = difficultyMod;
        isSaveState = true;

        GenerateQuestion generateQuestion = new GenerateQuestion(this);
        generateQuestion.execute(NUM_POKEMON, pokemon[0], pokemon[1], pokemon[2], pokemon[3]);
    }

    private void showLoadingDialog() {
        //Show the loading dialog
        loading = new Dialog(Objects.requireNonNull(this.getContext()));
        loading.setContentView(R.layout.loading_popup);
        (Objects.requireNonNull(loading.getWindow())).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loading.setCanceledOnTouchOutside(false);
        loading.show();

        //Timeout if the load lasts more than 5 seconds
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (loading.isShowing()) {
                    gameManager.questionFragmentTimeout();
                }
            }
        }, 5000);
    }

    private void setTextVisible(Boolean setVisible) {
        //Show/Hide text fields
        TextView[] textViews = {questionNumberText, scoreText, questionText};
        if (setVisible) {
            for (TextView t : textViews) {
                t.setVisibility(View.VISIBLE);
            }
        } else {
            for (TextView t : textViews) {
                t.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void setButtonsVisible(Boolean setVisible) {
        //Show/Hide buttons
        Button[] buttons = {buttonLifeline, button0, button1, button2, button3};
        if (setVisible) {
            for (Button b : buttons) {
                b.setVisibility(View.VISIBLE);
            }
            hud.setVisibility(View.VISIBLE);
            pokemonImage.setVisibility(View.VISIBLE);
        } else {
            for (Button b : buttons) {
                b.setVisibility(View.INVISIBLE);
            }
            hud.setVisibility(View.INVISIBLE);
            pokemonImage.setVisibility(View.INVISIBLE);

        }
        //Disable lifeline button if it has been used on another question
        if (!gameManager.questionFragmentGetLifelineStatus()) {
            buttonLifeline.setEnabled(false);
        }

    }

    private void updateQuestion() {

        //Determine the question difficulty
        if (!isSaveState) {
            if (questionNumber <= 3) {
                questionDifficulty = 0;
            } else {
                questionDifficulty = questionNumber + new Random().nextInt(100);
            }
        }

        //Update the Pokemon image
        pokemonImage.setImageBitmap(pokemons.get(0).getImageBitmap());

        //Determine the question topic and associated score
        Random r = new Random();
        if (questionDifficulty < 50) {
            correctAnswer = pokemons.get(0).getName();
            questionText.setText(R.string.question_name);
            setAnswerButtons(0);
            questionScore = 100;
            questionScore += r.nextInt(6) * 10;

        } else if (questionDifficulty < 80) {
            correctAnswer = pokemons.get(0).getTypes();
            questionText.setText(R.string.question_type);
            setAnswerButtons(1);
            questionScore = 200;
            questionScore += r.nextInt(11) * 10;

        } else if (questionDifficulty < 105) {
            correctAnswer = pokemons.get(0).getAbilities();
            questionText.setText(R.string.question_ability);
            setAnswerButtons(2);
            questionScore = 300;
            questionScore += r.nextInt(16) * 10;

        } else {
            correctAnswer = String.valueOf(pokemons.get(0).getId());
            questionText.setText(R.string.question_id);
            setAnswerButtons(3);
            questionScore += 400;
            questionScore += r.nextInt(21) * 10;

        }

        //Double score on hard mode
        if (GameManager.HARDMODE) {
            questionScore = questionScore * 2;
        }

        //Double score every 5th question
        if (questionNumber % 5 == 0) {
            questionScore = questionScore * 2;
        }

        //Update the question number
        questionNumberText.setText(getString(R.string.question_number, String.valueOf(questionNumber)));

        //Update the question score
        float totalScore = gameManager.questionFragmentGetScore();
        if (totalScore >= 1000) {
            scoreText.setText(getString(R.string.question_score, String.valueOf(totalScore / 1000)));
        } else {
            scoreText.setText(String.valueOf((int) totalScore));
        }

        //Set text and buttons to visible
        setTextVisible(true);
        setButtonsVisible(true);
    }

    private void setAnswerButtons(int questionType) {
        //Set the answer buttons in a random order
        @SuppressWarnings("unchecked")
        ArrayList<Pokemon> pokemonShuffle = (ArrayList<Pokemon>) pokemons.clone();
        Collections.shuffle(pokemonShuffle);
        Button[] buttons = {button0, button1, button2, button3};

        for (int i = 0; i < pokemonShuffle.size(); i++) {
            switch (questionType) {
                case 0:
                    buttons[i].setText(pokemonShuffle.get(i).getName());
                    break;
                case 1:
                    buttons[i].setText(pokemonShuffle.get(i).getTypes());
                    break;
                case 2:
                    buttons[i].setText(pokemonShuffle.get(i).getAbilities());
                    break;
                case 3:
                    buttons[i].setText(String.valueOf(pokemonShuffle.get(i).getId()));
                    break;
            }
        }
    }

    private void answerClicked(final Button input) {
        //Called when an answer is chosen


        if (correctAnswer.equals(Pokemon.ERROR)) {
            //Return if an error has occurred
            return;
        }

        if (!isQuestionAnswered) {
            //Update the selected button graphics
            input.setBackgroundResource(R.drawable.answer_button_pressed);
            isQuestionAnswered = true;
        } else {
            return;
        }

        //Disable all answer buttons
        Button[] buttons = {button0, button1, button2, button3};
        for (Button b : buttons) {
            b.setEnabled(false);
        }

        //Delayed process, change the correct answer button
        new Handler().postDelayed(new Runnable() {
            public void run() {
                Button[] buttons = {buttonLifeline, button0, button1, button2, button3};
                for (Button b : buttons) {
                    if (b.getText().equals(correctAnswer)) {
                        b.setBackgroundResource(R.drawable.answer_button_correct);
                    }
                }

                //Update the game manager of the question answer after a further quarter of a second
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        if (correctAnswer.contentEquals(input.getText())) {
                            gameManager.questionFragmentAnswer(true, questionScore, questionNumber);
                        } else {
                            gameManager.questionFragmentAnswer(false, questionScore, questionNumber);
                        }
                    }
                }, 250);
            }
        }, 1250);

    }

    //Called when user selects to use their lifeline
    private void useLifeline() {
        buttonLifeline.setEnabled(false);

        //Update the game manager of the lifeline use
        gameManager.questionFragmentUseLifeline();

        //Determine the button with the correct answer
        Button[] buttons = {button0, button1, button2, button3};
        Button correctButton = null;
        int count = 0;
        for (Button b : buttons) {
            if (b.getText().equals(correctAnswer)) {
                correctButton = b;
                break;
            }
        }

        //Randomly disable set two incorrect answer buttons
        while (count < 2) {
            int i = new Random().nextInt(4);
            if (buttons[i] != correctButton && buttons[i].isEnabled()) {
                buttons[i].setEnabled(false);
                buttons[i].setBackgroundResource(R.drawable.answer_button_disabled);
                count++;
            }
        }
    }

    public int[] getPokemon() {
        //returns pokemon in the current question - Used in saving processes
        return new int[]{pokemons.get(0).getId(), pokemons.get(1).getId(), pokemons.get(2).getId(), pokemons.get(3).getId()};
    }

    public int getQuestionDifficulty() {
        //returns question difficulty in the current question - Used in saving processes
        return questionDifficulty;
    }



    /*
     * GenerateQuestion is an AsyncTask that handles the retrieval and storing of data from the API
     * */


    private static class GenerateQuestion extends AsyncTask<Integer, Void, Void> {

        //Weak reference to the question fragment that created the task
        private WeakReference<QuestionFragment> weakReference;

        GenerateQuestion(QuestionFragment questionFragment) {
            weakReference = new WeakReference<>(questionFragment);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //Initialise the Array List of Pokemon to be stored
            QuestionFragment questionFragment = weakReference.get();
            if (questionFragment == null || questionFragment.isRemoving()) {
                return;
            }
            questionFragment.pokemons = new ArrayList<>();
        }


        @Override
        protected Void doInBackground(Integer... integers) {
            QuestionFragment questionFragment = weakReference.get();
            Pokemon pokemon;

            if (questionFragment == null || questionFragment.isRemoving()) {
                return null;
            }

            //Arguments being greater than 1 represents a load state
            if (integers.length > 1) {
                for (int i = 1; i <= integers[0]; i++) {
                    /*
                     * Create a new pokemon object, initialise its data and
                     * stores it in the questions pokemons ArrayList.
                     * First pokemon requires an image.
                     * */
                    if (questionFragment.pokemons.isEmpty()) {
                        pokemon = new Pokemon(true);
                    } else {
                        pokemon = new Pokemon(false);
                    }
                    pokemon.initialisePokemon(integers[i]);
                    questionFragment.pokemons.add(pokemon);

                }
            } else {
                //Represents no save state being available
                for (int i = 0; i < integers[0]; i++) {
                    /*
                     * Create a new pokemon object, initialise its data and
                     * stores it in the questions pokemons ArrayList.
                     * First pokemon requires an image.
                     * */
                    if (questionFragment.pokemons.isEmpty()) {
                        do {
                            pokemon = new Pokemon(true);
                        } while (!pokemon.initialisePokemon(-1));
                        questionFragment.pokemons.add(pokemon);
                    } else {
                        do {
                            pokemon = new Pokemon(false);
                            pokemon.initialisePokemon(-1);
                        } while (!isUniquePokemon(questionFragment.pokemons, pokemon.getName(), pokemon.getTypes(), pokemon.getAbilities()));
                        questionFragment.pokemons.add(pokemon);
                    }
                }
            }
            return null;
        }

        //Validates that the pokemon share no common attributes, all answers will be unique
        private boolean isUniquePokemon(ArrayList<Pokemon> pList, String pName, String pTypes, String pAlilities) {
            for (Pokemon p : pList) {
                if (pName.equals(p.getName()) || pTypes.equals(p.getTypes()) || pAlilities.equals(p.getTypes())) {
                    return false;
                }
                if (pName.equals(Pokemon.ERROR)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            QuestionFragment questionFragment = weakReference.get();
            if (questionFragment == null || questionFragment.isRemoving()) {
                return;
            }

            //Update the Async Task Flag
            questionFragment.isQuestionGenerated = true;

            // Dismiss the loading fragment if it was displayed
            if (questionFragment.isFragmentVisible) {
                questionFragment.loading.dismiss();
                //Update the question
                questionFragment.updateQuestion();
            }
        }
    }
}
