package com.igxxd.globalsearch.search.provider;

public class ProviderUtils {

    public static String formatCamelCase(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(input.charAt(0)));
        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(' ').append(c);
            } else if (c == '_' || c == '-') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
