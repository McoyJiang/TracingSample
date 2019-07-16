package com.danny_jiang.tracinglibrary.bean;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LetterFactory {

    public static final int A = 1;
    public static final int B = A + 1;
    public static final int C = B + 1;
    public static final int D = C + 1;
    public static final int E = D + 1;
    public static final int F = E + 1;
    public static final int G = F + 1;
    public static final int H = G + 1;
    public static final int I = H + 1;
    public static final int J = I + 1;
    public static final int K = J + 1;
    public static final int L = K + 1;
    public static final int M = L + 1;
    public static final int N = M + 1;
    public static final int O = N + 1;
    public static final int P = O + 1;
    public static final int Q = P + 1;
    public static final int R = Q + 1;
    public static final int S = R + 1;
    public static final int T = S + 1;
    public static final int U = T + 1;
    public static final int V = U + 1;
    public static final int W = V + 1;
    public static final int X = W + 1;
    public static final int Y = X + 1;
    public static final int Z = Y + 1;

    public String getLetterAssets() {
        return "letter/" + letter + "_bg.png";
    }

    public String getTracingAssets() {
        return "trace/" + letter + "_tracing.png";
    }

    public String getStrokeAssets() {
        return "strokes/" + letter + "_PointsInfo.json";
    }

    @IntDef({A, B, C, D, E, F, G,
            H, I, J, K, L, M, N,
            O, P, Q, R, S, T,
            U, V, W, X, Y, Z})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Letter {
    }

    private int letter = A;

    public void setLetter(@Letter int letterChar) {
        this.letter = letterChar;
    }
}
