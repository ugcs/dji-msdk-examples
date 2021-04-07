package com.example.ugcssample.model.utils;

import com.example.ugcssample.model.utils.unitsystem.providers.length.ImperialLengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.length.LengthUnitProvider;
import com.example.ugcssample.model.utils.unitsystem.providers.speed.SpeedUnitProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class StringUtils {

    private static final String VALID_IP = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
        + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private static final String VALID_PATH = "(/[\\w\\-]+)+";

    public static final String EMPTY_STRING = "";

    // Predefined strings used for logging:
    public static final String NL = getLineSeparator();
    public static final String NL1T = NL + "\t";
    public static final String NL2T = NL1T + "\t";
    public static final String NL3T = NL2T + "\t";

    private StringUtils() {
        // forbidden
    }

    public static String nullToEmpty(String string) {
        return string == null ? EMPTY_STRING : string;
    }

    public static String emptyToNull(String string) {
        return isNullOrEmpty(string) ? null : string;
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static String toReadableString(String string) {
        return string == null ? "(null)" : (string.isEmpty() ? "(empty)" : string);
    }

    public static String getLineSeparator() {
        String lineSeparator = System.getProperty("line.separator");
        if (isNullOrEmpty(lineSeparator))
            lineSeparator = "\n";
        return lineSeparator;
    }

    public static String padLeft(String source, int targetLength, char padChar) {
        StringBuilder builder = new StringBuilder(targetLength);
        for (int i = source.length(); i < targetLength; ++i) {
            builder.append(padChar);
        }
        builder.append(source);
        return builder.toString();
    }

    public static String padRight(String source, int targetLength, char padChar) {
        StringBuilder builder = targetLength > 0 ? new StringBuilder(targetLength) : new StringBuilder();
        builder.append(source);
        for (int i = source.length(); i < targetLength; ++i) {
            builder.append(padChar);
        }
        return builder.toString();
    }

    public static int findIndexOf(String source, boolean ignoreCase, String... variants) {
        boolean isEmptySource = isNullOrEmpty(source);
        if (variants != null && variants.length > 0) {
            for (int index = 0; index < variants.length; index++) {
                String variant = variants[index];
                if (isEmptySource) {
                    if (isNullOrEmpty(variant))
                        return index;
                } else {
                    if (ignoreCase && source.equalsIgnoreCase(variant))
                        return index;
                    if (!ignoreCase && source.equals(variant))
                        return index;
                }
            }
        }
        return -1;
    }

    public static String findOneOf(String source, boolean ignoreCase, String... variants) {
        int index = findIndexOf(source, ignoreCase, variants);
        return index == -1 ? null : variants[index];
    }

    public static boolean isEqualOneOf(String source, boolean ignoreCase, String... variants) {
        if (isNullOrEmpty(source) && (variants == null || variants.length == 0))
            return true;
        return findIndexOf(source, ignoreCase, variants) != -1;
    }

    public static String[] split(String source, String regex) {
        if (isNullOrEmpty(source))
            return new String[0];
        if (isNullOrEmpty(regex))
            return new String[]{source};

        String[] split = source.split(regex);
        if (split == null || split.length == 0)
            return split;
        List<String> result = new ArrayList<String>();
        for (String item : split)
            if (!isNullOrEmpty(item))
                result.add(item);
        return result.toArray(new String[result.size()]);
    }

    public static String[] collectNotEmpty(String... items) {
        if (items == null || items.length == 0)
            return null;
        Set<String> result = new HashSet<String>();
        for (String item : items) {
            if (!isNullOrEmpty(item)) {
                item = item.trim();
                if (item.length() > 0)
                    result.add(item);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public static String trim(String source, String... trimedVariants) {
        if (isNullOrEmpty(source))
            return source;
        if (trimedVariants == null || trimedVariants.length == 0)
            return source;

        String result = source;
        for (String trimedVariant : trimedVariants) {
            if (isNullOrEmpty(trimedVariant))
                continue;
            int trimLen = trimedVariant.length();
            int resultLen = 0;
            while (resultLen != result.length()) {
                resultLen = result.length();
                if (result.startsWith(trimedVariant))
                    result = result.substring(trimLen);
                if (result.endsWith(trimedVariant))
                    result = result.substring(0, resultLen - trimLen);
            }
        }
        return result;
    }

    public static String exceptionToStirng(Throwable error, String usedNewLine) {
        if (error == null)
            return EMPTY_STRING;

        StringWriter errWriter = new StringWriter();
        error.printStackTrace(new PrintWriter(errWriter));
        String result = errWriter.toString();

        if (!isNullOrEmpty(usedNewLine) && !NL.equals(usedNewLine))
            result = result.replace(NL, usedNewLine);
        return result;
    }

    /**
     * Modify the size of a given string. The string can be shortened if it is too long, or padded with a given
     * character if it is too short. The boolean <code>before</code> tells if the change must occur at the front or at
     * the end of the string.
     *
     * @param str            the string to modify, may be null
     * @param length         the desired string length
     * @param before         True if insertion / cutting must happen at the front, False at the end
     * @param charToInsert   the char to insert if the string is too short
     * @param cuttingAllowed True if the string can be cut
     * @return The modified string
     */
    public static String resizeString(String str, int length, boolean before, String charToInsert,
                                      boolean cuttingAllowed) {

        if (str == null) {
            str = "";
        }
        if (charToInsert == null) {
            charToInsert = " ";
        }
        if (length == str.length()) {
            return str;
        }

        // str is too short, insert characters before or after
        if (length > str.length()) {
            StringBuilder sb = new StringBuilder(str);
            while (sb.length() < length) {
                if (before) {
                    sb.insert(0, charToInsert);
                } else {
                    sb.append(charToInsert);
                }
            }
            str = sb.toString();
        } else { // str is too long
            if (cuttingAllowed) {
                if (before) {
                    str = str.substring(str.length() - length, str.length());
                } else {
                    str = str.substring(0, length);
                }
            }
        }
        return str;
    }

    public static String join(String s, double[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            sb.append(String.format(Locale.US, "%.1f", values[i]));
            if (i + 1 < values.length) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    public static String join(String s, String... strings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            sb.append(strings[i]);
            if (i + 1 < strings.length) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    public static String join(String s, List<String> strings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            sb.append(strings.get(i));
            if (i + 1 < strings.size()) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    public static String join(Iterator<Integer> iterator, String s) {
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNext()) {
            sb.append((iterator.next() + 1));
            if (iterator.hasNext()) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    public static String joinAsIs(Iterator<Integer> iterator, String s) {
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNext()) {
            sb.append((iterator.next()));
            if (iterator.hasNext()) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    public static boolean validIp(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) return false;

        try {
            Pattern pattern = Pattern.compile(VALID_IP);
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    public static boolean validPath(String path) {
        if (path == null || path.isEmpty())
            return false;
        try {
            Pattern pattern = Pattern.compile(VALID_PATH);
            Matcher matcher = pattern.matcher(path);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    public static String length(double val) {
        return String.format(AppUtils.LOCALE, "%2.1f", val);
    }

    public static String valueLetter(double val, String l) {
        return String.format(Locale.US, "%2.1f %s", val, l);
    }

    public static String formatLength(double val, LengthUnitProvider lup) {
        return String.format(AppUtils.LOCALE, "%2.1f %s", lup.getFromMeters(val), lup.getDefLetter());
    }

    public static String formatLengthRoundedInCaseOfFt(double val, LengthUnitProvider lup) {
        if (lup instanceof ImperialLengthUnitProvider) {
            return String.format(AppUtils.LOCALE, "%2.0f %s", lup.getFromMeters(val), lup.getDefLetter());
        } else {
            return formatLength(val, lup);
        }
    }

    public static String formatLengthRoundedInCaseOfFtWithoutDefLetter(double valInMeters, LengthUnitProvider lup) {
        if (lup instanceof ImperialLengthUnitProvider) {
            return String.format(AppUtils.LOCALE, "%2.0f", lup.getFromMeters(valInMeters));
        } else {
            return String.format(AppUtils.LOCALE, "%2.1f", lup.getFromMeters(valInMeters));
        }
    }

    public static String formatSpeed(double speed, SpeedUnitProvider speedUnitProvider) {
        return String.format(AppUtils.LOCALE, "%2.1f",
            speedUnitProvider.getFromMetersPerSecond(speed));
    }

    public static String formatDeg(double val) {
        return String.format(AppUtils.LOCALE, "%2.1f deg", val);
    }

    public static String formatDegree(double val) {
        return String.format(AppUtils.LOCALE, "%2.1f°", val);
    }

    public static String formatLatLon(double val) {
        return String.format(AppUtils.LOCALE, "%1.7f°", val);
    }

    public static String formatLatLonOrEmptyString(Double val) {
        return val == null ? "" : String.format(AppUtils.LOCALE, "%1.7f", val);
    }

    public static String formatPrc(Integer prc) {
        return prc == null ? "--%"
            : String.format(AppUtils.LOCALE, "%2d%%", prc);
    }

    public static String formatDoubleShortOrEmptyString(Double value) {
        return value == null ? "" : String.format(AppUtils.LOCALE, "%2.1f", value);
    }

}
