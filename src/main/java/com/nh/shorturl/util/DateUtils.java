package com.nh.shorturl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


public class DateUtils {
	
    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);

	
    public static final String DATE_FORMAT_DEFAULT = "yyyyMMdd";
	public static final String MONTH_FORMAT_DEFAULT = "yyyyMM";
	public static final String DATE_FORMAT_HYPHEN = "yyyy-MM-dd";
	public static final String DATE_FORMAT_SLUSH = "yyyy/MM/dd";
	public static final String DATE_FORMAT_COMMA = "yyyy.MM.dd";
    public static final String DATE_FORMAT_KR = "yyyy년 MM월 dd일";


    public static final String DATETIME_FORMAT_DEFAULT = "yyyyMMddHHmmss";
	public static final String DATETIME_SERVER_MILLISECOND = "yyyy.MM.dd HH:mm:ss.SSS";
	public static final String DATETIME_SERVER_MILLISECOND_FOR_LAMP = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String DATETIME_SERVER_MILLISECOND_PURE = "yyyyMMddHHmmssSSS";
	
	public static final String DATETIME_SERVER_TIME= "yyyyMMddHH";
	public static final String DATETIME_SERVER_TIME_MINUTE= "yyyyMMddHHmm";
	public static final String DATETIME_SERVER_DAY = "yyyyMMdd";

	public static final String DAY_AM = "AM";
	public static final String DAY_PM = "PM";


	
    public static String convertDate(String date, String inFormat, String outFormat) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(inFormat);
        try {
            LocalDate localDate = LocalDate.parse(date, formatter);
            return localDate.format(DateTimeFormatter.ofPattern(outFormat));
        } catch (Exception e) {
            return "";
        }
    }

    public static String convertDate(LocalDate date) {
		try {
			return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT_DEFAULT));
		} catch (Exception e) {
			return "";
		}
	}
	public static String convertDate(LocalDate date, String format) {
		try {
			return date.format(DateTimeFormatter.ofPattern(format));
		} catch (Exception e) {
			return "";
		}
	}

	public static String convertDateTime(LocalDateTime dateTime) {
		return convertDateTime(dateTime, DATETIME_FORMAT_DEFAULT);
	}

	public static String convertDateTime(LocalDateTime dateTime, String format) {
		try {
			return dateTime.format(DateTimeFormatter.ofPattern(format));
		} catch (Exception e) {
			return "";
		}
	}


	public static String currentMonth() {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(MONTH_FORMAT_DEFAULT);
		try {
			return LocalDateTime.now().format(formatter);
		} catch (Exception e) {
			return "";
		}
	}

	public static String currentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT_DEFAULT);
        try {
            return LocalDateTime.now().format(formatter);
		} catch (Exception e) {
			return "";
		}
    }

	public static String currentDateTime(String format) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		try {
			return LocalDateTime.now().format(formatter);
		} catch (Exception e) {
			return "";
		}
	}

	public static String convertHyphen(String date) {
        return convertDate(date.length() > 8 ? date.substring(0, 8) : date, DATE_FORMAT_DEFAULT, DATE_FORMAT_HYPHEN);
    }
    public static String convertHyphen(String date, String format) {
        return convertDate(date, format, DATE_FORMAT_HYPHEN);
    }

    public static String convertKR(String date) {
        return convertDate(date.length() > 8 ? date.substring(0, 8) : date, DATE_FORMAT_DEFAULT, DATE_FORMAT_KR);
    }

    public static String currentDate(String format) {
        LocalDate localDate = LocalDate.now();
        try {
            return localDate.format(DateTimeFormatter.ofPattern(format));
        } catch (Exception e) {
            return localDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT_HYPHEN));
        }
    }

    public static LocalDate convertDate(String date) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_DEFAULT);
			return LocalDate.parse(date, formatter);
		} catch (Exception e) {
			log.error("Exception, {}", e.getMessage());
		    return null;
		}
	}

	public static LocalDate convertDate(String date, String format) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
			return LocalDate.parse(date, formatter);
		} catch (Exception e) {
			log.error("Exception, {}", e.getMessage());
			return null;
		}
	}

	public static LocalDateTime convertDateTime(String datetime) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT_DEFAULT);
			return LocalDateTime.parse(datetime, formatter);
		} catch (Exception e) {
			log.error("Exception, {}", e.getMessage());
			return null;
		}
	}

    public static String currentDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT_DEFAULT));
    }

    public static String convertTime(LocalTime time) {
		try {
			return time.format(DateTimeFormatter.ofPattern("HHmmss"));
		} catch (Exception e) {
			return null;
		}
	}

    public static String getNowLocalDateTimeString(final String forPattern) {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(forPattern);
		return LocalDateTime.now().format(formatter);
    }
    
    public static LocalDateTime getNowLocalDateTime() {
		return LocalDateTime.now();
    }
    
    public static String getLocalDateTimeDataFormat(LocalDateTime localDateTime, String format) {
        try {
            return localDateTime.format(DateTimeFormatter.ofPattern(format));
        } catch (Exception e) {
        	log.trace(e.getMessage(), e);
        }
        return null;
    }
    
    
    public static boolean isElapsedTime(LocalDateTime time) {
    	log.debug("time changTime={}", DateUtils.getLocalDateTimeDataFormat(time, DateUtils.DATETIME_FORMAT_DEFAULT));
    	log.debug("time now  Time={}", DateUtils.getLocalDateTimeDataFormat(DateUtils.getNowLocalDateTime(), DateUtils.DATETIME_FORMAT_DEFAULT));

    	return time.isBefore(DateUtils.getNowLocalDateTime());
    }
    
    public static boolean isElapsedTime(String time) {
    	log.debug("time={}", time);
    	LocalDateTime nowTime = DateUtils.getNowLocalDateTime();
    	log.debug("nowTime={}", time);
    	return LocalDateTime.parse(time, DateTimeFormatter.ofPattern(DateUtils.DATETIME_FORMAT_DEFAULT)).isBefore(nowTime);
    }
    
    public static boolean isElapsedTime(String time, Long tokenChangMinusMinutes) {
    	LocalDateTime nowTime = DateUtils.getNowLocalDateTime();
    	log.debug("time={} ", LocalDateTime.parse(time, DateTimeFormatter.ofPattern(DateUtils.DATETIME_FORMAT_DEFAULT)));
    	log.debug("nowTime={} ", nowTime);
    	log.debug("tokenChangMinusMinutes={}", LocalDateTime.parse(time, DateTimeFormatter.ofPattern(DateUtils.DATETIME_FORMAT_DEFAULT)).minusMinutes(tokenChangMinusMinutes));
    	return LocalDateTime.parse(time, DateTimeFormatter.ofPattern(DateUtils.DATETIME_FORMAT_DEFAULT)).minusMinutes(tokenChangMinusMinutes).isBefore(nowTime);
    }
    
    public static boolean checkDate(String date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        try {
            LocalDate.parse(date, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
