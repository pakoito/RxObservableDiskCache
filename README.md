#RxObservableDiskCache

RxObservableDiskCache is a library to save the results of `Single`s or single value `Observable`s request on a local disk cache, so the next time the same request is called you get an immediate result.

##Rationale

RxObservableDiskCache was created with a single purpose: help you store your network results in a disk cache and refetch them as soon as they're re-requested. It also solves displaying values while waiting for network results, when the user is offline, or when the server is unavailable.

To provide a good UX your app should be able to work offline and also display results as soon as they're requested. Historically this has been done by storing your data on SQLite, or a custom network cache. These options introduce maintenance overhead: SQLite requires a strict data model, careful database updates, and usage of sintactic sugar libraries like ORMs to make it palatable. The custom network cache depends on your server team introducing etags and other similar mechanisms, which are not always available.

RxObservableDiskCache relies on a technology that's common on desktop and server: key-value disk stores. By using [RxPaper](https://github.com/pakoito/RxPaper) behind the scenes it's able to efficiently store any result in an schemaless document just to refetch it later. This makes caching transparent for most network calls, you just need to configure them once with the correct caching policy. It's just one method call that wraps your `Single` or single value `Observable`.

To avoid your data getting stale due to time limits or versioning, RxObservableDiskCache allows you to store an arbitrary caching Policy. This Policy is any simple object that helps you identify whether your data is outdated and has to be removed. RxObservableDiskCache provides three different Policy objects, but you can create and use your own. This way you can decide how to handle staleness the same way you would do in SQLite or when using etags: by dropping the data, or programming defensively to account for model changes.

##Usage

####Storage

RxObservableDiskCache uses [RxPaper](https://github.com/pakoito/RxPaper) internally, so it's recommended to go to its [README](https://github.com/pakoito/RxPaper/blob/master/README.md) for reference on what Values are serializable, and what other behaviours are expected. RxObservableDiskCache is not opinionanted about the `RxPaperBook` you pass onto it, so feel free to use it externally to read, modify, or purge any data outside the RxObservableDiskCache scope.

####Policy

Behind the scenes Policy objects are stored and retrieved separately from Values to avoid unnecessary deserialization. They're checked before the Value is retrieved to see if it has to be deleted instead. Policy objects are recommended to be kept as small as possible.

Three simple Policy classes are included with RxObservableDiskCache: TimePolicy, VersionPolicy, and TimeAndVersionPolicy. You can still use any other class as Policy.

####Error handling

Any storage errors are: logged with a "cache miss" message, the current key and value get deleted, and the error is forwarded.

Any errors on the operation are forwarded too, like with any `Observable`.

Expect 0 results for cache and operation failures, 1 result when the cache is not found or valid and the operation succeeds, 1 result when the cache is found and valid but the operation fails, and 2 results when both the cache and the operation succeed.

There is a full test suite with examples on the sample project.

####Configuration

The configuration parameters are:

* The `Single` or single value `Observable` operation to be wrapped.
* A key string under which the Value will be stored.
* The [RxPaperBook](https://github.com/pakoito/RxPaper/blob/master/README.md#working-on-a-book) database where the Value and Policy will be stored.
* A creation and validation functions for the Policy.

####Static single use

`RxObservableDiskCache.transform()` are a set of methods you can call with any observable and configuration parameters that will return the transformed `Observable`.

```java
RxObservableDiskCache.
    <UserProfile, TimeAndVersionPolicy> transform(
        userRequest(),
        "user_profile",
        RxPaperBook.with("my_app_cache"),
        TimeAndVersionPolicy.<UserProfile>create(BuildConfig.VERSION_CODE),
        TimeAndVersionPolicy.validate(BuildConfig.VERSION_CODE))
    .subscribe(/* Do something withe the data */);
```
####Instance

`RxObservableDiskCache.create()` creates an instance of RxObservableDiskCache for the same book, Value and Policy that can be reused for different `Single`s or  single value `Observable`s.

```java
RxObservableDiskCache<UserProfile, TimeAndVersionPolicy> myCache =
    RxObservableDiskCache.create(
        RxPaperBook.with("my_app_cache"),
        TimeAndVersionPolicy.<UserProfile>create(BuildConfig.VERSION_CODE),
        TimeAndVersionPolicy.validate(BuildConfig.VERSION_CODE));

myCache.transform(userRequest(), "user_profile").subscribe(/* Do something withe the data */);

myCache.transform(userRequest("54663"), "friend_54663_profile").subscribe(/* Do something withe the data */);
```

##Distribution

Add as a dependency to your `build.gradle`
```groovy
repositories {
    ...
    maven { url "https://jitpack.io" }
    ...
}

dependencies {
    ...
    compile 'com.github.pakoito:RxObservableDiskCache:1.0.0'
    ...
}
```
or to your `pom.xml`
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.pakoito</groupId>
    <artifactId>RxObservableDiskCache</artifactId>
    <version>1.0.0</version>
</dependency>
```

##FAQ

#### How fast is it? Isn't there an overhead to be always getting old values?

As fast as the underlying [Paper](https://github.com/pilgr/Paper) library. Policy is used to reduce deserialization overhead when it's not required, so Values are only fetched when they're surely required and validated. [Kryo](https://github.com/EsotericSoftware/kryo) is binary serialization faster than Jackson and Gson.

####How do I deal with model updates?

Same way you do on SQLite: you drop the data or code defensively. The storage is schemaless, so no update scripts are required. The data is deserialized under the same premises as [Paper](https://github.com/pilgr/Paper)/[Kryo](https://github.com/EsotericSoftware/kryo), so their documentation is the best reference. The usage of Policy was introduced to automate the process, but you're free to ignore it and operate directly on the [RxPaperBook](https://github.com/pakoito/RxPaper/blob/master/README.md#working-on-a-book) you pass to the transformation.

####Why isn't it an `Observable` transformer instead?

Because it transforms from `Single` to `Observable`, and I wanted to keep the transformation explicit.

####I want to store an `Observable` that returns more than one result

Although it doesn't make much sense to me to duplicate every value on an `Observable` operation, you can do it like this:

```java
myNotSingleObservable.flatMap(
            value -> 
                RxObservableDiskCache.transform(
                        Single.just(value), /* rest of parameters */))
```

####If I cancel an operation it throws a `Single` or `Completable` incomplete exception

Database operations are not meant to be cancellable, and shouldn't be applied directly to UI precisely to avoid leaks. Apply them to a `PublishSubject` instead, and bind that subject directly to the view making sure that you `unsubscribe()` it when required.

##Contribution

PRs and suggestions for new features welcome.

For any error report please send an issue with a full stack trace and reproduction steps.

##License

Copyright (c) pakoito 2016

The Apache Software License, Version 2.0

See LICENSE.md