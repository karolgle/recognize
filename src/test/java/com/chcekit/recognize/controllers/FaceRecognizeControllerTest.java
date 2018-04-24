package com.chcekit.recognize.controllers;

import com.chcekit.recognize.helper.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class FaceRecognizeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private FaceRecognizeController faceRecognizeController;

  private Map<String, MultipartFile> files = new HashMap<>();

  @Before
  public void setUp() throws Exception {
    //fill files map
    files = TestHelper.fillFilesMap();
  }

  @Test
  public void contexLoads() throws Exception {
    assertThat(faceRecognizeController).isNotNull();
  }

  @Test
  public void shouldCollectDocumentsAndReturnOk() throws Exception {
    MockMultipartFile selfie = new MockMultipartFile("selfie", files.get("face1.jpg")
                                                                    .getBytes());
    MockMultipartFile id_front = new MockMultipartFile("id_front", files.get("id_doc_fr_front.jpg")
                                                                        .getBytes());
    MockMultipartFile id_back = new MockMultipartFile("id_back", files.get("id_doc_pl_back.jpg")
                                                                      .getBytes());

    MvcResult mvcResult = this.mockMvc.perform(multipart("/api/collect").file(selfie)
                                                                        .file(id_front)
                                                                        .file(id_back))
                                      .andExpect(status().isOk())
                                      .andReturn();

    assertThat(mvcResult.getResponse()
                        .getContentAsString()).containsIgnoringCase("\"faceDetected\":true")
                                              .containsIgnoringCase("\"sorrow\":\"VERY_UNLIKELY\"")
                                              .containsIgnoringCase("\"anger\":\"VERY_UNLIKELY\"")
                                              .containsIgnoringCase("\"joy\":\"VERY_UNLIKELY\"")
                                              .containsIgnoringCase("\"surprise\":\"VERY_UNLIKELY\"")
                                              .containsIgnoringCase("\"idFrontDocumentDetected\":true")
                                              .containsIgnoringCase("\"idBackDocumentDetected\":true")
                                              .containsIgnoringCase("\"mrz\":\"MrzData(input=IDFRA00000<<<<<<<<<<<<<<<<<<<<7640160306730326262LAURENCE<<DANI6616667F6AALAMYSTOCKPHOTODWTM1KWWW.ALAMY.COM, error=false, missing=false, passportCountryCode=null, nationalityCode=FRA, surname=00000, givenNames=[LAURENCE, DANI], documentNumber=null, birthYear=1966, birthMonth=16, birthDay=66, sex=null, validYear=null, validMonth=null, validDay=null)");
  }
}
