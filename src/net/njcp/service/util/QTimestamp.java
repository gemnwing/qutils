package net.njcp.service.util;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QTimestamp implements Serializable, Cloneable, Comparable<QTimestamp> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int YEAR = Calendar.YEAR;
	public static final int MONTH = Calendar.MONTH;
	public static final int DAY = Calendar.DATE;
	public static final int HOUR = Calendar.HOUR_OF_DAY;
	public static final int MINUTE = Calendar.MINUTE;
	public static final int SECOND = Calendar.SECOND;
	public static final int MILLISECOND = Calendar.MILLISECOND;

	public static final String VALID_TIME_PATTERN = "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{1,2}:[0-9]{1,2}:[0-9]{1,2}(\\.[0-9]{1,9}){0,1}";
	public static final String VALID_DIGITS_PATTERN = "(19[7-9][0-9]|20[0-9][0-9])(0[1-9]|1[0-2])([0-2][0-9]|3[0-1])([0-2][0-9]([0-5][0-9]([0-5][0-9]([0-9]{0,3})?)?)?)?";

	private Calendar cal = null;
	private Integer interval = 0;
	private int nanos = 0;

	public QTimestamp() {
		this.cal = Calendar.getInstance();
	}

	public QTimestamp(long time) {
		this();
		this.cal.setTimeInMillis(time);
	}

	public QTimestamp(int year, int month, int date, int hour, int minute, int second, int millis) {
		this();
		setYear(year);
		setMonth(month);
		setDate(date);
		setHours(hour);
		setMinutes(minute);
		setSeconds(second);
		setMillis(millis);
	}

	public QTimestamp(int year, int month, int date, int hour, int minute, int second) {
		this(year, month, date, hour, minute, second, 0);
	}

	private QTimestamp(Integer... digits) {
		this();
		Integer[] d = new Integer[] { 1970, 1, 1, 0, 0, 0, 0 };
		for ( int i = 0; i < d.length; i++ ) {
			int num = d[i];
			if ( i < digits.length && digits[i] != null ) {
				num = digits[i];
			}
			switch ( i ) {
			case 0:
				setYear(num);
				break;
			case 1:
				setMonth(num);
				break;
			case 2:
				setDate(num);
				break;
			case 3:
				setHours(num);
				break;
			case 4:
				setMinutes(num);
				break;
			case 5:
				setSeconds(num);
				break;
			case 6:
				setMillis(num);
				break;
			}
		}
	}

	public static Date trunc(Date date, int intervalInMillis, Integer... offset) {
		return QTimestamp.valueOf(date).trunc(intervalInMillis, offset).toDate();
	}

	public static Timestamp trunc(Timestamp time, int intervalInMillis, Integer... offset) {
		return QTimestamp.valueOf(time).trunc(intervalInMillis, offset).toTimestamp();
	}

	public static QTimestamp trunc(QTimestamp qt, int intervalInMillis, Integer... offset) {
		return qt.clone().trunc(intervalInMillis, offset);
	}

	private QTimestamp trunc(long intervalInMillis, Integer... offset) {
		if ( offset.length > 0 ) {
			return trunc(intervalInMillis).offset(offset[0]);
		} else {
			return trunc(intervalInMillis);
		}
	}

	public QTimestamp trunc(long intervalInMillis) {
		Long interval = intervalInMillis;
		switch ( interval.intValue() ) {
		case YEAR:
			trunc(86400000);
			set(MONTH, 0);
			set(DAY, 1);
			break;
		case MONTH:
			trunc(86400000);
			set(DAY, 1);
			break;
		case DAY:
			trunc(86400000);
			break;
		case HOUR:
			trunc(3600000);
			break;
		case MINUTE:
			trunc(60000);
			break;
		default:
			long tzOffset = get(Calendar.ZONE_OFFSET);
			long timeInMillis = getTime();
			long remainder = (timeInMillis + tzOffset) % intervalInMillis;
			timeInMillis = timeInMillis - remainder;
			this.cal.setTimeInMillis(timeInMillis);
			break;
		}
		this.interval = interval.intValue();
		return this;
	}

	@Override
	public QTimestamp clone() {
		QTimestamp qt = new QTimestamp();
		qt.setTime(getTime());
		qt.setNanos(getNanos());
		return qt;
	}

	/**
	 * Only works after trunc()
	 * @param offset
	 * @return
	 */
	public QTimestamp offset(int offset) {
		QTimestamp qt = clone();
		switch ( this.interval.intValue() ) {
		case YEAR:
		case MONTH:
		case DAY:
		case HOUR:
		case MINUTE:
			qt.add(this.interval.intValue(), offset);
			break;
		default:
			qt.addMillis(this.interval * offset);
			break;
		}
		return qt;
	}

	public Timestamp toTimestamp() {
		Timestamp newTime = new Timestamp(getTime());
		newTime.setNanos(this.nanos);
		return newTime;
	}

	public Date toDate() {
		return new Date(getTime());
	}

	public int toTimeT() {
		return ((Double) Math.ceil(getTime() / 1000.0)).intValue();
	}

	public void set2Now() {
		this.cal.setTimeInMillis(System.currentTimeMillis());
	}

	public void setTime(long timeInMillis) {
		this.cal.setTimeInMillis(timeInMillis);
	}

	public long getTime() {
		return this.cal.getTimeInMillis();
	}

	public long getUTCTime() {
		return getTime() + getTimezoneOffsetInMillis();
	}

	public Calendar toCalendar() {
		return (Calendar) this.cal.clone();
	}

	public static void set(Date date, int field, int value) {
		QTimestamp qt = QTimestamp.valueOf(date);
		qt.set(field, value);
		date.setTime(qt.getTime());
	}

	public static void set(Timestamp time, int field, int value) {
		QTimestamp qt = QTimestamp.valueOf(time);
		qt.set(field, value);
		time.setTime(qt.getTime());
		time.setNanos(qt.getNanos());
	}

	public void set(int field, int value) {
		this.cal.set(field, value);
	}

	private int get(int field) {
		return this.cal.get(field);
	}

	public static int get(Date date, int field) {
		return QTimestamp.valueOf(date).get(field);
	}

	public static int get(long timeInMillis, int field) {
		return new QTimestamp(timeInMillis).get(field);
	}

	public int getYear() {
		return get(Calendar.YEAR);
	}

	public void setYear(int year) {
		set(Calendar.YEAR, year);
	}

	/**
	 * Month of year, starts from 1.
	 */
	public int getMonth() {
		return get(Calendar.MONTH) + 1;
	}

	/**
	 * Month of year, starts from 1.
	 */
	public void setMonth(int month) {
		set(Calendar.MONTH, month - 1);
	}

	public int getDay() {
		return getDate();
	}

	public void setDay(int day) {
		setDate(day);
	}

	public int getDate() {
		return get(Calendar.DATE);
	}

	public void setDate(int day) {
		set(Calendar.DATE, day);
	}

	public int getHours() {
		return get(Calendar.HOUR_OF_DAY);
	}

	public void setHours(int hour) {
		set(Calendar.HOUR_OF_DAY, hour);
	}

	public int getMinutes() {
		return get(Calendar.MINUTE);
	}

	public void setMinutes(int minute) {
		set(Calendar.MINUTE, minute);
	}

	public int getSeconds() {
		return get(Calendar.SECOND);
	}

	public void setSeconds(int second) {
		set(Calendar.SECOND, second);
	}

	public int getMillis() {
		return get(Calendar.MILLISECOND);
	}

	public void setMillis(int millis) {
		set(Calendar.MILLISECOND, millis);
	}

	public static Date add(Date date, int timeUnit, int offset) {
		return QTimestamp.valueOf(date).add(timeUnit, offset).toDate();
	}

	public static Timestamp add(Timestamp time, int timeUnit, int offset) {
		return QTimestamp.valueOf(time).add(timeUnit, offset).toTimestamp();
	}

	public static QTimestamp add(QTimestamp qt, int timeUnit, int offset) {
		return qt.clone().add(timeUnit, offset);
	}

	public QTimestamp add(int timeUnit, int offset) {
		this.cal.add(timeUnit, offset);
		return this;
	}

	public QTimestamp addYear(int offset) {
		return add(YEAR, offset);
	}

	public QTimestamp addMonth(int offset) {
		return add(MONTH, offset);
	}

	public QTimestamp addDay(int offset) {
		return add(DAY, offset);
	}

	public QTimestamp addHour(int offset) {
		return add(HOUR, offset);
	}

	public QTimestamp addMinute(int offset) {
		return add(MINUTE, offset);
	}

	public QTimestamp addSecond(int offset) {
		return add(SECOND, offset);
	}

	public QTimestamp addMillis(int offset) {
		return add(MILLISECOND, offset);
	}

	public static QTimestamp valueOf(Object o) {
		if ( o instanceof Timestamp ) {
			return QTimestamp.valueOf((Timestamp) o);
		} else if ( o instanceof Date ) {
			return QTimestamp.valueOf((Date) o);
		} else if ( o instanceof Calendar ) {
			return QTimestamp.valueOf((Calendar) o);
		} else if ( o instanceof Number ) {
			return QTimestamp.valueOf(((Number) o).longValue());
		}
		return QTimestamp.valueOf(String.valueOf(o));
	}

	public static QTimestamp valueOf(Date date) {
		return QTimestamp.valueOf(date.getTime());
	}

	public static QTimestamp valueOf(Timestamp time) {
		QTimestamp qt = new QTimestamp();
		qt.setTime(time.getTime());
		qt.setNanos(time.getNanos());
		return qt;
	}

	public static QTimestamp valueOf(Calendar cal) {
		QTimestamp qt = new QTimestamp();
		qt.setTime(cal.getTimeInMillis());
		return qt;
	}

	public static QTimestamp valueOf(long timeInMillis) {
		return new QTimestamp(timeInMillis);
	}

	public static QTimestamp valueOf(String s) {
		if ( s == null ) {
			throw new NullPointerException();
		}
		s = s.trim();
		if ( !s.isEmpty() && !s.matches(VALID_TIME_PATTERN) ) {
			Integer[] d = new Integer[] { 1970, 1, 1, 0, 0, 0, 0 };
			if ( s.matches("^[0-9]+$") ) {
				String origin = "19700101000000000";
				char[] a = origin.toCharArray();
				s.getChars(0, Math.min(origin.length(), s.length()), a, 0);
				s = String.valueOf(a);
				d[0] = Integer.valueOf(s.substring(0, 4));
				d[1] = Integer.valueOf(s.substring(4, 6));
				d[2] = Integer.valueOf(s.substring(6, 8));
				d[3] = Integer.valueOf(s.substring(8, 10));
				d[4] = Integer.valueOf(s.substring(10, 12));
				d[5] = Integer.valueOf(s.substring(12, 14));
				d[6] = Integer.valueOf(s.substring(14));
			} else {
				Matcher matcher = Pattern.compile(VALID_DIGITS_PATTERN).matcher(s.replaceAll("[^0-9]", ""));
				if ( matcher.find() ) {
					return valueOf(matcher.group());
				}
				String[] a = s.split("[^0-9]");
				int j = 0;
				if ( a.length > 0 ) {
					for ( int i = 0; i < Math.min(d.length, a.length); i++ ) {
						if ( a[i].matches("^[0-9]+$") ) {
							if ( i == 6 ) {	// nanos
								d[j++] = (int) (Double.parseDouble("0." + a[i].toString()) * 1000000000);
							} else {
								d[j++] = Integer.valueOf(a[i]);
							}
						}
					}
				}
			}
			return new QTimestamp(d);
		}
		return QTimestamp.valueOf(Timestamp.valueOf(s));
	}

	public String format(String pattern) {
		return new SimpleDateFormat(pattern).format(toDate());
	}

	public String toSimpleString() {
		return format("yyyy-MM-dd HH:mm:ss.SSS").replaceAll("( 00:00:00)*\\.000$", "");
	}

	public int getTimezoneOffset() {
		return getTimezoneOffsetInMillis() / (60 * 1000);
	}

	public int getTimezoneOffsetInMillis() {
		return this.cal.get(Calendar.ZONE_OFFSET) + this.cal.get(Calendar.DST_OFFSET);
	}

	public int compareTo(Calendar c) {
		return this.cal.compareTo(c);
	}

	public int compareTo(Timestamp t) {
		return toTimestamp().compareTo(t);
	}

	public int compareTo(Date d) {
		return toDate().compareTo(d);
	}

	@Override
	public int compareTo(QTimestamp qt) {
		return this.cal.compareTo(qt.cal);
	}

	@Override
	public boolean equals(Object o) {
		if ( o instanceof Calendar ) {
			return this.cal.equals(o);
		}
		if ( o instanceof Timestamp ) {
			return toTimestamp().equals(o);
		}
		if ( o instanceof Date ) {
			return toDate().equals(o);
		}
		if ( this == o ) {
			return true;
		}
		if ( o instanceof QTimestamp ) {
			QTimestamp qt = (QTimestamp) o;
			if ( this.nanos != qt.nanos ) {
				return false;
			}
			if ( this.cal != null ) {
				if ( qt.cal == null ) {
					return false;
				} else if ( !this.cal.equals(qt.cal) ) {
					return false;
				}
			} else if ( qt.cal != null ) {
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.cal == null) ? 0 : this.cal.hashCode());
		result = prime * result + this.nanos;
		return result;
	}

	@Override
	public String toString() {
		return String.format("%04d-%02d-%02d %02d:%02d:%02d%s", getYear(), getMonth(), getDate(), getHours(), getMinutes(), getSeconds(),
				(getMillis() == 0 ? "" : "." + String.format("%03d", getMillis()).replaceAll("0+$", "")));
	}

	public int getNanos() {
		return this.nanos;
	}

	public void setNanos(int nanos) {
		this.nanos = nanos;
	}

	public static void main(String[] args) {
		QLog.println(QTimestamp.valueOf("33401-OBJ_110-20180205155812160133.JSON+ADA.DT"));
	}

}
