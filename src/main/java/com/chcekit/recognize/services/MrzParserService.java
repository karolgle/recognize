package com.chcekit.recognize.services;

import com.chcekit.recognize.model.MrzData;
import com.ibm.icu.text.Transliterator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

@Service
public class MrzParserService {

    private static final String MRZ_START_MARKER = "P<";

    private static final Transliterator latinConverter = Transliterator.getInstance("Any-Latin; NFD; [:M:] Remove; NFC; [^\\p{ASCII}] Remove");


    /**
     * Checks whether a given line is a beginning of the MRZ section.
     * This method should be tailored to respect all supported MRZ formats.
     *
     * @param text MRZ characters
     * @return true if a text begins with a valid/supported MRZ prefix
     */
    public boolean isStartOfMrz(String text) {
        // the below condition should be tailored to match DEMO documents!!!
        return text.startsWith(MRZ_START_MARKER);
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

            reader.skip(2); // skip P<

            // read passport country
            StringBuilder passportCountryBuilder = new StringBuilder();
            for(int i = 1; i <= 3; i++) {
                int character = reader.read();

                if(character == -1) {
                    return MrzData.builder().input(mrz).missing(true).build();
                }
                passportCountryBuilder.append((char)character);
            }
            final String passportCountryCode = passportCountryBuilder.toString();

            int delimiterStep = 0;
            int firstDelimiterPosition = 0;
            int secondDelimiterPosition = 0;

            int nameCharactersIterator = 1;

            StringBuilder surnameBuilder = new StringBuilder();
            StringBuilder firstNameBuilder = new StringBuilder();
            StringBuilder secondNameBuilder = new StringBuilder();
            while(nameCharactersIterator <= 39) {
                int character = reader.read();

                // names should always be present - fail fast if there is no data to process
                if(character == -1) {
                    return MrzData.builder().input(mrz).passportCountryCode(passportCountryCode).build();
                }

                if(delimiterStep == 0) {
                    if(character == '<') {
                        delimiterStep = 1;
                        firstDelimiterPosition = nameCharactersIterator;
                    } else {
                        surnameBuilder.append((char)character);
                    }
                } else if (delimiterStep == 1) {
                    // skip any '<' characters detected after the very first < with 3 characters threshold
                    if (nameCharactersIterator <= firstDelimiterPosition+3 && character == '<') {
                        nameCharactersIterator++;
                        continue;
                    }
                    // fill-in first name
                    else if (character != '<') {
                        firstNameBuilder.append((char)character);
                    }
                    // detect delimited before the second name
                    else {
                        delimiterStep = 2;
                        secondDelimiterPosition = nameCharactersIterator;
                    }
                } else if (delimiterStep == 2) {
                    // skip any '<' characters detected after the second < with 2 characters threshold
                    if (nameCharactersIterator <= secondDelimiterPosition+2 && character == '<') {
                        nameCharactersIterator++;
                        continue;
                    }
                    else if (character != '<') {
                        secondNameBuilder.append((char)character);
                    }
                    else {
                        nameCharactersIterator++;
                        break;
                    }
                }

                nameCharactersIterator++;
            }

            // at least the surname is required
            if(surnameBuilder.length() == 0) {
                // cannot detect surname
                return MrzData.builder().input(mrz).passportCountryCode(passportCountryCode).build();
            }

            final String surname = surnameBuilder.toString();

            if(firstNameBuilder.length() == 0) {
                // cannot detect first name
                return MrzData.builder().input(mrz).passportCountryCode(passportCountryCode).surname(surname).build();
            }

            final String firstName = firstNameBuilder.toString();
            final String secondName = secondNameBuilder.toString();
            final List<String> givenNames = Arrays.asList(firstName, secondName);

            // read passport number - it takes next 9 characters other than '<'
            StringBuilder passportNumberBuilder = new StringBuilder();
            for(int i = 1; i <= 9;) {
                int character = reader.read();

                if(character == -1) {
                    return MrzData.builder().input(mrz).passportCountryCode(passportCountryCode).surname(surname).givenNames(givenNames).build();
                }

                if(character == '<') {
                    continue;
                }

                passportNumberBuilder.append((char)character);
                i++;
            }

            final String passportNumber = passportNumberBuilder.toString();

            reader.skip(1); // skip control number

            // read nationality
            StringBuilder nationalityBuilder = new StringBuilder();
            for(int i = 1; i <= 3; i++) {
                int character = reader.read();

                if(character == -1) {
                    return MrzData.builder().input(mrz).passportCountryCode(passportCountryCode).surname(surname).givenNames(givenNames).documentNumber(passportNumber).build();
                }
                nationalityBuilder.append((char)character);
            }
            final String nationalityCode = nationalityBuilder.toString();

            // read birth date
            StringBuilder birthDateBuilder = new StringBuilder();
            for(int i = 1; i <= 6; i++) {
                int character = reader.read();

                if(character == -1) {
                    return MrzData.builder().input(mrz).passportCountryCode(passportCountryCode).surname(surname).givenNames(givenNames).documentNumber(passportNumber).nationalityCode(nationalityCode).build();
                }
                birthDateBuilder.append((char)character);
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

            reader.skip(1); // skip control number

            // read sex
            int sexRaw = reader.read();

            if(sexRaw == -1) {
                return MrzData.builder()
                        .input(mrz)
                        .passportCountryCode(passportCountryCode)
                        .surname(surname).givenNames(givenNames)
                        .documentNumber(passportNumber).nationalityCode(nationalityCode)
                        .birthYear(birthYear).birthMonth(birthMonth).birthDay(birthDay)
                        .build();
            }

            final String sex = sexRaw == 'M' ? "male" : "female";

            // read document validity date
            StringBuilder validityDateBuilder = new StringBuilder();
            for(int i = 1; i <= 6; i++) {
                int character = reader.read();

                if(character == -1) {
                    return MrzData.builder()
                            .input(mrz)
                            .passportCountryCode(passportCountryCode)
                            .surname(surname).givenNames(givenNames)
                            .documentNumber(passportNumber).nationalityCode(nationalityCode)
                            .birthYear(birthYear).birthMonth(birthMonth).birthDay(birthDay)
                            .sex(sex)
                            .build();
                }
                validityDateBuilder.append((char)character);
            }

            String validYear = "20" + validityDateBuilder.substring(0, 2);
            String validMonth = validityDateBuilder.substring(2, 4);
            String validDay = validityDateBuilder.substring(4, 6);

            if(!StringUtils.isNumeric(validYear) || !StringUtils.isNumeric(validMonth) || !StringUtils.isNumeric(validDay)) {
                validYear = validMonth = validDay = null;
            }

            return MrzData.builder()
                    .input(mrz)
                    .passportCountryCode(passportCountryCode)
                    .surname(surname).givenNames(givenNames)
                    .documentNumber(passportNumber).nationalityCode(nationalityCode)
                    .birthYear(birthYear).birthMonth(birthMonth).birthDay(birthDay)
                    .sex(sex)
                    .validYear(validYear).validMonth(validMonth).validDay(validDay)
                    .build();
        } catch (Exception e) {
            return MrzData.builder().input(mrzData).error(true).build();
        }
    }
}
