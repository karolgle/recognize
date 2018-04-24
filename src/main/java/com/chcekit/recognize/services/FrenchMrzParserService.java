package com.chcekit.recognize.services;

import com.chcekit.recognize.model.MrzData;
import com.ibm.icu.text.Transliterator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

@Service
public class FrenchMrzParserService {
    private static final String MRZ_START_MARKER = "IDFRA";

    private static final Transliterator latinConverter = Transliterator.getInstance("Any-Latin; NFD; [:M:] Remove; NFC; [^\\p{ASCII}] Remove");

    /**
     * Checks whether a given line is a beginning of the MRZ section.
     * This method should be tailored to respect all supported MRZ formats.
     *
     * @param text MRZ characters
     * @return true if a text begins with a valid/supported MRZ prefix
     */
    public boolean isStartOfMrz(String text) {
        return text != null && text.toUpperCase().startsWith(MRZ_START_MARKER);
    }

    /**
     * Ultra-forgiving MRZ parser.
     * Extracts surname, given names, document number, birth date, sex and validity date.
     *
     * @param mrzData MRZ text
     * @return structured MRZ data
     */
    public MrzData parse(String mrzData) {
        try {
            if (mrzData == null || mrzData.length() == 0) {
                return MrzData.builder().input(mrzData).missing(true).build();
            }

            final String mrzDataUpper = mrzData.toUpperCase();
            final String mrz = latinConverter.transform(mrzDataUpper);

            if (!isStartOfMrz(mrz)) {
                return MrzData.builder().input(mrzData).missing(true).build();
            }

            StringReader reader = new StringReader(mrz);

            reader.skip(5); // skip IDFRA

            StringBuilder surnameBuilder = new StringBuilder();
            int processedCharacters = 0;
            while(processedCharacters <= 25) {
                int character = reader.read();

                // names should always be present - fail fast if there is no data to process
                if(character == -1) {
                    return MrzData.builder().input(mrz).missing(true).build();
                }

                if(character == '<') {
                    processedCharacters++;
                    break;
                }

                surnameBuilder.append((char)character);

                processedCharacters++;
            }

            boolean characterDetected = false;
            boolean lateCharacterDetected = false;
            for(int i = processedCharacters + 1; i <= 25+6; i++) {
                int character = reader.read();

                // names should always be present - fail fast if there is no data to process
                if(character == -1) {
                    return MrzData.builder().input(mrz).nationalityCode("FRA").surname(surnameBuilder.toString()).build();
                }


                if(character != '<') {
                    // it means that some '<' chars were omitted during OCR
                    if(i < 27) {
                        characterDetected = true;
                        break;
                    }
                    else {
                        lateCharacterDetected = true;
                        break;
                    }
                }
            }

            if(characterDetected) {
                reader.skip(5); // skip 5-7 of ID card number, department of issuance
            }

            // year of issuance, month of issuance, department of issuance, place of issue and the date of application, control digit
            if(lateCharacterDetected) {
                reader.skip(12);
            } else {
                reader.skip(13);
            }



            int nameCharactersIterator = 1;

            int delimiterStep = 0;
            int firstDelimiterPosition = 0;

            StringBuilder firstNameBuilder = new StringBuilder();
            StringBuilder secondNameBuilder = new StringBuilder();
            while(nameCharactersIterator <= 14) {
                int character = reader.read();

                // names should always be present - fail fast if there is no data to process
                if(character == -1) {
                    return MrzData.builder().input(mrz).nationalityCode("FRA").surname(surnameBuilder.toString()).build();
                }

                if(delimiterStep == 0) {
                    if(character == '<') {
                        delimiterStep = 1;
                        firstDelimiterPosition = nameCharactersIterator;
                    } else {
                        firstNameBuilder.append((char)character);
                    }
                } else if (delimiterStep == 1) {
                    // skip any '<' characters detected after the very first < with 2 characters threshold
                    if (nameCharactersIterator <= firstDelimiterPosition+2 && character == '<') {
                        nameCharactersIterator++;
                        continue;
                    }
                    // fill-in first name
                    else if (character != '<') {
                        secondNameBuilder.append((char)character);
                    }
                    // detect delimited before the second name
                    else {
                        nameCharactersIterator++;
                        break;
                    }
                }

                nameCharactersIterator++;
            }

            final String firstName = firstNameBuilder.toString();
            final String secondName = secondNameBuilder.toString();
            final List<String> givenNames = Arrays.asList(firstName, secondName);

            StringBuilder birthDateBuilder = new StringBuilder();
            for(int i = 1; i <= 6;) {
                int character = reader.read();

                if(character == -1) {
                    return MrzData.builder().input(mrz).nationalityCode("FRA").surname(surnameBuilder.toString()).givenNames(givenNames).build();
                }

                if(character == '<') {
                    continue;
                }

                birthDateBuilder.append((char)character);
                i++;
            }

            String birthYear = birthDateBuilder.substring(0, 2);

            try {
                birthYear = Integer.parseInt(birthYear) > 17 ? "19" + birthYear : "20" + birthYear;
            } catch(NumberFormatException e) {
                birthYear = null;            }

            String birthMonth = birthDateBuilder.substring(2, 4);
            String birthDay = birthDateBuilder.substring(4, 6);

            if(!StringUtils.isNumeric(birthYear) || !StringUtils.isNumeric(birthMonth) || !StringUtils.isNumeric(birthDay)) {
                birthYear = birthMonth = birthDay = null;
            }

            return MrzData.builder()
                    .input(mrz)
                    .surname(surnameBuilder.toString()).givenNames(givenNames)
                    .birthYear(birthYear).birthMonth(birthMonth).birthDay(birthDay)
                    .nationalityCode("FRA")
                    .build();

        } catch (Exception e) {
            return MrzData.builder().input(mrzData).error(true).build();
        }
    }
}
