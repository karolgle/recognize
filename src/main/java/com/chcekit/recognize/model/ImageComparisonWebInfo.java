package com.chcekit.recognize.model;

import lombok.Data;

@Data
public class ImageComparisonWebInfo {
  private boolean faceDetected;
  private float faceScore;
  private String sorrow;
  private String anger;
  private String joy;
  private String surprise;
  private boolean idFrontDocumentDetected;
  private float idFrontDocumentScore;
  private boolean idBackDocumentDetected;
  private float idBackDocumentScore;

  private boolean faceComparisonResult;
  private float faceComparisonScore;
  private String mrz = "";
}
