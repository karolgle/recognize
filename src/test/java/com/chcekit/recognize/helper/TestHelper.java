package com.chcekit.recognize.helper;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestHelper {
  public static Map<String, MultipartFile> fillFilesMap() throws IOException {
    ClassLoader classloader = Thread.currentThread()
                                    .getContextClassLoader();
    Map<String, MultipartFile> files = new HashMap<>();
    files.put("face1.jpg", new MockMultipartFile("face1.jpg", classloader.getResourceAsStream("img/face1.jpg")));
    files.put("face2.jpg", new MockMultipartFile("face2.jpg", classloader.getResourceAsStream("img/face2.jpg")));
    files.put("face3.jpg", new MockMultipartFile("face3.jpg", classloader.getResourceAsStream("img/face3.jpg")));
    files.put("face4.jpg", new MockMultipartFile("face4.jpg", classloader.getResourceAsStream("img/face4.jpg")));
    files.put("4_faces.jpg", new MockMultipartFile("4_faces.jpg", classloader.getResourceAsStream("img/4_faces.jpg")));
    files.put("id_doc_fr_front.jpg", new MockMultipartFile("id_doc_fr_front.jpg", classloader.getResourceAsStream("img/id_doc_fr_front.jpg")));
    files.put("id_doc_pl_front.jpg", new MockMultipartFile("id_doc_pl_front.jpg", classloader.getResourceAsStream("img/id_doc_pl_front.jpg")));
    files.put("id_doc_pl_back.jpg", new MockMultipartFile("id_doc_pl_back.jpg", classloader.getResourceAsStream("img/id_doc_pl_back.jpg")));
    files.put("some_text1.png", new MockMultipartFile("some_text1.png", classloader.getResourceAsStream("img/some_text1.png")));
    files.put("some_text1.jpg", new MockMultipartFile("some_text1.jpg", classloader.getResourceAsStream("img/some_text1.jpg")));
    return files;
  }
}
