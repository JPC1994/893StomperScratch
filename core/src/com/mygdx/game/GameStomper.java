package com.mygdx.game;

import com.badlogic.gdx.Game;

public class GameStomper extends Game {
    public GameState currentState;
    ScrLostRespawn scrRespawn;
    ScrGame scrGame;

    //got this screen switching code from the intothewoods group:https://github.com/spidermanchild/IntoTheWoodsMultScreens
    public void updateScreen(){
        if(currentState== GameState.GAME){
            setScreen(scrGame);
        }else if(currentState== GameState.DEAD) {
            setScreen(scrRespawn);
        }
    }

    @Override
    public void create () {

        scrGame = new ScrGame(this);
        scrRespawn= new ScrLostRespawn(this);
        currentState = GameState.GAME; //Set the current state to the main menu, and update it.
        updateScreen();
    }

    //TODO: Add a main menu for user to select character and map
    public enum GameState {
        DEAD, GAME
    }
}
