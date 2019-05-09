package com.dev.mytbt.Guide.SpellCheckers;

/**
 * Created by Luís Henriques for MyTbt.
 * 17-04-2018, 17:05
 */
public class SpellCheckerEN implements SpellChecker {
    @Override
    public String fixText(String text) {
        text = text.replace("1 th", "first");
        text = text.replace("2 th", "second");
        text = text.replace("3 th", "third");
        text = text.replace("1th", "first");
        text = text.replace("2th", "second");
        text = text.replace("3th", "third");
        text = text.replace("1st", "first");
        text = text.replace("2nd", "second");
        text = text.replace("3rd", "third");
        return text;
    }
}
