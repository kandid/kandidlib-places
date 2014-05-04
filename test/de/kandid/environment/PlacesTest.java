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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.kandid.environment.Places.SystemParameter;
import de.kandid.junit.TestCase;

public class PlacesTest extends TestCase {

	public class TestParameter extends SystemParameter {
		public TestParameter(String osName, Class<? extends Places> clazz, String...values) throws IOException {
			_map.put("os.name", osName);
			_class = clazz;
			File home = new File(_root, "home");
			_map.put("user.home", home.getPath());
			for (int i = 0; i < values.length; i += 2) {
				_map.put(values[i], values[i + 1].replaceAll("\\$h", home.getPath()).replaceAll("\\$r", _root.getPath()));
			}
			_places = Places.forEnvironment(this);
			_osName = osName;
		}
		@Override
		String getprop(String name) {
			return _map.get(name);
		}
		@Override
		String getenv(String name) {
			return _map.get(name);
		}
		final File _root = createTempDir();
		final HashMap<String, String> _map = new HashMap<>();
		final Class<? extends Places> _class;
		final Places _places;
		final String _osName;
	};

	public PlacesTest(String name) {
		super(name);
	}

	public void testExistence() {
		assertNotNull("No places object constructed", Places.get());
	}

	public void testIdentification() throws IOException {
		for (TestParameter e : makePlaces())
			assertEquals(e._class, e._places.getClass());
	}

	public void testConfigRead() throws IOException {
		for (TestParameter e : makePlaces()) {
			File[] configRead = e._places.getConfigRead(getName());
			assertTrue(configRead.length >= 1);
			for (File f : configRead)
				assertTrue(f.getPath().startsWith(e._root.getPath()));
		}
	}

	public void testConfigWrite() throws IOException {
		for (TestParameter e : makePlaces()) {
			File configWrite = e._places.getConfigWrite(getName());
			assertTrue(configWrite.getPath().startsWith(e._root.getPath()));
			assertTrue(configWrite.isDirectory());
			assertTrue(configWrite.exists());
		}
	}

	public void testDataRead() throws IOException {
		for (TestParameter e : makePlaces()) {
			File[] dataRead = e._places.getDataRead(getName());
			assertTrue(dataRead.length >= 1);
			for (File f : dataRead)
				assertTrue(f.getPath().startsWith(e._root.getPath()));
		}
	}

	public void testDataWrite() throws IOException {
		for (TestParameter e : makePlaces()) {
			File dataWrite = e._places.getDataWrite(getName());
			assertTrue(dataWrite.getPath().startsWith(e._root.getPath()));
			assertTrue(dataWrite.isDirectory());
			assertTrue(dataWrite.exists());
		}
	}

	public void testCacheDir() throws IOException {
		for (TestParameter e : makePlaces()) {
			File chacheDir = e._places.getCacheDir(getName());
			assertTrue(chacheDir.getPath().startsWith(e._root.getPath()));
			assertTrue(chacheDir.isDirectory());
			assertTrue(chacheDir.exists());
		}
	}

	public void testRuntimeDir() throws IOException {
		for (TestParameter e : makePlaces()) {
			File runtimeDir = e._places.getRuntimeDir(getName(), false);
			assertTrue(runtimeDir.getPath().startsWith(e._root.getPath()));
			assertTrue(runtimeDir.isDirectory());
			assertTrue(runtimeDir.exists());
		}
	}

	public List<TestParameter> makePlaces() throws IOException {
		ArrayList<TestParameter> ret = new ArrayList<>();
		ret.add(new TestParameter("Linux", Places.XDG.class,
				"XDG_CONFIG_HOME", "$h/.config",
				"XDG_CONFIG_DIR", "$r/etc",
				"XDG_DATA_HOME", "$h/.local",
				"XDG_DATA_DIR", "$r/usr",
				"XDG_CACHE_HOME", "$h/.cache"
		));
		ret.add(new TestParameter("Linux", Places.XDG.class));
		ret.add(new TestParameter("Windows 7", Places.Windows.Vista.class,
				"APPDATA", "$h/AppData/Roaming",
				"LOCALAPPDATA", "$h/AppData/Roaming",
				"PROGRAMDATA", "$r/PROGRAMDATA",
				"TEMP", "$h/tmp"
		));
		ret.add(new TestParameter("Windows XP", Places.Windows.XP.class,
				"APPDATA", "$h/AppData/Roaming",
				"LOCALAPPDATA", "$h/AppData/Roaming",
				"PROGRAMFILES", "$r/Program Files",
				"TEMP", "$h/tmp"
		));
		ret.add(new TestParameter("Mac OS X", Places.MacOS.class));
		ret.add(new TestParameter("Unknown OS", Places.XDG.class));
		return ret;
	}
}
