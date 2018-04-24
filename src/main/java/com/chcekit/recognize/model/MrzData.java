package com.chcekit.recognize.model;

import com.neovisionaries.i18n.CountryCode;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Getter
@ToString
public class MrzData {
  // stores original MRZ data extracted from the document.
  private final String input;

  //indicates that an error occurred during parsing/OCR.
  private final boolean error;

  //indicates that MRZ data is missing.
  private final boolean missing;

  private final String passportCountryCode;
  private final String nationalityCode;
  private final String surname;
  private List<String> givenNames;
  private final String documentNumber;
  private final String birthYear;
  private final String birthMonth;
  private final String birthDay;
  private final String sex;
  private final String validYear;
  private final String validMonth;
  private final String validDay;

    public String getPassportCountry() {
        final CountryCode country = CountryCode.getByCode(passportCountryCode, false);
        return country != null ? country.getName() : "";
    }

  public String getSurnameCapital() {
        return StringUtils.capitalize(surname == null ? null : surname.toLowerCase());
    }

    public List<String> getGivenNamesCapital() {
        return givenNames == null ? null : givenNames.stream().map(String::toLowerCase).map(StringUtils::capitalize).collect(Collectors.toList());
    }
}
