package com.netflix.archaius.log;

public class NoOpLoggerFactory implements ArchaiusLoggerService {

	@Override
	public ArchaiusLogger getLogger(
			Class<?> clazz) {
		return logger;
	}
	
	private static ArchaiusLogger logger = new NoOpLogger();
	
	private static class NoOpLogger implements ArchaiusLogger {

		@Override
		public boolean isTraceEnabled() {
			return false;
		}

		@Override
		public void trace(
				String message) {
		}

		@Override
		public void trace(
				String message,
				Object... params) {
		}

		@Override
		public void trace(
				String message,
				Throwable error) {
		}

		@Override
		public boolean isDebugEnabled() {
			return false;
		}

		@Override
		public void debug(
				String message) {
		}

		@Override
		public void debug(
				String message,
				Object... params) {
		}

		@Override
		public void debug(
				String message,
				Throwable error) {
		}

		@Override
		public void info(
				String message) {
		}

		@Override
		public void info(
				String message,
				Object... params) {
		}

		@Override
		public void info(
				String message,
				Throwable error) {
		}

		@Override
		public boolean isWarnEnabled() {
			return false;
		}

		@Override
		public void warn(
				String message) {
		}

		@Override
		public void warn(
				String message,
				Object... params) {
		}

		@Override
		public void warn(
				String message,
				Throwable error) {
		}

		@Override
		public void error(
				String message) {
		}

		@Override
		public void error(
				String message,
				Object... params) {
		}

		@Override
		public void error(
				String message,
				Throwable error) {
		}
		
	}

}
