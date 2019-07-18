package com.danny_jiang.tracinglibrary.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author danny.jiang
 */

public class LetterStrokeBean {

    public String id;
    public String style;

    @SerializedName("char")
    public String letter;
    public List<Strokes> strokes;

    public static class Strokes {
        public List<String> points;
    }

    public List<String> getCurrentStrokePoints(int currentStroke) {
        return strokes.get(currentStroke).points;
    }
}
