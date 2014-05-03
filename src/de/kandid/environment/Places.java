/*
 *  Copyright (C) 2014  Dominikus Diesch
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package de.kandid.environment;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
 */
public abstract class Places {

	private static class XDG extends Places {

		public XDG() {
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

		private static File[] possibilities(String type, String defPath) {
			ArrayList<File> ret = new ArrayList<>();
			String home = System.getenv("XDG_" + type + "_HOME");
			if (home != null && home.length() > 0)
				ret.add(new File(home));
			String dirs = System.getenv("XDG_" + type + "_DIRS");
			dirs = dirs == null || dirs.length() == 0 ? defPath : dirs;
			if (dirs != null && dirs.length() > 0) {
				for (String dir : dirs.split(File.pathSeparator)) {
					File file = new File(dir);
					if (!ret.contains(file))
						ret.add(file);
				}
			}
			if (ret.size() == 0)
				ret.add(new File(System.getProperty("user.home"), "." + type.toLowerCase()));
			return ret.toArray(new File[ret.size()]);
		}

		private final File[] _configBases = possibilities("CONFIG", "/etc/xdg");
		private final File[] _dataBases = possibilities("DATA", "/usr/local/share/:/usr/share/");
		private final File _cacheBase = new File(getenv("XDG_CACHE_HOME", ".cache"));
		private final File _runtimeBase = new File(getenv("XDG_RUNTIME_DIR", System.getProperty("java.io.tmpdir")));
	}

	private static class Windows extends Places {

		private static class Vista extends Windows {
			Vista() {
				super (new File[] {
						new File(getenv("APPDATA", "AppData\\Roaming")),
						new File(getenv("LOCALAPPDATA", "AppData")),
						new File(getenv("PROGRAMDATA", "C:\\Program Files"))
				});
			}
		}

		private static class XP extends Windows {
			XP() {
				super (new File[] {
						new File(getenv("APPDATA", "AppData")),
						new File(getenv("PROGRAMFILES", "C:\\Program Files"))
				});
			}
		}

		Windows(File[] dirs) {
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
			return new File(System.getenv("TEMP"));
		}

		@Override
		public File getRuntimeBase() {
			return getCacheBase();
		}

		private final File[] _dirs;
	}

	private static class MacOS extends Places {

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

		private final File[] _configBases = new File[]{new File(System.getProperty("user.home") + "/Library/Preferences")};
		private final File[] _dataBases = new File[]{new File(System.getProperty("user.home") + "/Library")};
		private final File _cacheBase = new File(System.getProperty("user.home") + "/Library/Caches");
	}

	/**
	 * Retreive all directories where config files can be found. They are sorted descending
	 * by relevance. At least the first one can also be used to write config files.
	 * If not, it is an err
	 * @return
	 */
	public abstract File[] getConfigBases();

	public abstract File[] getDataBases();

	public abstract File getCacheBase();

	public abstract File getRuntimeBase();

	public File[] getConfigRead(String applicationName) {
		return appendToBases(getConfigBases(), applicationName);
	}

	public File getConfigWrite(String applicationName) {
		return createUserDir(getConfigBases()[0], applicationName);
	}

	public File[] getDataRead(String applicationName) {
		return  appendToBases(getDataBases(), applicationName);
	}

	public File getDataWrite(String applicationName) {
		return createUserDir(getDataBases()[0], applicationName);
	}

	public File getCacheDir(String applicationName) {
		return createUserDir(getCacheBase(), applicationName);
	}

	public File getRuntimeDir(String applicationName) {
		return createUserDir(getRuntimeBase(), applicationName);
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
			String osName = System.getProperty("os.name");
			if (osName.startsWith("Linux") || osName.startsWith("FreeBSD") || osName.startsWith("SunOS"))
				_instance = new XDG();
			else if ("Windows XP".equals(osName) || "Windows 2000".equals(osName) || "Windows NT".equals(osName))
				_instance = new Windows.XP();
			else if (osName.startsWith("Windows"))
				_instance = new Windows.Vista();
			else if ("Mac OS X".equals(osName))
				_instance = new MacOS();
			else {
				Logger.getGlobal().warning("While initializing de.kandid.environment.Places: Unknown OS: " + osName);
				_instance = new XDG();
			}
		}
		private static final Places _instance;
	}

	private static String getenv(String name, String def) {
		String ret = System.getenv(name);
		if (ret != null && ret.length() > 0)
			return ret;
		return System.getProperty("user.home") + File.separator + def;
	}

	public static Places get() {
		return Holder._instance;
	}
}
