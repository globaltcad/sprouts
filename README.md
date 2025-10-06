
# Sprouts :seedling: — [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) ![Java Version](https://img.shields.io/static/v1.svg?label=Java&message=8%2B&color=blue) #

> Immutable Collections and Reactive State Management through Properties

This library is a tool for simplifying state management, and it was specifically designed
to simplify the implementation of common application architecture patterns like MVVM, MVI, MVL, etc. 
It provides Null-safe, functional and event-based properties and
serves as the foundational library for the [SwingTree](https://github.com/globaltcad/swing-tree)
GUI framework.<br>
A core design goal of this library is to facilitate the implementation of
data oriented domain models (value objects) using immutable and
persistent collections and then connect these models to classical place oriented 
systems using the lens pattern on reactive properties.

- [Documentation](https://globaltcad.github.io/sprouts/)
- [Motivation](docs/markdown/Motivation.md)

---
## Getting started with Apache Maven ##

```
<dependency>
  <groupId>io.github.globaltcad</groupId>
  <artifactId>sprouts</artifactId>
  <version>2.0.0</version>
</dependency>
```

---

## Getting started with Gradle ##
Groovy DSL:
```
implementation 'io.github.globaltcad:sprouts:2.0.0'
```
Kotlin DSL:
```
implementation("io.github.globaltcad:sprouts:2.0.0")
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
	implementation 'com.github.globaltcad:sprouts:2.0.0'
}
```
...or by using a custom commit hash instead:
```
dependencies {
	implementation 'com.github.globaltcad:sprouts:01d076ea997656fb8f466533736f45f46a53072b'//Any commit hash...
}
```
---

