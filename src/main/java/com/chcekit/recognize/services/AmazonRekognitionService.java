package com.chcekit.recognize.services;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.chcekit.recognize.model.FaceComparisonResult;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class AmazonRekognitionService {

  private AmazonRekognition rekognitionClient;

  @PostConstruct
  public void initIt() throws Exception {
    try {

      rekognitionClient = AmazonRekognitionClientBuilder
        .standard()
        .withRegion(Regions.EU_WEST_1)
        .withCredentials(new ProfileCredentialsProvider())
        .build();

    } catch (Exception e) {
      throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
        + "Please make sure that your credentials file is at the correct "
        + "location (/Users/userid.aws/credentials), and is in a valid format.", e);
    }
  }

  public FaceComparisonResult compareFaces(Image sourceImage, Image targetImage) {

    CompareFacesRequest compareFacesRequest = new CompareFacesRequest()
      .withSourceImage(sourceImage)
      .withTargetImage(targetImage);

    try {
      CompareFacesResult result = rekognitionClient.compareFaces(compareFacesRequest);

      if (result.getFaceMatches().size() == 1) {
        CompareFacesMatch compareFacesMatch = result.getFaceMatches().get(0);
        return new FaceComparisonResult(true, compareFacesMatch.getSimilarity());
      }

      return new FaceComparisonResult(false, 0);
    } catch (AmazonRekognitionException e) {
      e.printStackTrace();
      return new FaceComparisonResult(false, -1);
    }
  }

}
