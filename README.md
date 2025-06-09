
# Sprouts — A State Management Property API [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) ![Java Version](https://img.shields.io/static/v1.svg?label=Java&message=8%2B&color=blue) #

This library was specifically designed as a tool for realizing
common application architecture patterns like MVVM, MVI, etc. 
It provides Null-safe, functional and event-based properties and
serves as the foundational library for the [SwingTree](https://github.com/globaltcad/swing-tree)
GUI framework.


- [Documentation](https://globaltcad.github.io/sprouts/)
- [Motivation](docs/markdown/Motivation.md)

---
## Getting started with Apache Maven ##

```
<dependency>
  <groupId>io.github.globaltcad</groupId>
  <artifactId>sprouts</artifactId>
  <version>2.0.0-M19</version>
</dependency>
```

---

## Getting started with Gradle ##
Groovy DSL:
```
implementation 'io.github.globaltcad:sprouts:2.0.0-M19'
```
Kotlin DSL:
```
implementation("io.github.globaltcad:sprouts:2.0.0-M19")
```
---

## Getting started with [![](https://jitpack.io/v/globaltcad/sprouts.svg)](https://jitpack.io/#globaltcad/sprouts) ##
**1. Add the JitPack url in your root `build.gradle` at the end of `repositories`**
```
allprojects {
	repositories {
		//...
		maven { url 'https://jitpack.io' }
	}
}
```
**2. Add sprouts as dependency**

...either by specifying the version tag:
```
dependencies {
	implementation 'com.github.globaltcad:sprouts:2.0.0-M19'
}
```
...or by using a custom commit hash instead:
```
dependencies {
	implementation 'com.github.globaltcad:sprouts:b3bf1b6'//Any commit hash...
}
```
---

