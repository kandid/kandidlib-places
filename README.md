kandidlib-places
=================

A small Java library to access standard places for configuration and data files.

The problem
---------
Each operating system has its own conventions where to place configuration files, look for data files or create cache data. Unfortunately Java has no built in mechanism to retrieve these locations. As a consequence many programs ignore these rules and pollute the users home directory with lots of unwanted files.

The solution
---------
This little library helps in locating the standard directories in an OS specific manner.

* On Unix systems it follows the [XDG Base Directory Specification](http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html)
* On Windows it follows the rules of [Common folder variables](http://www.microsoft.com/security/portal/mmpc/shared/variables.aspx)
* Mac OS X it follows the rules of [Important Java Directories on Mac OS X](http://developer.apple.com/library/mac/#qa/qa2001/qa1170.html)

For unknown systems the library chooses the XDG convention and spits out a warning on the Logger. The OS will be determined on behalf of `System.getProperty("os.name")` but since there is no exhaustive list, I can't foresee all possible returned strings. So expect some inaccuracies and drop me a note on github when you found one.

How to use it
----------
Add this library to your classpath and ask de.kandid.environment.Places for the wanted directories.

Building the kandidlib-places.jar
---------------------------
This library uses the [Gradle](http://gradle.org)-1.10 or later build system. Since I refuse to add the wrapper to the source code, you need to have it installed. Then
```sh
gradle jar
```
produces the jar.

Improving kandidlib-places
-------------
Of course any IDE may be used to work on kandidlib-places but support for gradle makes it more convenient.
