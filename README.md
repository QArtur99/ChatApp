# ChatApp
ChatApp is a free messaging app inspired by messaging & social media apps. Written in Kotlin implements the Jetpack libraries, Firebase services.

### Screenshots
![image](https://user-images.githubusercontent.com/25232443/63807967-22693480-c91f-11e9-8f22-af367171ae00.png)

### Used Tech
* [Kotlin](https://kotlinlang.org/)
* [MVVM](https://developer.android.com/jetpack/docs/guide)
* [Coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) - Asynchronous programming 
* [Data Binding](https://developer.android.com/topic/libraries/data-binding/) - Declaratively bind observable data to UI elements.
* [Lifecycles](https://developer.android.com/topic/libraries/architecture/lifecycle) - Create a UI that automatically responds to lifecycle events.
* [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) - Build data objects that notify views when the underlying database changes.
* [Navigation](https://developer.android.com/guide/navigation/) - Handle everything needed for in-app navigation.
* [Paging](https://developer.android.com/topic/libraries/architecture/paging/) - Load and display small chunks of data at a time.
* [Room](https://developer.android.com/topic/libraries/architecture/room) - Access your app's SQLite database with in-app objects and compile-time checks.
* [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - Store UI-related data that isn't destroyed on app rotations. Easily schedule asynchronous tasks.
* [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - Schedule deferrable, asynchronous tasks even if the app exits or device restarts.
* [Firebase](https://firebase.google.com/docs) - Tools to develop high-quality apps.
  - [Authentication](https://firebase.google.com/docs) - Allows an app to securely save user data in the cloud.
  - [Cloud Firestore](https://firebase.google.com/docs/firestore) - Flexible, scalable NoSQL cloud database to store and sync data.
  - [Cloud Functions](https://firebase.google.com/docs/functions) - Automatically run backend code in response to events triggered by Firebase 
  - [Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) - Notify a client app.
  - [Cloud Storage](https://firebase.google.com/docs/storage) - Store and serve user-generated content.
  - [Remote Config](https://firebase.google.com/docs/remote-config) - Change the settings of app without requiring users to download an app update.
* [Dagger 2](https://github.com/google/dagger) - Compile-time framework for dependency injection.
* [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) - Compile-time framework for dependency injection.
* [Glide](https://github.com/bumptech/glide) - Load and cache images by URL.
* [Moshi](https://github.com/square/moshi) - Serialize Kotlin objects and deserialize JSON objects.
* [Retrofit 2](https://github.com/square/retrofit) - Handle REST api communication.
* [Test](https://developer.android.com/training/testing/) - An Android testing framework for unit and runtime UI tests.
* [ktlint](https://ktlint.github.io/) - Enforce Kotlin coding styles.


### Features
* Firebase: 
  - Authentication (Email, Facebook, Google, Phone)
  - Cloud Firestore
  - Cloud Messaging
  - Functions
  - Remote Config
  - Storage
* Create user profile (Username).
* Search users by Username
* Send text, graphic, audio messages.
* Take a picture from camera or gallery.
* Record/play audio.
* Display graphics in full screen view, zoom in/out, drag and rotate.
* Check user online status.
* Check message read/delivery status.
* Get notifications about new messages.


### Report issues
Something not working quite as expected? Do you need a feature that has not been implemented yet? Check the [issue tracker](https://github.com/QArtur99/ChatApp/issues) and add a new one if your problem is not already listed. Please try to provide a detailed description of your problem, including the steps to reproduce it.

### Contribute
Awesome! If you would like to contribute with a new feature or submit a bugfix, fork this repo and send a pull request. Please, make sure all the [unit tests](https://github.com/QArtur99/ChatApp/tree/master/app/src/test/java/com/artf/chatapp), [integration tests](https://github.com/QArtur99/ChatApp/tree/master/app/src/androidTest/java/com/artf/chatapp)  & `./gradlew spotlessApply` are passing before submitting and add new ones in case you introduced new features.

### How to run the project in development mode
* Clone or download repository as a zip file.
* Open project in Android Studio.
* Set Facebook API key in build.gradle.
* Run 'app' `SHIFT+F10`.

#### Getting Started
* Create Firebase project.
* In Firebase console enable all Firebase services listed in section Features.
* To turn on notifications deploy following [function](https://github.com/QArtur99/ChatApp/tree/master/chatFun/functions/index.js) to Firebase functions service. 

#### Android Studio IDE setup 
ChatApp uses [ktlint](https://ktlint.github.io/) to enforce Kotlin coding styles.
Here's how to configure it for use with Android Studio (instructions adapted from the ktlint [README](https://github.com/shyiko/ktlint/blob/master/README.md)):
* Close Android Studio if it's open
* Download ktlint using these [installation instructions](https://github.com/shyiko/ktlint/blob/master/README.md#installation)    
* Inside the project root directory run: `./ktlint --apply-to-idea-project --android`    
* Remove ktlint if desired: `rm ktlint`
* Start Android Studio

### License
    Copyright 2019 Artur Gniewowski
 
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

