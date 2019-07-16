package com.danny_jiang.tracingsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.danny_jiang.tracinglibrary.bean.LetterFactory;
import com.danny_jiang.tracinglibrary.view.TracingLetterView;

public class MainActivity extends AppCompatActivity {

    private TracingLetterView letterView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        letterView = findViewById(R.id.letter);
        letterView.setLetterChar(LetterFactory.A);
    }
}
