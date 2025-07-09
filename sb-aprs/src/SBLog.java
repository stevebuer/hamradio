public class SBLog {

	static LogLevel level = LogLevel.INFO;

	public enum LogLevel { DEBUG, INFO, WARN };

	public static void log(String msg) {

		log(LogLevel.INFO, msg);
	}

	public static void log(LogLevel l, String msg) {

		if (l == LogLevel.DEBUG && level != LogLevel.DEBUG)
			return;

		System.out.println(msg);
	}
}
