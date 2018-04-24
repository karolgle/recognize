package com.chcekit.recognize.model;

import lombok.Data;

@Data
public class ImageLabel {
  public final String description;
  public final float score;
}
