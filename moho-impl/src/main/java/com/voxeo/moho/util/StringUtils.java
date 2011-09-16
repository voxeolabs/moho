package com.voxeo.moho.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * A collection of String utilities.
 */
public final class StringUtils {
  /** An empty string constant */
  public static final String EMPTY = "";

  public static final String SINGLE_QUOTE = "\'";

  public static final String DOUBLE_QUOTE = "\"";

  /** Millisecond conversion constants */
  private static final long MSEC = 1;

  private static final long SECS = 1000;

  private static final long MINS = 60 * 1000;

  private static final long HOUR = 60 * 60 * 1000;

  private static final char SLASH_CHAR = '/';

  private static final char BACKSLASH_CHAR = '\\';

  private static final String FILE_SEPARATOR = File.separator;

  private static final String PATH_SEPARATOR = File.pathSeparator;

  private static final String FILE_SEPARATOR_ALIAS = "/";

  private static final String PATH_SEPARATOR_ALIAS = ":";

  private static final int NORMAL = 0;

  private static final int SEEN_DOLLAR = 1;

  private static final int IN_BRACKET = 2;

  public static String[] addStringToArray(final String[] array, final String str) {
    if (Checker.isEmpty(array)) {
      return new String[] {str};
    }
    final String[] newArr = new String[array.length + 1];
    System.arraycopy(array, 0, newArr, 0, array.length);
    newArr[array.length] = str;
    return newArr;
  }

  public static String[] concatenateStringArrays(final String[] array1, final String[] array2) {
    if (Checker.isEmpty(array1)) {
      return array2;
    }
    if (Checker.isEmpty(array2)) {
      return array1;
    }
    final String[] newArr = new String[array1.length + array2.length];
    System.arraycopy(array1, 0, newArr, 0, array1.length);
    System.arraycopy(array2, 0, newArr, array1.length, array2.length);
    return newArr;
  }

  public static String[] mergeStringArrays(final String[] array1, final String[] array2) {
    if (Checker.isEmpty(array1)) {
      return array2;
    }
    if (Checker.isEmpty(array2)) {
      return array1;
    }
    final List<String> result = new ArrayList<String>();
    result.addAll(Arrays.asList(array1));
    for (int i = 0; i < array2.length; ++i) {
      final String str = array2[i];
      if (!result.contains(str)) {
        result.add(str);
      }
    }
    return toStringArray(result);
  }

  public static String[] removeDuplicateStrings(final String[] array) {
    if (Checker.isEmpty(array)) {
      return array;
    }
    final Set<String> set = new TreeSet<String>();
    for (int i = 0; i < array.length; ++i) {
      set.add(array[i]);
    }
    return toStringArray(set);
  }

  public static String[] sortStringArray(final String[] array) {
    if (Checker.isEmpty(array)) {
      return new String[0];
    }
    Arrays.sort(array);
    return array;
  }

