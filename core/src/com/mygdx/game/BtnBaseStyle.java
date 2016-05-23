package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/**
 * Created by Rueban Rasaselvan on 23/05/2016.
 */
//intothewoods group:https://github.com/spidermanchild/IntoTheWoodsMultScreens
public class BtnBaseStyle extends TextButton.TextButtonStyle {
    Skin skin = new Skin();
    TextureAtlas taAtlas;

    public BtnBaseStyle() {
        BitmapFont font = new BitmapFont();
        skin.add("default", font);
        taAtlas = new TextureAtlas(Gdx.files.internal("images/UpButton.pack"));
        skin.addRegions(taAtlas);
        this.up = skin.getDrawable("ArrowUp");
        this.down = skin.getDrawable("PressedArrowUp");
        this.font = skin.getFont("default");
    }
}

