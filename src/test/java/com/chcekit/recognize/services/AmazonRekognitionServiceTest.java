package com.chcekit.recognize.services;

import com.amazonaws.services.rekognition.model.Image;
import com.chcekit.recognize.helper.TestHelper;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AmazonRekognitionServiceTest {

  @Autowired
  AmazonRekognitionService amazonRekognitionService;

  Map<String, MultipartFile> files = new HashMap<>();
  Map<String, Image> imageMap = new HashMap<>();
  @Before
  public void setUp() throws Exception {

    //fill files map
    files = TestHelper.fillFilesMap();

    //fill aws image map
    imageMap = files.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), o ->createImage(o.getKey())));
  }

  @Test
  public void testCompareFaces() {
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("face1.jpg"), imageMap.get("face1.jpg")).isSuccess()).isTrue();
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("face1.jpg"), imageMap.get("face2.jpg")).isSuccess()).isTrue();
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("face2.jpg"), imageMap.get("face4.jpg")).isSuccess()).isTrue();
    //pictures need to be in high resolution if not...
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("face1.jpg"), imageMap.get("face3.jpg")).isSuccess()).isFalse();
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("face1.jpg"), imageMap.get("face4.jpg")).isSuccess()).isFalse();
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("face2.jpg"), imageMap.get("face3.jpg")).isSuccess()).isFalse();
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("face3.jpg"), imageMap.get("face4.jpg")).isSuccess()).isFalse();

    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("face1.jpg"), imageMap.get("4_faces.jpg")).isSuccess()).isFalse();

    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("id_doc_fr_front.jpg"), imageMap.get("id_doc_fr_front.jpg")).isSuccess()).isTrue();
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("id_doc_pl_front.jpg"), imageMap.get("id_doc_pl_front.jpg")).isSuccess()).isTrue();
    softly.assertThat(amazonRekognitionService.compareFaces(imageMap.get("id_doc_pl_back.jpg"), imageMap.get("id_doc_pl_back.jpg")).isSuccess()).isFalse();

    softly.assertAll();
  }

  private Image createImage(String key){
    try {
      return new Image().withBytes(ByteBuffer.wrap(files.get(key).getBytes()));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
