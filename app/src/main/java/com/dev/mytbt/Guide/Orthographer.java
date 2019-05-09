package com.dev.mytbt.Guide;

import android.util.Log;

import com.dev.mytbt.Guide.SpellCheckers.SpellChecker;
import com.dev.mytbt.Guide.SpellCheckers.SpellCheckerEN;
import com.dev.mytbt.Guide.SpellCheckers.SpellCheckerPT;

import java.util.Locale;

/**
 * Created by Luís Henriques for MyTbt.
 * 05-03-2018, 16:35
 *
 * This class selects the SpellChecker according to the given locale
 */

class Orthographer {

    private static final String TAG = "tbtTbt"; // Debug ta

    private SpellChecker spellChecker;

    Orthographer(Locale locale) {

        String language = locale.getLanguage().toLowerCase();

        // create a spell checker according to the specified locale
        switch (language) {
            case "pt":
                spellChecker = new SpellCheckerPT();
                break;
            case "en":
                spellChecker = new SpellCheckerEN();
                break;
            default:
                Log.e(TAG, "ERROR: Cannot correct text. No spell checker exists for " + locale.getLanguage());
                break;
        }

    }

    /**
     * corrects the text according to the specified spell checker
     * @param string the text to be corrected
     * @return returns the corrected string
     */
    String correctText(String string) {

        // basic general corrections
        String unwantedChars = "[^,\\s\\p{L}0-9]";
        string = string.replaceAll(unwantedChars, " ");

        String tooManyWhiteSpaces = "[ \\t]{2,}";
        string = string.replaceAll(tooManyWhiteSpaces, " ");

        // spellchecker corrections
        if (spellChecker != null) {
            return spellChecker.fixText(string);
        } else {
            return string;
        }
    }
}
