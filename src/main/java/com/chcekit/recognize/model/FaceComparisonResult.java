package com.chcekit.recognize.model;

import lombok.Data;

@Data
public class FaceComparisonResult {
    public final boolean success;
    public final float score;
}
