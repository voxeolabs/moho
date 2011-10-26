package com.voxeo.moho.common.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LanguageUtils {

  public final static String DEFAULT_ENCODING = "UTF-8";

  private static final Map<String, String> CHARSET_MAP;

  static {
    CHARSET_MAP = new HashMap<String, String>(0);
    CHARSET_MAP.put("sq", "ISO-8859-2");
    CHARSET_MAP.put("ar", "ISO-8859-6");
    CHARSET_MAP.put("bg", "ISO-8859-5");
    CHARSET_MAP.put("be", "ISO-8859-5");
    CHARSET_MAP.put("ca", "ISO-8859-1");
    CHARSET_MAP.put("zh", "GB2312");
    CHARSET_MAP.put("zh_TW", "Big5");
    CHARSET_MAP.put("hr", "ISO-8859-2");
    CHARSET_MAP.put("cs", "ISO-8859-2");
    CHARSET_MAP.put("da", "ISO-8859-1");
    CHARSET_MAP.put("nl", "ISO-8859-1");
    CHARSET_MAP.put("en", "ISO-8859-1");
    CHARSET_MAP.put("et", "ISO-8859-1");
    CHARSET_MAP.put("fi", "ISO-8859-1");
    CHARSET_MAP.put("fr", "ISO-8859-1");
    CHARSET_MAP.put("de", "ISO-8859-1");
    CHARSET_MAP.put("el", "ISO-8859-7");
    CHARSET_MAP.put("he", "ISO-8859-8");
    CHARSET_MAP.put("hu", "ISO-8859-2");
    CHARSET_MAP.put("is", "ISO-8859-1");
    CHARSET_MAP.put("it", "ISO-8859-1");
    CHARSET_MAP.put("ja", "Shift_JIS");
    CHARSET_MAP.put("ko", "EUC-KR");
    CHARSET_MAP.put("lv", "ISO-8859-2");
    CHARSET_MAP.put("lt", "ISO-8859-2");
    CHARSET_MAP.put("mk", "ISO-8859-5");
    CHARSET_MAP.put("no", "ISO-8859-1");
    CHARSET_MAP.put("pl", "ISO-8859-2");
    CHARSET_MAP.put("pt", "ISO-8859-1");
    CHARSET_MAP.put("ro", "ISO-8859-2");
    CHARSET_MAP.put("ru", "ISO-8859-5");
    CHARSET_MAP.put("sr", "ISO-8859-5");
    CHARSET_MAP.put("sh", "ISO-8859-5");
    CHARSET_MAP.put("sk", "ISO-8859-2");
    CHARSET_MAP.put("sl", "ISO-8859-2");
    CHARSET_MAP.put("es", "ISO-8859-1");
    CHARSET_MAP.put("sv", "ISO-8859-1");
    CHARSET_MAP.put("tr", "ISO-8859-9");
    CHARSET_MAP.put("uk", "ISO-8859-5");
  }

  public static String localeToCharset(final String locale) {
    return localeToCharset(parseLocale(locale));
  }

  public static String localeToCharset(final Locale vlocale) {
    if (vlocale == null) {
      return null;
    }
    String charset = CHARSET_MAP.get(vlocale.toString());
    if (charset == null) {
      charset = CHARSET_MAP.get(vlocale.getLanguage());
    }
    if (charset == null) {
      charset = DEFAULT_ENCODING;
    }
    return charset;
  }

  public static Locale parseLocale(final String value) {
    final String val = value.trim();
    final int dash1 = val.indexOf('-');
    final int dash2 = dash1 <= 0 ? -1 : val.indexOf('-', dash1 + 1);
    final int dash3 = dash2 <= 0 ? -1 : val.indexOf('-', dash2 + 1);
    if (dash3 > 0) {
      return new Locale(val.substring(0, dash1), val.substring(dash1 + 1, dash2), val.substring(dash2 + 1, dash3));
    }
    if (dash2 > 0) {
      return new Locale(val.substring(0, dash1), val.substring(dash1 + 1, dash2), val.substring(dash2 + 1));
    }
    if (dash1 > 0) {
      return new Locale(val.substring(0, dash1), val.substring(dash1 + 1));
    }
    else {
      return new Locale(val, "");
    }
  }
}
