/*
 * (C) Copyright 2014, by Dominikus Diesch.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kandid.environment;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Locate standard directories in an OS specific manner.<p/>
 *
 * <ul><li>
 * On Unix systems it follows the
 * <a href="http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html">
 * XDG Base Directory Specification</a>
 * </li><li>
 * On Windows it follows the rules of
 * <a href="http://www.microsoft.com/security/portal/mmpc/shared/variables.aspx">
 * Common folder variables</a>
 * </li><li>
 * On Mac OS X it follows the rules of
 * <a href="http://developer.apple.com/library/mac/#qa/qa2001/qa1170.html">
 * Important Java Directories on Mac OS X</a>
 * </li></ul>
 * For unknown systems the library chooses the XDG convention and spits out a
 * warning on the Logger. The OS will be determined on behalf of
 * {@code System.getProperty("os.name")} but since there is no exhaustive list, I
 * can't foresee all possible returned strings. So expect some inaccuracies and
 * drop me a note on <a href="https://github.com/kandid/kandidlib-places/issues">github</a>
 * when you found one.
 */

public abstract class Places {

	public static class SystemParameter {
		String getenv(String name) {
			return System.getenv(name);
		}
		String getprop(String name) {
			return System.getProperty(name);
		}

		String getenv(String name, String def) {
			String ret = getenv(name);
			if (ret != null && ret.length() > 0)
				return ret;
			return getprop("user.home") + File.separator + def;
		}
	}

	public static class XDG extends Places {

		public XDG(SystemParameter env) {
			super(env);
		}

		@Override
		public File[] getConfigBases() {
			return _configBases;
		}

		@Override
		public File[] getDataBases() {
			return _dataBases;
		}

		@Override
		public File getCacheBase() {
			return _cacheBase;
		}

		@Override
		public File getRuntimeBase() {
			return _runtimeBase;
		}

		private File[] possibilities(String type, String defPath) {
			ArrayList<File> ret = new ArrayList<>();
			String home = _env.getenv("XDG_" + type + "_HOME", null);
			if (home != null && home.length() > 0)
				ret.add(new File(home));
			String dirs = _env.getenv("XDG_" + type + "_DIRS", null);
			dirs = dirs == null || dirs.length() == 0 ? defPath : dirs;
			if (dirs != null && dirs.length() > 0) {
				for (String dir : dirs.split(File.pathSeparator)) {
					File file = new File(dir);
					if (!ret.contains(file))
						ret.add(file);
				}
			}
			if (ret.size() == 0)
				ret.add(new File(_env.getprop("user.home"), "." + type.toLowerCase()));
			return ret.toArray(new File[ret.size()]);
		}

		private final File[] _configBases = possibilities("CONFIG", "/etc/xdg");
		private final File[] _dataBases = possibilities("DATA", "/usr/local/share/:/usr/share/");
		private final File _cacheBase = new File(_env.getenv("XDG_CACHE_HOME", ".cache"));
		private final File _runtimeBase = new File(_env.getenv("XDG_RUNTIME_DIR", _env.getprop("java.io.tmpdir")));
	}

	public static class Windows extends Places {

		public static class Vista extends Windows {
			Vista(SystemParameter env) {
				super (env, new File[] {
						new File(env.getenv("APPDATA", "AppData\\Roaming")),
						new File(env.getenv("LOCALAPPDATA", "AppData")),
						new File(env.getenv("PROGRAMDATA", "C:\\Program Files"))
				});
			}
		}

		public static class XP extends Windows {
			XP(SystemParameter env) {
				super (env, new File[] {
						new File(env.getenv("APPDATA", "AppData")),
						new File(env.getenv("PROGRAMFILES", "C:\\Program Files"))
				});
			}
		}

		public Windows(SystemParameter env, File[] dirs) {
			super(env);
			_dirs = dirs;
		}

		@Override
		public File[] getConfigBases() {
			return _dirs;
		}

		@Override
		public File[] getDataBases() {
			return _dirs;
		}

		@Override
		public File getCacheBase() {
			return new File(_env.getenv("TEMP"));
		}

		@Override
		public File getRuntimeBase() {
			return getCacheBase();
		}

		private final File[] _dirs;
	}

	public static class MacOS extends Places {

		public MacOS(SystemParameter env) {
			super(env);
		}

		@Override
		public File[] getConfigBases() {
			return _configBases;
		}

		@Override
		public File[] getDataBases() {
			return _dataBases;
		}

		@Override
		public File getCacheBase() {
			return _cacheBase;
		}

		@Override
		public File getRuntimeBase() {
			// TODO Auto-generated method stub
			return null;
		}

		private final File[] _configBases = new File[]{new File(_env.getprop("user.home") + "/Library/Preferences")};
		private final File[] _dataBases = new File[]{new File(_env.getprop("user.home") + "/Library")};
		private final File _cacheBase = new File(_env.getprop("user.home") + "/Library/Caches");
	}

	public Places(SystemParameter env) {
		_env = env;
	}

