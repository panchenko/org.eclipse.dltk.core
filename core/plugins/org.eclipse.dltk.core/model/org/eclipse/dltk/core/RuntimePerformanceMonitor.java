package org.eclipse.dltk.core;

import static org.eclipse.core.runtime.Platform.getDebugOption;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.dltk.core.environment.IEnvironment;

public class RuntimePerformanceMonitor {
	public static final String IOREAD = "IO Read";
	public static final String IOWRITE = "IO Write";

	/**
	 * TODO (alex) This field was never used, remove in 6.0
	 */
	@Deprecated
	public static boolean RUNTIME_PERFORMANCE = true;

	private static volatile boolean active = Boolean.valueOf(
			getDebugOption("org.eclipse.dltk.core/performanceMonitor")) //$NON-NLS-1$
			.booleanValue();

	/**
	 * @since 5.1
	 */
	public static boolean isActive() {
		return active;
	}

	/**
	 * @since 5.1
	 */
	public static void setActive(boolean value) {
		active = value;
	}

	public static class DataEntry {
		long count = 0;
		long total = 0;
		long time = 0;

		public long getCount() {
			return count;
		}

		public long getTotal() {
			return total;
		}

		public long getTime() {
			return time;
		}
	}

	private static final Map<String, Map<String, DataEntry>> entries = new HashMap<String, Map<String, DataEntry>>();

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static synchronized void updateData(String language, String kind,
			long time, long value) {
		Map<String, DataEntry> attrs = internalGetEntries(language);
		DataEntry entry = attrs.get(kind);
		if (entry == null) {
			entry = new DataEntry();
			attrs.put(kind, entry);
		}
		entry.count++;
		entry.total += value;
		entry.time += time;
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static synchronized void updateData(String language, String kind,
			long time, long value, IEnvironment env) {
		if (env != null) {
			updateData(language, kind + " " + env.getName(), time, value);
		}
		updateData(language, kind, time, value);
	}

	private static synchronized Map<String, DataEntry> internalGetEntries(
			String language) {
		Map<String, DataEntry> attrs = entries.get(language);
		if (attrs == null) {
			attrs = new HashMap<String, DataEntry>();
			entries.put(language, attrs);
		}
		return attrs;
	}

	public static Map<String, DataEntry> getEntries(String language) {
		Map<String, DataEntry> copy = new HashMap<String, DataEntry>();
		Map<String, DataEntry> map = internalGetEntries(language);
		for (Map.Entry<String, DataEntry> i : map.entrySet()) {
			DataEntry value = i.getValue();
			DataEntry decopy = new DataEntry();
			decopy.count = value.count;
			decopy.total = value.total;
			decopy.time = value.time;
			copy.put(i.getKey(), decopy);
		}
		return copy;
	}

	public static Map<String, Map<String, DataEntry>> getAllEntries() {
		final Set<String> keySet;
		synchronized (RuntimePerformanceMonitor.class) {
			keySet = new HashSet<String>(entries.keySet());
		}
		Map<String, Map<String, DataEntry>> result = new HashMap<String, Map<String, DataEntry>>();
		for (String key : keySet) {
			result.put(key, getEntries(key));
		}
		return result;
	}

	public static class PerformanceNode {
		private long start;
		private long end;

		public long done() {
			end = System.currentTimeMillis();
			return get();
		}

		public long get() {
			return end - start;
		}

		public void renew() {
			start = System.currentTimeMillis();
		}

		public void done(String natureId, String string, long value) {
			RuntimePerformanceMonitor.updateData(natureId, string, done(),
					value);
		}

		public void done(String natureId, String kind, long value,
				IEnvironment environment) {
			RuntimePerformanceMonitor.updateData(natureId, kind, done(), value,
					environment);
		}
	}

	private static final class DummyPerformanceNode extends PerformanceNode {
		@Override
		public long done() {
			// empty
			return 0;
		}

		@Override
		public void renew() {
			// empty
		}

		@Override
		public void done(String natureId, String kind, long value,
				IEnvironment environment) {
			// empty
		}

		@Override
		public void done(String natureId, String string, long value) {
			// empty
		}
	}

	private static final DummyPerformanceNode dummyNode = new DummyPerformanceNode();

	public static PerformanceNode begin() {
		if (!active) {
			return dummyNode;
		}
		PerformanceNode node = new PerformanceNode();
		node.renew();
		return node;
	}

	public static synchronized void clear() {
		entries.clear();
	}
}
