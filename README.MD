# IInjector

IInjector is a Gradle plugin designed to allow for the compile-time injection of interfaces into Java class bytecode.

## Usage

build.gradle:

```
iinject {
    inputJar jar.getArchivePath()
    // alternatively, an archive task may be supplied as input instead (this will always override inputJar):
    // inputTask jar
    config file('iinject.txt') // required
    outputJarName jar.getBaseName() + '-' + jar.getVersion() + '-iinjected.jar' // optional, defaults to this if classifier is not set (overrides classifier)
    
}
```

iinject.txt:

```
com/example/SomeClass com/example/SomeInterface com/example/SomeOtherInterface
```

In this example, the artifact from the `jar` task will be read into the plugin, and `com/example/SomeClass` will be
transformed, if found, to implement interfaces `com/example/SomeInterface` and `com/example/SomeOtherInterface`. Note that
only one interface is required, and that the interfaces need not be present in the JAR or even on the classpath.
