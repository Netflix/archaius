package com.netflix.archaius.log;

public interface ArchaiusLogger {
	
	   public abstract boolean isTraceEnabled();

	   public abstract void trace(String message);

	   public abstract void trace(String message, Object... params);

	   public abstract void trace(String message, Throwable error);

	   public abstract boolean isDebugEnabled();

	   public abstract void debug(String message);

	   public abstract void debug(String message, Object... params);

	   public abstract void debug(String message, Throwable error);

	   public abstract void info(String message);

	   public abstract void info(String message, Object... params);

	   public abstract void info(String message, Throwable error);

	   public abstract boolean isWarnEnabled();

	   public abstract void warn(String message);

	   public abstract void warn(String message, Object... params);

	   public abstract void warn(String message, Throwable error);

	   public abstract void error(String message);

	   public abstract void error(String message, Object... params);

	   public abstract void error(String message, Throwable error);

}
