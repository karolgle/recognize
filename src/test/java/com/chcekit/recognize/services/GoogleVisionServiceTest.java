package com.chcekit.recognize.services;

import com.chcekit.recognize.helper.TestHelper;
import com.chcekit.recognize.model.MrzData;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GoogleVisionServiceTest {


  @Autowired
  GoogleVisionService googleVisionService;

  private Map<String, MultipartFile> files = new HashMap<>();
  private Map<String, Image> imageMap = new HashMap<>();
  @Before
  public void setUp() throws Exception {
    //fill files map
    files = TestHelper.fillFilesMap();

    //fill google image map
    imageMap = files.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, o ->createImage(o.getKey())));

  }

  @Test
  public void shouldNotDetectIdDoc() {

    assertThat(googleVisionService.detectIdDoc(files.get("face1.jpg"))).isFalse();
    assertThat(googleVisionService.detectIdDoc(files.get("face2.jpg"))).isFalse();
    assertThat(googleVisionService.detectIdDoc(files.get("face3.jpg"))).isFalse();
    assertThat(googleVisionService.detectIdDoc(files.get("face4.jpg"))).isFalse();
    assertThat(googleVisionService.detectIdDoc(files.get("4_faces.jpg"))).isFalse();
    assertThat(googleVisionService.detectIdDoc(files.get("some_text1.png"))).isFalse();
    assertThat(googleVisionService.detectIdDoc(files.get("some_text1.jpg"))).isFalse();
  }

  @Test
  public void shouldDetectIdDoc() {
    assertThat(googleVisionService.detectIdDoc(files.get("id_doc_fr_front.jpg"))).isTrue();
    assertThat(googleVisionService.detectIdDoc(files.get("id_doc_pl_front.jpg"))).isTrue();
    assertThat(googleVisionService.detectIdDoc(files.get("id_doc_pl_back.jpg"))).isTrue();
  }

  @Test
  public void shouldNotDetectProofOfAddress() {
      assertThat(googleVisionService.detectProofOfAddress(files.get("face1.jpg"))).isFalse();
      assertThat(googleVisionService.detectProofOfAddress(files.get("face2.jpg"))).isFalse();
      assertThat(googleVisionService.detectProofOfAddress(files.get("face3.jpg"))).isFalse();
      assertThat(googleVisionService.detectProofOfAddress(files.get("face4.jpg"))).isFalse();
      assertThat(googleVisionService.detectProofOfAddress(files.get("4_faces.jpg"))).isFalse();
      assertThat(googleVisionService.detectProofOfAddress(files.get("id_doc_fr_front.jpg"))).isFalse();
      assertThat(googleVisionService.detectProofOfAddress(files.get("id_doc_pl_front.jpg"))).isFalse();
      assertThat(googleVisionService.detectProofOfAddress(files.get("id_doc_pl_back.jpg"))).isFalse();
  }
  @Test
  public void shouldDetectProofOfAddress() {
      assertThat(googleVisionService.detectProofOfAddress(files.get("some_text1.png"))).isTrue();
      assertThat(googleVisionService.detectProofOfAddress(files.get("some_text1.jpg"))).isTrue();
  }


  @Test
  public void shouldResolveFaces() throws ExecutionException, InterruptedException {
    //given
    //when
   CompletableFuture<List<FaceAnnotation>> face1Annotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("face1.jpg")));
   CompletableFuture<List<FaceAnnotation>> face2Annotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("face2.jpg")));
   CompletableFuture<List<FaceAnnotation>> face3Annotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("face3.jpg")));
   CompletableFuture<List<FaceAnnotation>> face4Annotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("face4.jpg")));
   CompletableFuture<List<FaceAnnotation>> facesAnnotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("4_faces.jpg")));
   CompletableFuture<List<FaceAnnotation>> frFrontIdAnnotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("id_doc_fr_front.jpg")));
   CompletableFuture<List<FaceAnnotation>> plFrontIdAnnotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("id_doc_pl_front.jpg")));
   CompletableFuture<List<FaceAnnotation>> plBackIdAnnotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("id_doc_pl_back.jpg")));
   CompletableFuture<List<FaceAnnotation>> poaJpgAnnotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("some_text1.jpg")));
   CompletableFuture<List<FaceAnnotation>> poaPngAnnotations =  CompletableFuture.supplyAsync(() -> googleVisionService.resolveFaces(imageMap.get("some_text1.png")));

    CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(face1Annotations, face2Annotations, face3Annotations, face4Annotations, facesAnnotations, frFrontIdAnnotations, plFrontIdAnnotations, plBackIdAnnotations, poaJpgAnnotations, poaPngAnnotations);

    // wait for all futures
    combinedFuture.get();

    //then
    assertThat(face1Annotations.get().size()).isOne();
    assertThat(face2Annotations.get().size()).isOne();
    assertThat(face3Annotations.get().size()).isOne();
    assertThat(face4Annotations.get().size()).isOne();
    assertThat(facesAnnotations.get().size()).isEqualTo(4);
    assertThat(frFrontIdAnnotations.get().size()).isOne();
    assertThat(plFrontIdAnnotations.get().size()).isOne();
    assertThat(plBackIdAnnotations.get().size()).isZero();
    assertThat(poaJpgAnnotations.get().size()).isZero();
    assertThat(poaPngAnnotations.get().size()).isZero();
  }

  @Test
  public void shouldDetectMrzForPassportOrFrDocId() {
    //given
    //when

    MrzData mrzDataFace1 = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("face1.jpg"));
    MrzData mrzDataFace2 = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("face2.jpg"));
    MrzData mrzDataFace3 = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("face3.jpg"));
    MrzData mrzDataFace4 = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("face4.jpg"));
    MrzData mrzDataFaces = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("4_faces.jpg"));
    MrzData mrzDataIdFrontFr = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("id_doc_fr_front.jpg"));
    MrzData mrzDataIdFrontPl = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("id_doc_pl_front.jpg"));
    MrzData mrzDataIdBackPl = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("id_doc_pl_back.jpg"));
    MrzData mrzDataPoaJpg = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("some_text1.jpg"));
    MrzData mrzDataPoaPng = this.googleVisionService.detectMrzForPassportOrFrDocId(imageMap.get("some_text1.png"));

    //then
    assertThat(mrzDataFace1.isMissing()).isTrue();
    assertThat(mrzDataFace2.isMissing()).isTrue();
    assertThat(mrzDataFace3.isMissing()).isTrue();
    assertThat(mrzDataFace4.isMissing()).isTrue();
    assertThat(mrzDataFaces.isMissing()).isTrue();
    assertThat(mrzDataIdFrontPl.isMissing()).isTrue();
    assertThat(mrzDataIdBackPl.isMissing()).isTrue();
    assertThat(mrzDataPoaJpg.isMissing()).isTrue();
    assertThat(mrzDataPoaPng.isMissing()).isTrue();

    assertThat(mrzDataIdFrontFr.isMissing()).isFalse();
   assertThat(mrzDataIdFrontFr.getInput()).isEqualTo("IDFRA00000<<<<<<<<<<<<<<<<<<<<7640160306730326262LAURENCE<<DANI6616667F6AALAMYSTOCKPHOTODWTM1KWWW.ALAMY.COM");
  }

  private Image createImage(String key){
    try {
      return Image.newBuilder()
                  .setContent(ByteString.copyFrom(files.get(key).getBytes()))
                  .build();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
