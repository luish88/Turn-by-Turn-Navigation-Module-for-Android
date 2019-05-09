package com.dev.mytbt.Guide.SpellCheckers;

/**
 * Created by Luís Henriques for MyTbt.
 * 05-03-2018, 16:46
 */

public class SpellCheckerPT implements SpellChecker {
    @Override
    public String fixText(String text) {
        text = text.replace(" saída", "ª saída");
        text = text.replace(" saida", "ª saída");
        text = text.replace("rotatória", "rotunda");
        text = text.replace("keep", "mantenha-se à");
        text = text.replace("left", "esquerda");
        text = text.replace("right", "direita");
        text = text.replace("à curva", "na curva");
        text = text.replace("continuar", "continue");
        text = text.replace("chegada a", "chegará à");
        return text;
    }
}