  public static boolean isUpperCase(final String s) {
    if (Checker.isEmpty(s)) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLowerCase(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static String capitalize(final String str) {
    return changeFirstCharacterCase(str, true);
  }

  public static String uncapitalize(final String str) {
    return changeFirstCharacterCase(str, false);
  }

  private static String changeFirstCharacterCase(final String str, final boolean capitalize) {
    if (str == null || str.length() == 0) {
      return str;
    }
    final StringBuffer buf = new StringBuffer(str.length());
    if (capitalize) {
      buf.append(Character.toUpperCase(str.charAt(0)));
    }
    else {
      buf.append(Character.toLowerCase(str.charAt(0)));
    }
    buf.append(str.substring(1));
    return buf.toString();
  }

  public static int select(final String line, final int index) {
    try {
      final int i = Integer.parseInt(line);
      if (i >= 0 && i < index) {
        return i;
      }
      else {
        return -1;
      }
    }
    catch (final Exception e) {
      return -1;
    }
  }

  // ///////////////////////////////////////////////////////////////////////
  // Substitution Methods //
  // ///////////////////////////////////////////////////////////////////////

  /**
   * Substitute sub-strings in side of a string.
   * 
   * @param buff
   *          Stirng buffer to use for substitution (buffer is not reset)
   * @param from
   *          String to substitute from
   * @param to
   *          String to substitute to
   * @param string
   *          String to look for from in
   * @return Substituted string
   */
  public static String subst(final StringBuffer buff, final String from, final String to, final String string) {
    int begin = 0, end = 0;

    while ((end = string.indexOf(from, end)) != -1) {
      // append the first part of the string
      buff.append(string.substring(begin, end));

      // append the replaced string
      buff.append(to);

      // update positions
      begin = end + from.length();
      end = begin;
    }

    // append the rest of the string
    buff.append(string.substring(begin, string.length()));

    return buff.toString();
  }

  /**
   * Substitute sub-strings in side of a string.
   * 
   * @param from
   *          String to substitute from
   * @param to
   *          String to substitute to
   * @param string
   *          String to look for from in
   * @return Substituted string
   */
  public static String subst(final String from, final String to, final String string) {
    return subst(new StringBuffer(), from, to, string);
  }

  /**
   * Substitute sub-strings in side of a string.
   * 
   * @param buff
   *          String buffer to use for substitution (buffer is not reset)
   * @param string
   *          String to subst mappings in
   * @param map
   *          Map of from->to strings
   * @param beginToken
   *          Beginning token
   * @param endToken
   *          Ending token
   * @return Substituted string
   */
  public static String subst(final StringBuffer buff, final String string, final Map map, final String beginToken,
      final String endToken) {
    int begin = 0, rangeEnd = 0;
    Range range;

    while ((range = rangeOf(beginToken, endToken, string, rangeEnd)) != null) {
      // append the first part of the string
      buff.append(string.substring(begin, range.begin));

      // Get the string to replace from the map
      final String key = string.substring(range.begin + beginToken.length(), range.end);
      Object value = map.get(key);
      // if mapping does not exist then use empty;
      if (value == null) {
        value = EMPTY;
      }

      // append the replaced string
      buff.append(value);

      // update positions
      begin = range.end + endToken.length();
      rangeEnd = begin;
    }

    // append the rest of the string
    buff.append(string.substring(begin, string.length()));

    return buff.toString();
  }

  /**
   * Substitute sub-strings in side of a string.
   * 
   * @param string
   *          String to subst mappings in
   * @param map
   *          Map of from->to strings
   * @param beginToken
   *          Beginning token
   * @param endToken
   *          Ending token
   * @return Substituted string
   */
  public static String subst(final String string, final Map map, final String beginToken, final String endToken) {
    return subst(new StringBuffer(), string, map, beginToken, endToken);
  }

  /**
   * Substitute index identifiers with the replacement value from the given
   * array for the corresponding index.
   * 
   * @param buff
   *          The string buffer used for the substitution (buffer is not reset).
   * @param string
   *          String substitution format.
   * @param replace
   *          Array of strings whose values will be used as replacements in the
   *          given string when a token with their index is found.
   * @param token
   *          The character token to specify the start of an index reference.
   * @return Substituted string.
   */
  public static String subst(final StringBuffer buff, final String string, final String replace[], final char token) {
    final int i = string.length();
    for (int j = 0; j >= 0 && j < i; j++) {
      final char c = string.charAt(j);

      // if the char is the token, then get the index
      if (c == token) {

        // if we aren't at the end of the string, get the index
        if (j != i) {
          final int k = Character.digit(string.charAt(j + 1), 10);

          if (k == -1) {
            buff.append(string.charAt(j + 1));
          }
          else if (k < replace.length) {
            buff.append(replace[k]);
          }

          j++;
        }
      }
      else {
        buff.append(c);
      }
    }

    return buff.toString();
  }

  /**
   * Substitute index identifiers with the replacement value from the given
   * array for the corresponding index.
   * 
   * @param string
   *          String substitution format.
   * @param replace
   *          Array of strings whose values will be used as replacements in the
   *          given string when a token with their index is found.
   * @param token
   *          The character token to specify the start of an index reference.
   * @return Substituted string.
   */
  public static String subst(final String string, final String replace[], final char token) {
    return subst(new StringBuffer(), string, replace, token);
  }

  /**
   * Substitute index identifiers (with <code>%</code> for the index token) with
   * the replacement value from the given array for the corresponding index.
   * 
   * @param string
   *          String substitution format.
   * @param replace
   *          Array of strings whose values will be used as replacements in the
   *          given string when a token with their index is found.
   * @return Substituted string.
   */
  public static String subst(final String string, final String replace[]) {
    return subst(new StringBuffer(), string, replace, '%');
  }

  // ///////////////////////////////////////////////////////////////////////
  // Range Methods //
  // ///////////////////////////////////////////////////////////////////////

  /**
   * Represents a range between two integers.
   */
  public static class Range {
    /** The beginning of the range. */
    public int begin;

    /** The end of the range. */
    public int end;

    /**
     * Construct a new range.
     * 
     * @param begin
     *          The beginning of the range.
     * @param end
     *          The end of the range.
     */
    public Range(final int begin, final int end) {
      this.begin = begin;
      this.end = end;
    }

    /**
     * Default constructor.
     */
    public Range() {
    }
  }

  /**
   * Return the range from a begining token to an ending token.
   * 
   * @param beginToken
   *          String to indicate begining of range.
   * @param endToken
   *          String to indicate ending of range.
   * @param string
   *          String to look for range in.
   * @param fromIndex
   *          Beginning index.
   * @return (begin index, end index) or <i>null</i>.
   */
  public static Range rangeOf(final String beginToken, final String endToken, final String string, final int fromIndex) {
    final int begin = string.indexOf(beginToken, fromIndex);

    if (begin != -1) {
      final int end = string.indexOf(endToken, begin + 1);
      if (end != -1) {
        return new Range(begin, end);
      }
    }

    return null;
  }

  /**
   * Return the range from a begining token to an ending token.
   * 
   * @param beginToken
   *          String to indicate begining of range.
   * @param endToken
   *          String to indicate ending of range.
   * @param string
   *          String to look for range in.
   * @return (begin index, end index) or <i>null</i>.
   */
  public static Range rangeOf(final String beginToken, final String endToken, final String string) {
    return rangeOf(beginToken, endToken, string, 0);
  }

  public static String[] split(final String string, final String delim, final int limit) {
    // get the count of delim in string, if count is > limit
    // then use limit for count. The number of delimiters is less by one
    // than the number of elements, so add one to count.
    int count = count(string, delim) + 1;
    if (limit > 0 && count > limit) {
      count = limit;
    }

    final String strings[] = new String[count];
    int begin = 0;

    for (int i = 0; i < count; i++) {
      // get the next index of delim
      int end = string.indexOf(delim, begin);

      // if the end index is -1 or if this is the last element
      // then use the string's length for the end index
      if (end == -1 || i + 1 == count) {
        end = string.length();
      }

      // if end is 0, then the first element is empty
      if (end == 0) {
        strings[i] = EMPTY;
      }
      else {
        strings[i] = string.substring(begin, end);
      }

      // update the begining index
      begin = end + 1;
    }

    return strings;
  }

  public static String[] split(final String string, final String delim) {
    return split(string, delim, -1);
  }

  public static List<String> splitToList(final String string, final String delim, final int limit) {
    // get the count of delim in string, if count is > limit
    // then use limit for count. The number of delimiters is less by one
    // than the number of elements, so add one to count.
    int count = count(string, delim) + 1;
    if (limit > 0 && count > limit) {
      count = limit;
    }

    final List<String> strings = new ArrayList<String>(count);
    int begin = 0;

    for (int i = 0; i < count; i++) {
      // get the next index of delim
      int end = string.indexOf(delim, begin);

      // if the end index is -1 or if this is the last element
      // then use the string's length for the end index
      if (end == -1 || i + 1 == count) {
        end = string.length();
      }

      // if end is 0, then the first element is empty
      if (end == 0) {
        strings.add(EMPTY);
      }
      else {
        strings.add(string.substring(begin, end));
      }
      // update the begining index
      begin = end + 1;
    }
    return strings;
  }

  public static List<String> splitToList(final String string, final String delim) {
    return splitToList(string, delim, -1);
  }

  // ///////////////////////////////////////////////////////////////////////
  // Joining/Concatenation Methods //
  // ///////////////////////////////////////////////////////////////////////

  /**
   * Join an array of strings into one delimited string.
   * 
   * @param buff
   *          String buffered used for join (buffer is not reset).
   * @param array
   *          Array of objects to join as strings.
   * @param delim
   *          Delimiter to join strings with or <i>null</i>.
   * @return Joined string.
   */
  public static String join(final StringBuffer buff, final Object array[], final String delim) {
    final boolean haveDelim = delim != null;

    for (int i = 0; i < array.length; i++) {
      if (array[i] != null) {
        buff.append(array[i]);
      }

      // if this is the last element then don't append delim
      if (haveDelim && i + 1 < array.length) {
        buff.append(delim);
      }
    }

    return buff.toString();
  }

  /**
   * Join an array of strings into one delimited string.
   * 
   * @param array
   *          Array of objects to join as strings.
   * @param delim
   *          Delimiter to join strings with or <i>null</i>.
   * @return Joined string.
   */
  public static String join(final Object array[], final String delim) {
    return join(new StringBuffer(), array, delim);
  }

  /**
   * Convert and join an array of objects into one string.
   * 
   * @param array
   *          Array of objects to join as strings.
   * @return Converted and joined objects.
   */
  public static String join(final Object array[]) {
    return join(array, null);
  }

  /**
   * Convert and join an array of bytes into one string.
   * 
   * @param array
   *          Array of objects to join as strings.
   * @return Converted and joined objects.
   */
  public static String join(final byte array[]) {
    final Byte bytes[] = new Byte[array.length];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = new Byte(array[i]);
    }

    return join(bytes, null);
  }

  /**
   * Return a string composed of the given array.
   * 
   * @param buff
   *          Buffer used to construct string value (not reset).
   * @param array
   *          Array of objects.
   * @param prefix
   *          String prefix.
   * @param separator
   *          Element sepearator.
   * @param suffix
   *          String suffix.
   * @return String in the format of: prefix + n ( + separator + n+i)* + suffix.
   */
  public static String join(final StringBuffer buff, final Object[] array, final String prefix, final String separator,
      final String suffix) {
    buff.append(prefix);
    join(buff, array, separator);
    buff.append(suffix);

    return buff.toString();
  }

  /**
   * Return a string composed of the given array.
   * 
   * @param array
   *          Array of objects.
   * @param prefix
   *          String prefix.
   * @param separator
   *          Element sepearator.
   * @param suffix
   *          String suffix.
   * @return String in the format of: prefix + n ( + separator + n+i)* + suffix.
   */
  public static String join(final Object[] array, final String prefix, final String separator, final String suffix) {
    return join(new StringBuffer(), array, prefix, separator, suffix);
  }

  // ///////////////////////////////////////////////////////////////////////
  // Counting Methods //
  // ///////////////////////////////////////////////////////////////////////

  /**
   * Count the number of instances of substring within a string.
   * 
   * @param string
   *          String to look for substring in.
   * @param substring
   *          Sub-string to look for.
   * @return Count of substrings in string.
   */
  public static int count(final String string, final String substring) {
    int count = 0;
    int idx = 0;

    while ((idx = string.indexOf(substring, idx)) != -1) {
      idx++;
      count++;
    }

    return count;
  }

  /**
   * Count the number of instances of character within a string.
   * 
   * @param string
   *          String to look for substring in.
   * @param c
   *          Character to look for.
   * @return Count of substrings in string.
   */
  public static int count(final String string, final char c) {
    return count(string, String.valueOf(c));
  }

  // ///////////////////////////////////////////////////////////////////////
  // Padding Methods //
  // ///////////////////////////////////////////////////////////////////////

  /**
   * Return a string padded with the given string for the given count.
   * 
   * @param buff
   *          String buffer used for padding (buffer is not reset).
   * @param string
   *          Pad element.
   * @param count
   *          Pad count.
   * @return Padded string.
   */
  public static String pad(final StringBuffer buff, final String string, final int count) {
    for (int i = 0; i < count; i++) {
      buff.append(string);
    }

    return buff.toString();
  }

  /**
   * Return a string padded with the given string for the given count.
   * 
   * @param string
   *          Pad element.
   * @param count
   *          Pad count.
   * @return Padded string.
   */
  public static String pad(final String string, final int count) {
    return pad(new StringBuffer(), string, count);
  }

  /**
   * Return a string padded with the given string value of an object for the
   * given count.
   * 
   * @param obj
   *          Object to convert to a string.
   * @param count
   *          Pad count.
   * @return Padded string.
   */
  public static String pad(final Object obj, final int count) {
    return pad(new StringBuffer(), String.valueOf(obj), count);
  }

  // ///////////////////////////////////////////////////////////////////////
  // Misc Methods //
  // ///////////////////////////////////////////////////////////////////////

  /**
   * <p>
   * Compare two strings.
   * <p>
   * Both or one of them may be null.
   * 
   * @return true if object equals or intern ==, else false.
   */
  public static boolean compare(final String me, final String you) {
    // If both null or intern equals
    if (me == you) {
      return true;
    }

    // if me null and you are not
    if (me == null && you != null) {
      return false;
    }

    // me will not be null, test for equality
    return me.equals(you);
  }

  /**
   * Check if the given string is empty.
   * 
   * @param string
   *          String to check
   * @return True if string is empty
   */
  public static boolean isEmpty(final String string) {
    if (string == null || string.trim().length() == 0 || string.equals(EMPTY)) {
      return true;
    }
    return false;
  }

  /**
   * Return the <i>nth</i> index of the given token occurring in the given
   * string.
   * 
   * @param string
   *          String to search.
   * @param token
   *          Token to match.
   * @param index
   *          <i>Nth</i> index.
   * @return Index of <i>nth</i> item or -1.
   */
  public static int nthIndexOf(final String string, final String token, final int index) {
    int j = 0;

    for (int i = 0; i < index; i++) {
      j = string.indexOf(token, j + 1);
      if (j == -1) {
        break;
      }
    }
    return j;
  }

  /**
   * Parses a time period into a long. Translates possible [msec|sec|min|h]
   * suffixes For example: "1" -> 1 (msec) "1msec -> 1 (msec) "1sec" -> 1000
   * (msecs) "1min" -> 60000 (msecs) "1h" -> 3600000 (msecs) Accepts negative
   * periods, e.g. "-1"
   * 
   * @param period
   *          the stringfied time period
   * @return the parsed time period as long
   * @throws NumberFormatException
   */
  public static long parseTimePeriod(final String period) {
    try {
      String s = period.toLowerCase();
      long factor;

      // look for suffix
      if (s.endsWith("msec")) {
        s = s.substring(0, s.lastIndexOf("msec"));
        factor = MSEC;
      }
      else if (s.endsWith("sec")) {
        s = s.substring(0, s.lastIndexOf("sec"));
        factor = SECS;
      }
      else if (s.endsWith("min")) {
        s = s.substring(0, s.lastIndexOf("min"));
        factor = MINS;
      }
      else if (s.endsWith("h")) {
        s = s.substring(0, s.lastIndexOf("h"));
        factor = HOUR;
      }
      else {
        factor = 1;
      }
      return Long.parseLong(s) * factor;
    }
    catch (final RuntimeException e) {
      // thrown in addition when period is 'null'
      throw new NumberFormatException("For input time period: '" + period + "'");
    }
  }

  /**
   * Same like parseTimePeriod(), but guards for negative entries.
   * 
   * @param period
   *          the stringfied time period
   * @return the parsed time period as long
   * @throws NumberFormatException
   */
  public static long parsePositiveTimePeriod(final String period) {
    final long retval = parseTimePeriod(period);
    if (retval < 0) {
      throw new NumberFormatException("Negative input time period: '" + period + "'");
    }
    return retval;
  }

  public static String[] trim(final String[] strings) {
    for (int i = 0; i < strings.length; i++) {
      strings[i] = strings[i].trim();
    }

    return strings;
  }

  public static String trim(final String str) {
    if (isEmpty(str)) {
      return str;
    }
    final StringBuffer buf = new StringBuffer(str);
    while (buf.length() > 0 && Character.isWhitespace(buf.charAt(0))) {
      buf.deleteCharAt(0);
    }
    while (buf.length() > 0 && Character.isWhitespace(buf.charAt(buf.length() - 1))) {
      buf.deleteCharAt(buf.length() - 1);
    }
    return buf.toString();
  }

  public static String trimLeading(final String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) != ' ' && s.charAt(i) != '\t') {
        return s.substring(i);
      }
    }
    return "";
  }

  public static String trimLeading(final String s, final String prefix) {
    if (s.startsWith(prefix)) {
      return s.substring(prefix.length());
    }
    return s;
  }

  public static String trimTrailing(final String str) {
    if (isEmpty(str)) {
      return str;
    }
    final StringBuffer buf = new StringBuffer(str);
    while (buf.length() > 0 && Character.isWhitespace(buf.charAt(buf.length() - 1))) {
      buf.deleteCharAt(buf.length() - 1);
    }
    return buf.toString();
  }

  public static String trimLeadingCharacter(final String str, final char leadingCharacter) {
    if (isEmpty(str)) {
      return str;
    }
    final StringBuffer buf = new StringBuffer(str);
    while (buf.length() > 0 && buf.charAt(0) == leadingCharacter) {
      buf.deleteCharAt(0);
    }
    return buf.toString();
  }

  public static String trimTrailingCharacter(final String str, final char trailingCharacter) {
    if (isEmpty(str)) {
      return str;
    }
    final StringBuffer buf = new StringBuffer(str);
    while (buf.length() > 0 && buf.charAt(buf.length() - 1) == trailingCharacter) {
      buf.deleteCharAt(buf.length() - 1);
    }
    return buf.toString();
  }

  public static String[] trimArrayElements(final String[] array) {
    if (Checker.isEmpty(array)) {
      return new String[0];
    }
    final String[] result = new String[array.length];
    for (int i = 0; i < array.length; ++i) {
      final String element = array[i];
      result[i] = element != null ? element.trim() : null;
    }
    return result;
  }

  public static String removeWhiteSpace(final String s) {
    String retn = null;

    if (s != null) {
      final int len = s.length();
      final StringBuffer sbuf = new StringBuffer(len);

      for (int i = 0; i < len; i++) {
        final char c = s.charAt(i);

        if (!Character.isWhitespace(c)) {
          sbuf.append(c);
        }
      }
      retn = sbuf.toString();
    }
    return retn;
  }

  public static StringBuffer stringSubstitution(final String argStr, final Map vars, final boolean isLenient) {
    final StringBuffer argBuf = new StringBuffer();
    if (argStr == null || argStr.length() == 0) {
      return argBuf;
    }
    if (vars == null || vars.size() == 0) {
      return argBuf.append(argStr);
    }
    final int argStrLength = argStr.length();
    for (int cIdx = 0; cIdx < argStrLength;) {
      char ch = argStr.charAt(cIdx);
      char del = ' ';
      switch (ch) {
        case '$':
          final StringBuffer nameBuf = new StringBuffer();
          del = argStr.charAt(cIdx + 1);
          if (del == '{') {
            cIdx++;
            for (++cIdx; cIdx < argStr.length(); ++cIdx) {
              ch = argStr.charAt(cIdx);
              if (ch == '_' || ch == '.' || ch == '-' || ch == '+' || Character.isLetterOrDigit(ch)) {
                nameBuf.append(ch);
              }
              else {
                break;
              }
            }
            if (nameBuf.length() > 0) {
              final Object temp = vars.get(nameBuf.toString());
              final String value = temp != null ? temp.toString() : null;
              if (value != null) {
                argBuf.append(value);
              }
              else {
                if (isLenient) {
                  // just append the unresolved variable declaration
                  argBuf.append("${").append(nameBuf.toString()).append("}");
                }
                else {
                  // complain that no variable was found
                  throw new RuntimeException("No value found for : " + nameBuf);
                }
              }
              del = argStr.charAt(cIdx);
              if (del != '}') {
                throw new RuntimeException("Delimiter not found for : " + nameBuf);
              }
            }
            cIdx++;
          }
          else {
            argBuf.append(ch);
            ++cIdx;
          }
          break;
        default:
          argBuf.append(ch);
          ++cIdx;
          break;
      }
    }
    return argBuf;
  }

  public static String fixFileSeparatorChar(final String arg) {
    return arg.replace(SLASH_CHAR, File.separatorChar).replace(BACKSLASH_CHAR, File.separatorChar);
  }

  public static String processQuotedString(final String quoted) {
    final StringBuffer buf = new StringBuffer();
    final int len = quoted.length();
    for (int index = 1; index <= len - 2; index++) {
      final char c = quoted.charAt(index);
      if (c != '\\') {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  public static String quote(final String str) {
    return str != null ? "'" + str + "'" : null;
  }

  public static String unqualify(final String qualifiedName) {
    return unqualify(qualifiedName, '.');
  }

  public static String unqualify(final String qualifiedName, final char separator) {
    return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
  }

  public static boolean startsWithIgnoreCase(final String str, final String prefix) {
    if (str == null || prefix == null) {
      return false;
    }
    if (str.startsWith(prefix)) {
      return true;
    }
    if (str.length() < prefix.length()) {
      return false;
    }
    final String lcStr = str.substring(0, prefix.length()).toLowerCase();
    final String lcPrefix = prefix.toLowerCase();
    return lcStr.equals(lcPrefix);
  }

  public static boolean endsWithIgnoreCase(final String str, final String suffix) {
    if (str == null || suffix == null) {
      return false;
    }
    if (str.endsWith(suffix)) {
      return true;
    }
    if (str.length() < suffix.length()) {
      return false;
    }

    final String lcStr = str.substring(str.length() - suffix.length()).toLowerCase();
    final String lcSuffix = suffix.toLowerCase();
    return lcStr.equals(lcSuffix);
  }

  public static boolean containsWithIgnoreCase(final String base, final String string) {
    return base.toLowerCase().contains(string.toLowerCase());
  }

  public static boolean containsWhitespace(final String str) {
    if (isEmpty(str)) {
      return false;
    }
    final int strLen = str.length();
    for (int i = 0; i < strLen; ++i) {
      if (Character.isWhitespace(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public static String replace(final String inString, final String oldPattern, final String newPattern) {
    if (isEmpty(inString) || isEmpty(oldPattern) || newPattern == null) {
      return inString;
    }
    final StringBuffer sbuf = new StringBuffer();

    int pos = 0;
    int index = inString.indexOf(oldPattern);

    final int patLen = oldPattern.length();
    while (index >= 0) {
      sbuf.append(inString.substring(pos, index));
      sbuf.append(newPattern);
      pos = index + patLen;
      index = inString.indexOf(oldPattern, pos);
    }
    sbuf.append(inString.substring(pos));
    return sbuf.toString();
  }

  public static String delete(final String inString, final String pattern) {
    return replace(inString, pattern, "");
  }

  public static String replaceProperties(final String string) {
    return replaceProperties(string, (Properties) null);
  }

  public static String replaceProperties(final String string, final Properties props) {
    if (string == null) {
      return string;
    }
    final char[] chars = string.toCharArray();
    final StringBuffer buffer = new StringBuffer();
    boolean properties = false;
    int state = NORMAL;
    int start = 0;
    for (int i = 0; i < chars.length; ++i) {
      final char c = chars[i];

      // Dollar sign outside brackets
      if (c == '$' && state != IN_BRACKET) {
        state = SEEN_DOLLAR;
      }
      else if (c == '{' && state == SEEN_DOLLAR) {
        buffer.append(string.substring(start, i - 1));
        state = IN_BRACKET;
        start = i - 1;
      }

      // No open bracket after dollar
      else if (state == SEEN_DOLLAR) {
        state = NORMAL;
      }
      else if (c == '}' && state == IN_BRACKET) {
        // No content
        if (start + 2 == i) {
          buffer.append("${}"); // REVIEW: Correct?
        }
        else { // Collect the system property
          String value = null;

          final String key = string.substring(start + 2, i);

          // check for alias
          if (FILE_SEPARATOR_ALIAS.equals(key)) {
            value = FILE_SEPARATOR;
          }
          else if (PATH_SEPARATOR_ALIAS.equals(key)) {
            value = PATH_SEPARATOR;
          }
          else {
            // check from the properties
            if (props != null) {
              value = props.getProperty(key);
            }
            else {
              value = System.getProperty(key);
            }

            if (value == null) {
              // Check for a default value ${key:default}
              final int colon = key.indexOf(':');
              if (colon > 0) {
                final String realKey = key.substring(0, colon);
                if (props != null) {
                  value = props.getProperty(realKey);
                }
                else {
                  value = System.getProperty(realKey);
                }

                if (value == null) {
                  // Check for a composite key, "key1,key2"
                  value = resolveCompositeKey(realKey, props);

                  // Not a composite key either, use the specified default
                  if (value == null) {
                    value = key.substring(colon + 1);
                  }
                }
              }
              else {
                // No default, check for a composite key, "key1,key2"
                value = resolveCompositeKey(key, props);
              }
            }
          }

          if (value != null) {
            properties = true;
            buffer.append(value);
          }
        }
        start = i + 1;
        state = NORMAL;
      }
    }

    // No properties
    if (properties == false) {
      return string;
    }

    // Collect the trailing characters
    if (start != chars.length) {
      buffer.append(string.substring(start, chars.length));
    }

    // Done
    return buffer.toString();
  }

  public static String replaceProperties(final String string, final Map<String, Object> props) {
    if (string == null) {
      return string;
    }
    final char[] chars = string.toCharArray();
    final StringBuffer buffer = new StringBuffer();
    boolean properties = false;
    int state = NORMAL;
    int start = 0;
    for (int i = 0; i < chars.length; ++i) {
      final char c = chars[i];

      // Dollar sign outside brackets
      if (c == '$' && state != IN_BRACKET) {
        state = SEEN_DOLLAR;
      }
      else if (c == '{' && state == SEEN_DOLLAR) {
        buffer.append(string.substring(start, i - 1));
        state = IN_BRACKET;
        start = i - 1;
      }

      // No open bracket after dollar
      else if (state == SEEN_DOLLAR) {
        state = NORMAL;
      }
      else if (c == '}' && state == IN_BRACKET) {
        // No content
        if (start + 2 == i) {
          buffer.append("${}"); // REVIEW: Correct?
        }
        else { // Collect the system property
          String value = null;

          final String key = string.substring(start + 2, i);

          // check for alias
          if (FILE_SEPARATOR_ALIAS.equals(key)) {
            value = FILE_SEPARATOR;
          }
          else if (PATH_SEPARATOR_ALIAS.equals(key)) {
            value = PATH_SEPARATOR;
          }
          else {
            // check from the properties
            if (props != null) {
              Object o = props.get(key);
              if (o instanceof Callable) {
                try {
                  o = ((Callable) o).call();
                }
                catch (final Exception e) {
                  ;
                }
              }
              if (o != null) {
                value = String.valueOf(o);
              }
            }
            else {
              value = System.getProperty(key);
            }
            if (value == null) {
              // Check for a default value ${key:default}
              final int colon = key.indexOf(':');
              if (colon > 0) {
                final String realKey = key.substring(0, colon);
                if (props != null) {
                  Object o = props.get(realKey);
                  if (o instanceof Callable) {
                    try {
                      o = ((Callable) o).call();
                    }
                    catch (final Exception e) {
                      ;
                    }
                  }
                  if (o != null) {
                    value = String.valueOf(o);
                  }
                }
                else {
                  value = System.getProperty(realKey);
                }
                if (value == null) {
                  value = resolveCompositeKey(realKey, props);
                  if (value == null) {
                    value = key.substring(colon + 1);
                  }
                }
              }
              else {
                // No default, check for a composite key, "key1,key2"
                value = resolveCompositeKey(key, props);
              }
            }
          }

          if (value != null) {
            properties = true;
            buffer.append(value);
          }
        }
        start = i + 1;
        state = NORMAL;
      }
    }

    // No properties
    if (properties == false) {
      return string;
    }

    // Collect the trailing characters
    if (start != chars.length) {
      buffer.append(string.substring(start, chars.length));
    }

    // Done
    return buffer.toString();
  }

  private static String resolveCompositeKey(final String key, final Properties props) {
    String value = null;

    // Look for the comma
    final int comma = key.indexOf(',');
    if (comma > -1) {
      // If we have a first part, try resolve it
      if (comma > 0) {
        // Check the first part
        final String key1 = key.substring(0, comma);
        if (props != null) {
          value = props.getProperty(key1);
        }
        else {
          value = System.getProperty(key1);
        }
      }
      // Check the second part, if there is one and first lookup failed
      if (value == null && comma < key.length() - 1) {
        final String key2 = key.substring(comma + 1);
        if (props != null) {
          value = props.getProperty(key2);
        }
        else {
          value = System.getProperty(key2);
        }
      }
    }
    // Return whatever we've found or null
    return value;
  }

  private static String resolveCompositeKey(final String key, final Map<String, Object> props) {
    String value = null;

    // Look for the comma
    final int comma = key.indexOf(',');
    if (comma > -1) {
      // If we have a first part, try resolve it
      if (comma > 0) {
        // Check the first part
        final String key1 = key.substring(0, comma);
        if (props != null) {
          Object o = props.get(key1);
          if (o instanceof Callable) {
            try {
              o = ((Callable) o).call();
            }
            catch (final Exception e) {
              ;
            }
          }
          if (o != null) {
            value = String.valueOf(o);
          }
        }
        else {
          value = System.getProperty(key1);
        }
      }
      // Check the second part, if there is one and first lookup failed
      if (value == null && comma < key.length() - 1) {
        final String key2 = key.substring(comma + 1);
        if (props != null) {
          Object o = props.get(key2);
          if (o instanceof Callable) {
            try {
              o = ((Callable) o).call();
            }
            catch (final Exception e) {
              ;
            }
          }
          if (o != null) {
            value = String.valueOf(o);
          }
        }
        else {
          value = System.getProperty(key2);
        }
      }
    }
    // Return whatever we've found or null
    return value;
  }

  public static boolean toBoolean(final String s) {
    return "true".equalsIgnoreCase(s) || "1".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s)
        || "enable".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
  }

  public static URL toURL(String urlspec, final String relativePrefix) throws MalformedURLException {
    urlspec = urlspec.trim();

    URL url;

    try {
      url = new URL(urlspec);
      if (url.getProtocol().equals("file")) {
        url = makeURLFromFilespec(url.getFile(), relativePrefix);
      }
    }
    catch (final Exception e) {
      // make sure we have a absolute & canonical file url
      try {
        url = makeURLFromFilespec(urlspec, relativePrefix);
      }
      catch (final IOException n) {
        //
        // jason: or should we rethrow e?
        //
        throw new MalformedURLException(n.toString());
      }
    }

    return url;
  }

  public static URI toURI(String urispec, final String relativePrefix) throws URISyntaxException {
    urispec = urispec.trim();

    URI uri;

    if (urispec.startsWith("file:")) {
      uri = makeURIFromFilespec(urispec.substring(5), relativePrefix);
    }
    else {
      uri = new URI(urispec);
    }

    return uri;
  }

  public static URL toURL(final String urlspec) throws MalformedURLException {
    return toURL(urlspec, null);
  }

  /**
   * @param urispec
   * @return
   * @throws MalformedURLException
   */
  public static URI toURI(final String urispec) throws URISyntaxException {
    return toURI(urispec, null);
  }

  @SuppressWarnings("deprecation")
  private static URL makeURLFromFilespec(final String filespec, final String relativePrefix) throws IOException {
    // make sure the file is absolute & canonical file url
    File file = new File(filespec);

    // if we have a prefix and the file is not abs then prepend
    if (relativePrefix != null && !file.isAbsolute()) {
      file = new File(relativePrefix, filespec);
    }

    // make sure it is canonical (no ../ and such)
    file = file.getCanonicalFile();

    return file.toURL();
  }

  private static URI makeURIFromFilespec(final String filespec, final String relativePrefix) {
    // make sure the file is absolute & canonical file url
    File file = new File(filespec);

    // if we have a prefix and the file is not abs then prepend
    if (relativePrefix != null && !file.isAbsolute()) {
      file = new File(relativePrefix, filespec);
    }

    return file.toURI();
  }

  public static Properties toProperties(final String[] array, final String delimiter) {
    if (Checker.isEmpty(array)) {
      return new Properties();
    }
    final Properties result = new Properties();
    for (int i = 0; i < array.length; ++i) {
      final String element = array[i];
      final String[] splittedElement = split(element, delimiter);
      if (splittedElement == null) {
        continue;
      }
      result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
    }
    return result;
  }

  public static String format(final String message, final Object... args) {
    try {
      for (int i = 0; i < args.length; i++) {
        final Object o = args[i];
        if (o != null && o.getClass().isArray()) {
          args[i] = Arrays.asList((Object[]) o);
        }
      }
      return String.format(message, args);
    }
    catch (final Exception e) {
      final String error = buildExceptionMessage(message, e, args);
      return error;
    }
  }

  private static String buildExceptionMessage(final String message, final Exception e, final Object... args) {
    String error = "Could not format message with format string: {" + message + "}, args: {";
    final boolean useComma = false;
    for (final Object arg : args) {
      if (useComma) {
        error += ", ";
      }
      error += "{" + (arg != null ? arg : "null") + "}";
    }
    return error;
  }

  public static byte[] toBytes(final String s) {
    try {
      return s.getBytes(LanguageUtils.DEFAULT_ENCODING);
    }
    catch (final UnsupportedEncodingException e) {
      return s.getBytes();
    }
  }

  public static String toString(final Object input) {
    return toString(input, null);
  }

  public static String toString(final Object input, final String separator) {
    final StringBuffer buffer = new StringBuffer();
    if (input instanceof Collection) {
      final Collection col = (Collection) input;
      for (final Iterator it = col.iterator(); it.hasNext();) {
        final Object o = it.next();
        buffer.append("{").append(toString(o, separator)).append("}").append(separator == null ? "," : separator);
      }
      if (buffer.length() > 0) {
        buffer.deleteCharAt(buffer.length() - 1);
      }
    }
    else if (input instanceof Map) {
      final Map map = (Map) input;
      for (final Iterator it = map.keySet().iterator(); it.hasNext();) {
        final Object key = it.next();
        final Object value = map.get(key);
        buffer.append(toString(key, separator)).append("=").append("{").append(toString(value, separator)).append("}")
            .append(separator == null ? "," : separator);
      }
      if (buffer.length() > 0) {
        buffer.deleteCharAt(buffer.length() - 1);
      }
    }
    else if (input != null && input.getClass().isArray()) {
      final Object[] array = (Object[]) input;
      buffer.append(toString(Arrays.asList(array), separator));
    }
    else if (input instanceof InetSocketAddress) {
      buffer.append(NetworkUtils.hostportizeAddress((InetAddress) input));
    }
    else if (input instanceof LazyInetSocketAddress) {
      buffer.append(((LazyInetSocketAddress) input).hostportize());
    }
    else if (input instanceof InetAddress) {
      buffer.append(NetworkUtils.translateAddress((InetAddress) input));
    }
    else {
      buffer.append(String.valueOf(input));
    }
    return buffer.toString();
  }

  public static String[] toStringArray(final Collection<?> collection) {
    if (collection == null) {
      return null;
    }
    final String[] retval = new String[collection.size()];
    int i = 0;
    for (final Object o : collection) {
      retval[i] = String.valueOf(o);
      i = i + 1;
    }
    return retval;
  }
}
