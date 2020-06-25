/*  Main Activity, responsible for game management
 * Implements listeners from fragments that need to influence the flow of the app
 * */

package com.webster.pokequiz;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.webster.pokequiz.dataClasses.LeaderboardEntry;
import com.webster.pokequiz.fragments.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;


public class GameManager extends AppCompatActivity implements MainMenuFragment.MainMenuListener,
        QuestionFragment.QuestionListener, GameOverFragment.GameOverListener, LeaderboardFragment.LeaderboardListener, HighScoreFragment.HighScoreListener {

    //Fragments for question generation
    private QuestionFragment currentQuestionFragment;
    private QuestionFragment nextQuestionFragment;

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    //Save data variables
    private ArrayList<LeaderboardEntry> leaderboard;
    private int[] savedPokemon = {-1, -1, -1, -1};
    private int difficultyMod;

    //Game variables
    private Boolean isLifelineAvailable = true;
    public static Boolean HARDMODE = false;
    private int questionNumber = -1;
    private int totalScore = -1;

    MainMenuFragment mainMenuFragment;

    //Activity context required in the saving of images by the data classes
    //Unlikely to cause memory leaks due to the task being short running
    public static Context getContext() {
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        setContentView(R.layout.game_manager_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Create main menu fragment
        mainMenuFragment = new MainMenuFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, mainMenuFragment).commit();
    }


    @Override
    protected void onPause() {
        //Call save whenever the app is paused
        super.onPause();
        saveGameData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Attempt to load game data whenever the app is resumed, then set game up a new or loaded game
        if (loadGameData()) {
            loadGame();
        } else {
            newGame();
        }

        //Update the main menu buttons to reflect the game state
        mainMenuFragment.updateButtons(totalScore, HARDMODE);
    }

    @Override
    public void onBackPressed() {
        if (mainMenuFragment.isAdded()) {
            //Return from super class if main menu fragment is already displayed,
            //avoids fragment is showing error
            return;
        }
        super.onBackPressed();

        //Update the main menu buttons to reflect the game state
        mainMenuFragment.updateButtons(totalScore, HARDMODE);
    }


    /*
     * MainMenuFragmentListener methods
     */

    //Main menu fragment action handler
    @Override
    public void mainMenuFragment(int action) {
        //Start quiz
        if (action == MainMenuFragment.TO_QUIZ) {
            //-1 score is a flag for no saved data
            if (totalScore == -1)
                totalScore = 0;

            //transition to the quiz
            fragmentTransition(currentQuestionFragment, true, true);

        } else if (action == MainMenuFragment.TO_HELP) {
            //Start help fragment
            fragmentTransition(new HelpFragment(), false, true);

        } else if (action == MainMenuFragment.TO_LEADERBOARD) {
            //Start leaderboard fragment
            fragmentTransition(new LeaderboardFragment(), false, true);

        } else if (action == MainMenuFragment.TOGGLE_HARD) {
            //Toggle hard mode
            HARDMODE = !HARDMODE;
            mainMenuFragment.updateButtons(totalScore, HARDMODE);
            newGame();
        }
    }

    @Override
    public int mainMenuGetScore() {
        return totalScore;
    }


    /*
     * GameOverFragmentListener method
     */

    //Game over fragment action
    @Override
    public void gameOverFragment(int action) {
        if (action == GameOverFragment.TO_MENU) {
            //Return to main menu
            fragmentTransition(mainMenuFragment, true, false);
        }
    }

    /*
     * LeaderboardFragmentListener methods
     */

    //Leaderboard fragment action
    @Override
    public void leaderboardFragment(int action) {
        //Return to main menu
        if (action == LeaderboardFragment.TO_MENU) {
            fragmentTransition(mainMenuFragment, true, true);

        }
    }

    //Getter for the leaderboard array list
    @Override
    public ArrayList<LeaderboardEntry> leaderboardFragmentGetLeaderboard() {
        sortLeaderboard();
        return leaderboard;
    }

    /*
     * QuestionFragmentListener methods
     */

    //Question fragment input, on answering a question
    @Override
    public void questionFragmentAnswer(Boolean correct, int questionScore, final int questionNum) {
        if (correct) {
            //Update game variables
            totalScore += questionScore;
            questionNumber++;
            //Show gained score splash screen
            showScoreSplash(questionScore);

            //Delayed fragment transition to next question
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    fragmentTransition(nextQuestionFragment, true, false);
                    //Queue up next question
                    currentQuestionFragment = nextQuestionFragment;
                    try {
                        savedPokemon = currentQuestionFragment.getPokemon();
                    } catch (IndexOutOfBoundsException e) {
                        //Do not save if the pokemon are yet to be loaded
                    }

                    nextQuestionFragment = QuestionFragment.newInstance(questionNumber);
                    nextQuestionFragment.newQuestionGeneration();
                }
            }, 1000);

            //Incorrect answer
        } else {
            //Delayed fragment transition to game over screen
            new Handler().postDelayed(new Runnable() {
                public void run() {

                    sortLeaderboard();

                    //Transition to game over or new high score fragment
                    if (totalScore > leaderboard.get(leaderboard.size() - 1).getScore()) {
                        fragmentTransition(new HighScoreFragment(totalScore, questionNum), true, false);
                    } else {
                        fragmentTransition(new GameOverFragment(totalScore, questionNum), true, false);
                    }

                    //Tell the user if they didn't use their lifeline
                    if (isLifelineAvailable){
                        Toast.makeText(GameManager.this, "Don't Forget About Your Lifeline!", Toast.LENGTH_SHORT).show();
                    }

                    //Set game to new game state
                    newGame();
                }
            }, 1000);
        }
    }

    //Question fragment getter for total score
    @Override
    public int questionFragmentGetScore() {
        return totalScore;
    }

    //Question fragment getter for lifeline status
    @Override
    public Boolean questionFragmentGetLifelineStatus() {
        return isLifelineAvailable;
    }

    @Override
    public void questionFragmentTimeout() {
        Intent exitApp = new Intent(Intent.ACTION_MAIN);
        exitApp.addCategory(Intent.CATEGORY_HOME);
        exitApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(exitApp);
        Toast.makeText(context, "INTERNET CONNECTION REQUIRED", Toast.LENGTH_SHORT).show();
    }

    //Question fragment setter for using the lifeline
    @Override
    public void questionFragmentUseLifeline() {
        isLifelineAvailable = false;
    }


    /*
     * HighScoreFragmentListener methods
     */

    @Override
    public void highScoreFragAction(int input) {
        if (input == HighScoreFragment.TO_MENU) {

            fragmentTransition(mainMenuFragment, true, true);

        } else {
            String shareText = "I just got " + input + " score on PokeQuiz! " +
                    "Check it out on the Play Store and see if you can beat me! #PokeQuiz #Pokemon #PokeAPI";
            String shareImage = "_logo";

            Uri imageUri = Uri.parse("android.resource://" + getPackageName() + "/drawable/" + shareImage);
            Intent tweetIntent = new Intent(Intent.ACTION_SEND);
            tweetIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            tweetIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            tweetIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            tweetIntent.setType("text/plain");
            startActivity(Intent.createChooser(tweetIntent, "Share Your High Score!"));
        }
    }

    @Override
    public void addLeaderboardEntry(LeaderboardEntry e) {
        leaderboard.add(e);
    }


    //Set game state to new game
    private void newGame() {
        totalScore = -1;
        isLifelineAvailable = true;

        questionNumber = 1;
        currentQuestionFragment = QuestionFragment.newInstance(questionNumber);
        currentQuestionFragment.newQuestionGeneration();

        questionNumber = 2;
        nextQuestionFragment = QuestionFragment.newInstance(questionNumber);
        nextQuestionFragment.newQuestionGeneration();
    }

    //Set game state to loaded game
    private void loadGame() {
        currentQuestionFragment = QuestionFragment.newInstance(questionNumber);
        currentQuestionFragment.oldQuestionGeneration(savedPokemon, difficultyMod);
        questionNumber++;

        nextQuestionFragment = QuestionFragment.newInstance(questionNumber);
        nextQuestionFragment.newQuestionGeneration();

    }

    //Transition between fragments
    private void fragmentTransition(Fragment fragment, boolean rightToLeftAnim, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (rightToLeftAnim) {
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        } else {
            transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
        }

        transaction.replace(R.id.container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null).commit();
        } else {
            transaction.commit();
        }
    }

    //Add the score gained splash screen fragment
    private void showScoreSplash(int addedScore) {
        ScoreSplash scoreSplash = new ScoreSplash(addedScore);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        transaction.add(R.id.container, scoreSplash);
        transaction.commit();
    }

    //Cut leaderboard down to top 5 entries
    private void sortLeaderboard() {
        Collections.sort(leaderboard);

        while (leaderboard.size() > 5) {
            leaderboard.remove(0);
        }

        for (LeaderboardEntry e : leaderboard) {
            e.setName(e.getName().toUpperCase());
        }
        Collections.reverse(leaderboard);
    }

    //Default leaderboard, used when no leaderboard could be loaded
    private ArrayList<LeaderboardEntry> defaultLeaderboard() {
        ArrayList<LeaderboardEntry> defaultLeaderboard = new ArrayList<>();
        defaultLeaderboard.add(new LeaderboardEntry("Oak", 13530));
        defaultLeaderboard.add(new LeaderboardEntry("Ash", 8300));
        defaultLeaderboard.add(new LeaderboardEntry("Brock", 4010));
        defaultLeaderboard.add(new LeaderboardEntry("Misty", 1080));
        defaultLeaderboard.add(new LeaderboardEntry("Gary", 460));
        return defaultLeaderboard;
    }


    //Constants for Saving and Loading
    private static final String LEADERBOARD = "L";
    private static final String POKEMON0 = "P0";
    private static final String POKEMON1 = "P1";
    private static final String POKEMON2 = "P2";
    private static final String POKEMON3 = "P3";

    private static final String QUESTION_NUM = "Q";
    private static final String SCORE = "S";
    private static final String LIFELINE = "LL";
    private static final String QUESTION_DIF = "D";
    private static final String HARDMODE_SAVE = "H";


    //Save leaderboard data to shared prefs
    private void saveGameData() {
        difficultyMod = currentQuestionFragment.getQuestionDifficulty();

        SharedPreferences sharedPreferences = getSharedPreferences("pokequiz", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //Gson is used to serialize and deserialize Java objects to JSON.
        Gson gson = new Gson();
        //Save leaderboard
        String json = gson.toJson(leaderboard);
        editor.putString(LEADERBOARD, json);

        //Save Pokemon
        editor.putInt(POKEMON0, savedPokemon[0]);
        editor.putInt(POKEMON1, savedPokemon[1]);
        editor.putInt(POKEMON2, savedPokemon[2]);
        editor.putInt(POKEMON3, savedPokemon[3]);

        //Save game state
        editor.putInt(QUESTION_NUM, questionNumber);
        editor.putInt(SCORE, totalScore);
        editor.putInt(QUESTION_DIF, difficultyMod);
        editor.putBoolean(LIFELINE, isLifelineAvailable);
        editor.putBoolean(HARDMODE_SAVE, HARDMODE);
        editor.apply();

    }

    //Save leaderboard data from shared prefs
    private Boolean loadGameData() {
        SharedPreferences sharedPreferences = getSharedPreferences("pokequiz", MODE_PRIVATE);
        Gson gson = new Gson();

        //Load leaderboard
        String json = sharedPreferences.getString(LEADERBOARD, null);
        Type type = new TypeToken<ArrayList<LeaderboardEntry>>() {
        }.getType();
        leaderboard = gson.fromJson(json, type);

        //Load Pokemon Ids
        savedPokemon[0] = sharedPreferences.getInt(POKEMON0, -1);
        savedPokemon[1] = sharedPreferences.getInt(POKEMON1, -1);
        savedPokemon[2] = sharedPreferences.getInt(POKEMON2, -1);
        savedPokemon[3] = sharedPreferences.getInt(POKEMON3, -1);


        questionNumber = sharedPreferences.getInt(QUESTION_NUM, -1) - 1;
        totalScore = sharedPreferences.getInt(SCORE, -1);
        difficultyMod = sharedPreferences.getInt(QUESTION_DIF, -1);
        isLifelineAvailable = sharedPreferences.getBoolean(LIFELINE, true);
        HARDMODE = sharedPreferences.getBoolean(HARDMODE_SAVE, false);

        if (leaderboard == null)
            leaderboard = defaultLeaderboard();


        //Was data loaded
        return savedPokemon[0] != -1 && savedPokemon[1] != -1 && savedPokemon[2] != -1 && savedPokemon[3] != -1
                && questionNumber != -1 && totalScore != -1 && difficultyMod != -1;
    }

    public static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }
}
