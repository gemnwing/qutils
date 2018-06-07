package net.njcp.service.util;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QStringUtil {
	private static final String[] DIGIT_TRANS = new String[] { "零", "一", "二", "三", "四", "五", "六", "七", "八", "九" };
	private static final String[] DIGIT_SCALE = new String[] { "", "十", "百", "千" };
	private static final int PATTERN_OPTION = Pattern.DOTALL | Pattern.MULTILINE;

	public static final int WITH_LEADING_SPACE = 1;
	public static final int DISPLAY_ZERO_ANYWAY = 2;
	public static final int WITH_END_SPACE = 3;

	private static ConcurrentHashMap<String, Pattern> patterns = new ConcurrentHashMap<String, Pattern>();

	public static boolean match(Object stringObject, String paramString) {
		if ( stringObject != null ) {
			Pattern pattern = patterns.get(paramString);
			if ( pattern == null ) {
				pattern = Pattern.compile(paramString, PATTERN_OPTION);
				patterns.put(paramString, pattern);
			}
			return pattern.matcher(stringObject.toString()).matches();
		}
		return false;
	}

	public static String passwd(Object o) {
		if ( o == null ) {
			return "";
		}
		return times("*", o.toString().length());
	}

	public static String times(String s, int n) {
		StringBuilder retSb = new StringBuilder();
		if ( n >= 1 ) {
			for ( int i = 1; i <= n; i++ ) {
				retSb.append(s);
			}
		}
		return retSb.toString();
	}

	public static int charCount(Object str) {
		if ( str == null ) {
			return 0;
		}
		return str.toString().getBytes(Charset.defaultCharset()).length;
	}

	public static int charCount(Object str, Charset charset) {
		if ( str == null ) {
			return 0;
		}
		return str.toString().getBytes(charset).length;
	}

	public static String cutString(Object str, int size) {
		return cutString(str, size, Charset.defaultCharset(), false);
	}

	public static String cutStringWithEllipsis(Object str, int size) {
		return cutString(str, size, Charset.defaultCharset(), true);
	}

	public static String cutString(Object str, int size, Charset charset) {
		return cutString(str, size, charset, false);
	}

	public static String cutString(Object str, int size, Charset charset, boolean withEllipsis) {
		if ( str == null || charCount(str, charset) <= Math.abs(size) ) {
			return String.valueOf(str);
		}
		String suffix = "";
		if ( withEllipsis ) {
			suffix = "...";
			size -= 3;
		}
		StringBuffer sb = new StringBuffer(str.toString());
		boolean fromRight = false;
		if ( size < 0 ) {
			fromRight = true;
			size = Math.abs(size);
			sb = sb.reverse();
		}
		int count = 0;
		int idx = 0;
		for ( ; idx < sb.length(); idx++ ) {
			String s = sb.substring(idx, idx + 1);
			count += s.getBytes(charset).length;
			if ( count > size ) {
				break;
			}
		}
		if ( fromRight ) {
			return sb.delete(idx, sb.length()).reverse().toString() + suffix;
		} else {
			return sb.substring(0, idx) + suffix;
		}
	}

	public static String transNumberToChn(Object number) {
		if ( number instanceof Number ) {
			if ( number instanceof Integer || number instanceof Long || number instanceof BigInteger ) {
				return transNumberToChn(number.toString(), "");
			} else {
				return transNumberToChn(new DecimalFormat("#.#########").format(number).replaceAll("\\.$", ""), "");
			}
		} else {
			return transNumberToChn(number.toString().replaceAll("[^\\-\\.0-9]", ""), "");
		}
	}

	private static String transNumberToChn(String number, String base) {
		if ( !number.matches("^[-]?[0-9]+(\\.[0-9]+)?$") ) {
			return number;
		}
		if ( number.equals("0") ) {
			return "零";
		}
		StringBuffer intSb = new StringBuffer();
		StringBuffer decSb = new StringBuffer();
		String retStr = "";
		boolean negative = false;
		if ( number.contains(".") ) {
			intSb = new StringBuffer(number.split("\\.")[0]);
			decSb = new StringBuffer(number.split("\\.")[1]);
		} else {
			intSb = new StringBuffer(number);
		}
		if ( number.startsWith("-") ) {
			negative = true;
			intSb.deleteCharAt(0);
		}
		intSb = intSb.reverse();
		for ( int pos = 0; pos < intSb.length(); pos++ ) {
			if ( pos == 4 ) {
				retStr = transNumberToChn(intSb.reverse().substring(0, intSb.length() - pos), base.equals("万") ? "亿" : "万") + retStr;
				break;
			}
			int digit = Integer.valueOf(intSb.substring(pos, pos + 1));
			retStr = DIGIT_TRANS[digit] + (digit == 0 ? "" : DIGIT_SCALE[pos]) + retStr;
		}
		if ( base.isEmpty() ) {
			retStr = retStr.replaceAll("^一十", "十");
		}
		retStr = retStr.replaceAll("零+", "零");
		for ( int pos = 0; pos < decSb.length(); pos++ ) {
			int digit = Integer.valueOf(decSb.substring(pos, pos + 1));
			retStr += ((pos == 0) ? "点" : "") + DIGIT_TRANS[digit];
		}
		retStr = retStr.replaceAll("零$", "");
		return (negative ? "负" : "") + retStr + base;
	}

	public static String sentenceCase(Object paramStr) {
		StringBuilder sb = new StringBuilder(String.valueOf(paramStr));
		if ( sb.length() != 0 ) {
			sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		}
		return sb.toString();
	}

	public static String titleCase(Object paramStr) {
		return titleCase(paramStr, true);
	}

	public static String titleCase(Object paramStr, boolean nextTitleCase) {
		String input = String.valueOf(paramStr);
		if ( input.toString().trim().isEmpty() ) {
			return input;
		}
		StringBuilder titleCase = new StringBuilder();

		for ( char c : input.toCharArray() ) {
			// if ( Character.isSpaceChar(c) || c == '_' ) {
			if ( !Character.isLetterOrDigit(c) ) {
				nextTitleCase = true;
			} else if ( nextTitleCase ) {
				c = Character.toTitleCase(c);
				nextTitleCase = false;
			} else {
				c = Character.toLowerCase(c);
			}
			titleCase.append(c);
		}
		return titleCase.toString();
	}

	public static String namingConvetionalize(String input) {
		return titleCase(input, false).replaceAll("[^0-9A-Za-z]", "");
	}

	public static String NamingConvetionalize(String input) {
		return titleCase(input, true).replaceAll("[^0-9A-Za-z]", "");
	}

	public static String countableDesc(Integer num, String str, Integer... options) {
		boolean showZero = false;
		boolean leadingSpace = false;
		boolean followingSpace = false;
		if ( options.length != 0 ) {
			for ( Integer opt : options ) {
				if ( opt != null ) {
					switch ( opt ) {
					case WITH_LEADING_SPACE:
						leadingSpace = true;
						break;
					case WITH_END_SPACE:
						followingSpace = true;
						break;
					case DISPLAY_ZERO_ANYWAY:
						showZero = true;
						break;
					default:
						break;
					}
				}
			}
		}
		StringBuilder retSb = new StringBuilder();
		if ( num == null || (showZero && num < 0) || (!showZero && num <= 0) ) {
			return retSb.toString();
		}
		if ( leadingSpace ) {
			retSb.append(" ");
		}
		retSb.append(num).append(" ").append(str);
		if ( num > 1 ) {
			switch ( retSb.charAt(retSb.length() - 1) ) {
			case 's':
				retSb.append("es");
			case 'y':
				retSb.deleteCharAt(retSb.length() - 1);
				retSb.append("ies");
				break;
			default:
				retSb.append("s");
				break;
			}
		}
		if ( followingSpace ) {
			retSb.append(" ");
		}
		return retSb.toString();
	}

	public static String repeat(Object str, int times) {
		if ( str == null ) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < times; i++ ) {
			sb.append(str);
		}
		return sb.toString();
	}

	public static List<String> asStringList(Object... args) {
		if ( args == null ) {
			return null;
		}
		List<String> retList = new ArrayList<String>();
		for ( Object arg : args ) {
			if ( arg == null ) {
				retList.add(null);
			} else {
				if ( arg instanceof Collection<?> ) {
					retList.addAll(asStringList(((Collection<?>) arg).toArray()));
				} else if ( arg instanceof boolean[] ) {
					for ( Object o : ((boolean[]) arg) ) {
						retList.add(o.toString());
					}
				} else if ( arg instanceof char[] ) {
					for ( Object o : ((char[]) arg) ) {
						retList.add(o.toString());
					}
				} else if ( arg instanceof byte[] ) {
					for ( Object o : ((byte[]) arg) ) {
						retList.add(o.toString());
					}
				} else if ( arg instanceof short[] ) {
					for ( Object o : ((short[]) arg) ) {
						retList.add(o.toString());
					}
				} else if ( arg instanceof int[] ) {
					for ( Object o : ((int[]) arg) ) {
						retList.add(o.toString());
					}
				} else if ( arg instanceof long[] ) {
					for ( Object o : ((long[]) arg) ) {
						retList.add(o.toString());
					}
				} else if ( arg instanceof float[] ) {
					for ( Object o : ((float[]) arg) ) {
						retList.add(o.toString());
					}
				} else if ( arg instanceof double[] ) {
					for ( Object o : ((double[]) arg) ) {
						retList.add(o.toString());
					}
				} else if ( arg instanceof Object[] ) {
					retList.addAll(asStringList((Object[]) arg));
				} else {
					retList.add(arg.toString());
				}
			}
		}
		return retList;
	}

	/**
	 * @param paramStr
	 * @param regex is a regex pattern
	 * @return
	 */
	public static String extract(Object paramStr, String regex) {
		if ( paramStr == null ) {
			return null;
		}
		String retStr = paramStr.toString();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(retStr);
		if ( matcher.find() ) {
			retStr = matcher.group();
		} else {
			retStr = "";
		}
		return retStr;
	}

	public static void main(String[] args) {
		System.out.println(extract("5 more tries and I'll do it, I REALLY mean it this time.","REA.*LLY"));
	}
}
