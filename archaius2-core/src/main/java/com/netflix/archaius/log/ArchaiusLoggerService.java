package com.netflix.archaius.log;

public interface ArchaiusLoggerService {

	public ArchaiusLogger getLogger(
			Class<?> clazz);
}
