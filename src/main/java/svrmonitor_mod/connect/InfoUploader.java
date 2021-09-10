
package com.github.kpnmserver.svrmonitor_mod.connect;

import java.util.Properties;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public interface InfoUploader{
	public static final Properties PROPERTIES = System.getProperties();
	public static final Runtime RUNTIME = Runtime.getRuntime();
	public static final int CPU_NUM = RUNTIME.availableProcessors();
	public static final long MAX_MEMORY = RUNTIME.maxMemory();
	public static final String JAVA_VERSION = PROPERTIES.getProperty("java.version");
	public static final String OS_NAME = PROPERTIES.getProperty("os.name");
	public static final String OS_VERSION = PROPERTIES.getProperty("os.version");
	public static final String OS_ARCH = PROPERTIES.getProperty("os.arch");
	public static final OperatingSystemMXBean OS_MXB = (OperatingSystemMXBean)(ManagementFactory.getOperatingSystemMXBean());

	public static long getTotalMemory(){
		return RUNTIME.totalMemory();
	}

	public static long getFreeMemory(){
		return RUNTIME.freeMemory();
	}

	public static long getUsedMemory(){
		return RUNTIME.totalMemory() - RUNTIME.freeMemory();
	}

	public static double getCpuLoad(){
		return OS_MXB.getProcessCpuLoad() * CPU_NUM;
	}

	public static double getCpuTime(){
		return OS_MXB.getProcessCpuTime() / 1000000000f; // return second
	}

	public static int getThreadNum(){
		return Thread.activeCount();
	}

	public static final int SERVER_UNKNOWN = 0;
	public static final int SERVER_STARTING = 1;
	public static final int SERVER_STARTED = 2;
	public static final int SERVER_STOPPING = 3;
	public static final int SERVER_STOPPED = 4;

	/**********/

	public void sendServerStatus(final int code);

	public void tick(final int ticks);

	public void close();

	public void close(final Throwable e);
}
