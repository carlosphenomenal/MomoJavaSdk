package io.github.carlosphenomenal;

public class StringUtils {

    public static String reverse(String value) {

        if (value == null) {
            return null;
        }

        return new StringBuilder(value)
                .reverse()
                .toString();
    }

}