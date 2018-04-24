package com.chcekit.recognize.controllers;

import com.chcekit.recognize.model.FaceComparisonResult;
import com.chcekit.recognize.model.ImageComparisonWebInfo;
import com.chcekit.recognize.model.ImageLabel;
import com.chcekit.recognize.model.MrzData;
import com.chcekit.recognize.services.AmazonRekognitionService;
import com.chcekit.recognize.services.GoogleVisionService;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController()
@RequestMapping(path = "/api")
public class FaceRecognizeController {

  private final String IDENTITY_DOCUMENT_LABEL = "identity document";

  private final GoogleVisionService googleVisionService;
  private final AmazonRekognitionService amazonRekognitionService;

  public FaceRecognizeController(GoogleVisionService googleVisionService, AmazonRekognitionService amazonRekognitionService) {
    this.googleVisionService = googleVisionService;
    this.amazonRekognitionService = amazonRekognitionService;
  }

  @PostMapping(path = "/collect")
  public ResponseEntity<?> collectDocuments(
    @RequestParam("selfie") MultipartFile selfie,
    @RequestParam("id_front") MultipartFile idFront,
    @RequestParam("id_back") MultipartFile idBack
  ) throws Exception {

    ImageComparisonWebInfo imageComparisonWebInfo = new ImageComparisonWebInfo();

    Image selfieImage = Image.newBuilder()
                             .setContent(ByteString.copyFrom(selfie.getBytes()))
                             .build();
    Image idFrontImage = Image.newBuilder()
                              .setContent(ByteString.copyFrom(idFront.getBytes()))
                              .build();
    Image idBackImage = Image.newBuilder()
                             .setContent(ByteString.copyFrom(idBack.getBytes()))
                             .build();

    // check if face detected


    // label/face detection using Google Vision API
    CompletableFuture<List<FaceAnnotation>> selfieFaceResolver = CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(selfieImage));
    CompletableFuture<List<ImageLabel>> idFrontResolver = CompletableFuture.supplyAsync(() -> googleVisionService.resolveLabels(idFrontImage));
    CompletableFuture<List<ImageLabel>> idBackResolver = CompletableFuture.supplyAsync(() -> googleVisionService.resolveLabels(idBackImage));

    CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(selfieFaceResolver, idFrontResolver, idBackResolver);

    // wait for all futures
    combinedFuture.get();

    // check whether selfie is a real selfie
    List<FaceAnnotation> selfieFaceResolverLabels = selfieFaceResolver.get();
    selfieFaceResolverLabels.stream()
                            .filter(faceAnnotation -> faceAnnotation.getDetectionConfidence() > 0.55)
                            .findFirst()
                            .ifPresent(faceAnnotation -> {
                              imageComparisonWebInfo.setFaceDetected(true);
                              imageComparisonWebInfo.setFaceScore(faceAnnotation.getDetectionConfidence());
                              imageComparisonWebInfo.setSorrow(faceAnnotation.getSorrowLikelihood()
                                                                             .name());
                              imageComparisonWebInfo.setAnger(faceAnnotation.getAngerLikelihood()
                                                                            .name());
                              imageComparisonWebInfo.setJoy(faceAnnotation.getJoyLikelihood()
                                                                          .name());
                              imageComparisonWebInfo.setSurprise(faceAnnotation.getSurpriseLikelihood()
                                                                               .name());
                            });


    // check whether other images contain identity documents

    List<ImageLabel> idFrontLabels = idFrontResolver.get();
    idFrontLabels.stream()
                 .filter(imageLabel -> IDENTITY_DOCUMENT_LABEL.equalsIgnoreCase(imageLabel.getDescription()))
                 .filter(imageLabel -> imageLabel.getScore() > 0.6)
                 .findFirst()
                 .ifPresent(imageLabel ->
                   {
                     imageComparisonWebInfo.setIdFrontDocumentDetected(true);
                     imageComparisonWebInfo.setIdFrontDocumentScore(imageLabel.getScore());
                   }
                 );


    List<ImageLabel> idBackLabels = idBackResolver.get();
    idBackLabels.stream()
                .filter(imageLabel -> IDENTITY_DOCUMENT_LABEL.equalsIgnoreCase(imageLabel.getDescription()))
                .filter(imageLabel -> imageLabel.getScore() > 0.6)
                .findFirst()
                .ifPresent(imageLabel ->
                  {
                    imageComparisonWebInfo.setIdBackDocumentDetected(true);
                    imageComparisonWebInfo.setIdBackDocumentScore(imageLabel.getScore());
                  }
                );


    // compare faces using AWS Rekognition
    if (imageComparisonWebInfo.isFaceDetected() && imageComparisonWebInfo.isIdFrontDocumentDetected()) {
      com.amazonaws.services.rekognition.model.Image selfieAwsImage =
        new com.amazonaws.services.rekognition.model.Image().withBytes(ByteBuffer.wrap(selfie.getBytes()));

      com.amazonaws.services.rekognition.model.Image frontIdAwsImage =
        new com.amazonaws.services.rekognition.model.Image().withBytes(ByteBuffer.wrap(idFront.getBytes()));

      FaceComparisonResult faceComparisonResult = this.amazonRekognitionService.compareFaces(selfieAwsImage, frontIdAwsImage);

      imageComparisonWebInfo.setFaceComparisonResult(faceComparisonResult.isSuccess());
      imageComparisonWebInfo.setFaceComparisonScore(faceComparisonResult.getScore());
    }

    // detect and read MRZ using dummy algorithm
    if (imageComparisonWebInfo.isIdFrontDocumentDetected()) {
      MrzData mrzDataFront = this.googleVisionService.detectMrzForPassportOrFrDocId(idFrontImage);

      if (!mrzDataFront.isMissing()) {
        imageComparisonWebInfo.setMrz(mrzDataFront.toString());
      } else if (imageComparisonWebInfo.isIdBackDocumentDetected()) {
        MrzData mrzDataBack = this.googleVisionService.detectMrzForPassportOrFrDocId(idBackImage);

        if (!mrzDataBack.isMissing()) {
          imageComparisonWebInfo.setMrz(mrzDataBack.toString());
        }
      }
    }
    return ResponseEntity.ok()
                         .body(imageComparisonWebInfo);
  }
}
