package com.chcekit.recognize.services;

import com.chcekit.recognize.model.ImageLabel;
import com.chcekit.recognize.model.MrzData;
import com.google.cloud.vision.v1.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoogleVisionService {

    private final MrzParserService mrzParser;
    private final FrenchMrzParserService frenchMrzParserService;

    @Autowired
    public GoogleVisionService(MrzParserService mrzParser, FrenchMrzParserService frenchMrzParserService) {
        this.mrzParser = mrzParser;
        this.frenchMrzParserService = frenchMrzParserService;
    }

   /**
   * Checks if image contains identification document
   * @param file
   * @return
   */
    public boolean detectIdDoc(MultipartFile file){

        Map<String, Float> thresholdMap = Maps.newHashMap(new ImmutableMap.Builder<String, Float>()
                .put("identity document", 0.6f)
                .put("passport", 0.6f)
                .put("driver's license", 0.6f)
                .build());
        return detectLabels(file, thresholdMap);
    }

  /**
   * Checks if image contains document
   * @param file
   * @return
   */
    public boolean detectProofOfAddress(MultipartFile file){
        Map<String, Float> thresholdMap = Maps.newHashMap(new ImmutableMap.Builder<String, Float>()
                .put("document", 0.6f)
                .put("text", 0.8f)
                .build());
        return detectLabels(file, thresholdMap);
    }
  /**
   * Checks if labels describing the image contains at least one label from the map of labels passed as parameter with enough high score
   * @param file
   * @return
   */
    private boolean detectLabels(MultipartFile file, Map<String, Float> searchForLabels) {
        ByteString imgBytes = null;
        try {
            imgBytes = ByteString.copyFrom(file.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Image img = Image.newBuilder().setContent(imgBytes).build();

      return resolveLabels(img).stream()
                               .filter(imageLabel -> searchForLabels.get(imageLabel.getDescription()) != null)
                               .anyMatch(imageLabel -> imageLabel.getScore() > searchForLabels.get(imageLabel.getDescription()));
    }

    public List<ImageLabel> resolveLabels(Image img) {
        try {
            Feature labelDetection = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                                                               .addFeatures(labelDetection)
                                                               .setImage(img)
                                                               .build();

            // Instantiates a client
            try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = vision.batchAnnotateImages(Arrays.asList(request));
                List<AnnotateImageResponse> responses = response.getResponsesList();

              responses.stream()
                       .filter(AnnotateImageResponse::hasError)
                       .findFirst()
                       .ifPresent(annotateImageResponse -> {
                         System.out.printf("Error: %s\n", annotateImageResponse.getError().getMessage());
                         throw new RuntimeException(annotateImageResponse.getError().getMessage());
                       });

              return responses.stream()
                              .flatMap(annotateImageResponse -> annotateImageResponse.getLabelAnnotationsList().stream())
                              .map(annotation -> new ImageLabel(annotation.getDescription(), annotation.getScore()))
                              .collect(Collectors.toList());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<FaceAnnotation> resolveFaces(Image img) {
        try {
            List<AnnotateImageRequest> requests = new ArrayList<>();
            List<FaceAnnotation> faceAnnotationsList = new ArrayList<>();

            Feature featureDetection = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                                                               .addFeatures(featureDetection)
                                                               .setImage(img)
                                                               .build();
            requests.add(request);

            // Instantiates a client
            try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        System.out.printf("Error: %s\n", res.getError().getMessage());
                        throw new RuntimeException(res.getError().getMessage());
                    }

                    for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
                      faceAnnotationsList.add(annotation);
                    }
                }
            }

            return faceAnnotationsList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MrzData detectMrzForPassportOrFrDocId(Image img) {
        try {
            List<String> textBlocks = this.ocr(img);

            int mrzStart = this.detectPassportMrz(textBlocks);
            if(mrzStart != -1) {
                return this.mrzParser.parse(getMrz(textBlocks, mrzStart));
            }

            mrzStart = this.detectFrenchIDMrz(textBlocks);
            if(mrzStart != -1) {
                return this.frenchMrzParserService.parse(getMrz(textBlocks, mrzStart));
            }

            return MrzData.builder().missing(true).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int detectPassportMrz(List<String> textBlocks) {
        // detect start of MRZ block (the last one)
        int mrzStart = -1;
        for (int i = 0; i < textBlocks.size(); i++) {
            final String block = textBlocks.get(i);
            if (this.mrzParser.isStartOfMrz(block)) {
                mrzStart = i;
            }
        }

        return mrzStart;
    }

    private int detectFrenchIDMrz(List<String> textBlocks) {
        // detect start of MRZ block (the last one)
        int mrzStart = -1;
        for (int i = 0; i < textBlocks.size(); i++) {
            final String block = textBlocks.get(i);
            if (this.frenchMrzParserService.isStartOfMrz(block)) {
                mrzStart = i;
            }
        }

        return mrzStart;
    }

    private String getMrz(List<String> textBlocks, int mrzStart) {
        StringBuilder mrz = new StringBuilder();
        for (int i = 0; i < textBlocks.size(); i++) {
            final String block = textBlocks.get(i);
            if (i >= mrzStart) {
                mrz.append(block);
            }
        }
        return mrz.toString();
    }

    private List<String> ocr(Image image) {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        Feature documentTextDetection = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                                                           .addFeatures(documentTextDetection)
                                                           .setImage(image)
                                                           .build();
        requests.add(request);

        // Instantiates a client
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    log.error(res.getError().getMessage());
                    throw new RuntimeException(res.getError().getMessage());
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                TextAnnotation documentTextAnnotation = res.getFullTextAnnotation();
                return detectTextBlocks(documentTextAnnotation);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Collections.emptyList();
    }

    private List<String> detectTextBlocks(TextAnnotation documentTextAnnotation) {
        List<String> blocks = new ArrayList<>();

        for (Page page : documentTextAnnotation.getPagesList()) {
            for (Block block : page.getBlocksList()) {
                StringBuilder blockText = new StringBuilder();
                for (Paragraph paragraph : block.getParagraphsList()) {
                    StringBuilder paraText = new StringBuilder();
                    for (Word word : paragraph.getWordsList()) {
                        StringBuilder wordText = new StringBuilder();
                        for (Symbol symbol : word.getSymbolsList()) {
                            wordText.append(symbol.getText());
                        }
                        paraText.append(wordText);
                    }

                    blockText.append(paraText);
                }
                blocks.add(blockText.toString());
            }
        }

        return blocks;
    }
}