	/**
	 * Retreive all directories where config files can be found. They are sorted descending
	 * by relevance. At least the first one can also be used to write config files.<p/>
	 * <em>Note</em>: these directories are <em>not</em> application specific. If you want
	 * an application specific directory, use {@link #getConfigRead(String)}<p/>
	 * The returned array must have at least one entry - the users preferred place.
	 * @return an array of all directories where config files can be found
	 */
	public abstract File[] getConfigBases();

	/**
	 * Retreive all directories where data files can be found. They are sorted descending
	 * by relevance. At least the first one can also be used to write data files.<p/>
	 * <em>Note</em>: these directories are <em>not</em> application specific. If you want
	 * an application specific directory, use {@link #getDataRead(String)}<p/>
	 * The returned array must have at least one entry - the users preferred place.
	 * @return an array of all directories where data files can be found
	 */
	public abstract File[] getDataBases();

	/**
	 * Retrieve the directory relative to which user specific non-essential data files should
	 * be stored.<p/>
	 * <em>Note</em>: this directory is <em>not</em> application specific. If you want
	 * an application specific directory, use {@link #getCacheDir(String)}
	 * @return the directory for non-essential data files; not allowed to be {@code null}
	 */
	public abstract File getCacheBase();

	/**
	 * Retrieve the directory relative to which user-specific non-essential runtime files and
	 * other file objects (such as sockets, named pipes, ...) should be stored.<p/>
	 * <em>Note</em>: this directory is <em>not</em> application specific. If you want
	 * an application specific directory, use {@link #getRuntimeDir(String,boolean)}
	 * @return the directory for non-essential data files; may be {@code null}
	 */
	public abstract File getRuntimeBase();

	/**
	 * Get a preference-ordered list of all application specific directories to search for
	 * config files. The list uses {@link #getConfigBases()} and appends the application name.
	 * @param applicationName	the name of the application
	 * @return an application specific list of config directories
	 */
	public File[] getConfigRead(String applicationName) {
		return appendToBases(getConfigBases(), applicationName);
	}

	/**
	 * Get the application specific directory where to write config files.
	 * @param applicationName the name of the application
	 * @return the application specific directory where to write config files
	 */
	public File getConfigWrite(String applicationName) {
		return createUserDir(getConfigBases()[0], applicationName);
	}

	/**
	 * Get a preference-ordered list of all application specific directories to search for
	 * data files. The list uses {@link #getDataBases()} and appends the application name.
	 * @param applicationName	the name of the application
	 * @return an application specific list of data directories
	 */
	public File[] getDataRead(String applicationName) {
		return  appendToBases(getDataBases(), applicationName);
	}

	/**
	 * Get the application specific directory where to write data files.
	 * @param applicationName the name of the application
	 * @return the application specific directory where to write data files
	 */
	public File getDataWrite(String applicationName) {
		return createUserDir(getDataBases()[0], applicationName);
	}

	/**
	 * Get the application specific directory where to read and write non-essential files.
	 * @param applicationName the name of the application
	 * @return the application specific directory where to read and write non-essential files
	 */
	public File getCacheDir(String applicationName) {
		return createUserDir(getCacheBase(), applicationName);
	}

	/**
	 * Get the application specific the directory relative to which user-specific non-essential
	 * runtime files and other file objects (such as sockets, named pipes, ...) should be stored.
	 * This directory is guaranteed to be cleared between system restarts.<p/>
	 * Since not all operating systems support a place fulfilling the last requirement, this
	 * method may return {@code null} when strict is {@code true}. If {@code strict} is
	 * {@code false}, this method returns never returns {@code null} but always a directory
	 * that is as close as possible.
	 * @param applicationName the name of the application
	 * @param strict detrmines whether to be strict at the requirements
	 * @return the application specific directory where to read and write non-essential files
	 */
	public File getRuntimeDir(String applicationName, boolean strict) {
		File base = getRuntimeBase();
		if (base == null) {
			if (strict)
				return null;
			base = getCacheBase();
		}
		return createUserDir(base, applicationName);
	}

	private static File createUserDir(File file, String applicationName) {
		File ret = new File(file, applicationName);
		ret.mkdirs();
		return ret;
	}

	private static File[] appendToBases(File[] bases, String applicationName) {
		File[] ret = new File[bases.length];
		for (int i = 0; i < bases.length; ++i)
			ret[i] = new File(bases[i], applicationName);
		return ret;
	}

	private static class Holder {
		static {
			_instance = forEnvironment(new SystemParameter());
		}
		private static final Places _instance;
	}

	public static Places forEnvironment(SystemParameter env) {
		String osName = env.getprop("os.name");
		if (osName.startsWith("Linux") || osName.startsWith("FreeBSD") || osName.startsWith("SunOS"))
			return new XDG(env);
		else if ("Windows XP".equals(osName) || "Windows 2000".equals(osName) || "Windows NT".equals(osName))
			return new Windows.XP(env);
		else if (osName.startsWith("Windows"))
			return new Windows.Vista(env);
		else if ("Mac OS X".equals(osName))
			return new MacOS(env);
		else {
			Logger.getGlobal().warning("While initializing de.kandid.environment.Places: Unknown OS: " + osName);
			return new XDG(env);
		}
	}

	/**
	 * Returns the singleton
	 * @return the instance of this class
	 */
	public static Places get() {
		return Holder._instance;
	}

	final SystemParameter _env;
}