# Wowza GoCoder SDK for Android Sample App

This repository contains a sample application that demonstrates the capabilities of the [Wowza GoCoder™ SDK for Android](https://www.wowza.com/products/gocoder/sdk) and is provided for developer educational purposes. To build your own application, you must use the GoCoder SDK.

## Prerequisites

- Wowza GoCoder SDK for Android v1.9.0.0632

> **Notes:**
>
> - The GoCoder SDK library necessary to build this app is not provided here. To get the free SDK, complete the [GoCoder SDK sign-up form](https://www.wowza.com/products/gocoder/sdk/license) to receive a link to allow you to download the SDK along with a license key. If you already have a license key, you can download the current SDK release [here](https://www.wowza.com/pricing/installer#gocodersdk-downloads). Be sure to include the ```.aar``` file in the libs directory.
> - In the **GoCoderSDKActivityBase.java** file located at **gocoder-sdk-sample-app/java/ui/GoCoderSDKActivityBase.java**, be sure to replace the **SDK_SAMPLE_APP_LICENSE_KEY** value with your license key. 
> - In the **defaultConfig** section of the *build.gradle* file located at **Gradle Scripts/build.gradle (Module: gocoder-sdk-sample-app)**, be sure to replace the applicationID value with the App ID provided in your GoCoder SDK license welcome email.

- [Android 5.0 (API level 21 or later) for broadcasting](https://developer.android.com/about/versions/android-5.0).
- [Android 6.0 (API level 23 or later) for playback](https://developer.android.com/about/versions/marshmallow/android-6.0).
- [Android Studio 3.4.0](https://developer.android.com/studio/index.html) or later.
- Access to a [Wowza Streaming Engine](https://www.wowza.com/products/streaming-engine)™ media server or a [Wowza Streaming Cloud](https://www.wowza.com/products/streaming-cloud)™ account. You can request a free 180-day Wowza Streaming Engine developer license by completing the [Wowza Streaming Engine Developer License form](https://www.wowza.com/media-server/developers/license) or sign up for a Wowza Streaming Cloud developer trial by completing the [Wowza Streaming Cloud Developer Free Trial form](https://www.wowza.com/pricing/cloud-developer-free-trial).

## Sample App Activities
The sample app includes a number of activities that demonstrate the features of the GoCoder SDK, including:

- CameraActivity.java, which uses the SDK to capture and broadcast video using the device's internal cameras,
- InfoActivity.java, which uses the SDK's version and device interrogation APIs to list detailed information about the SDK and the mobile device, and
- MP4BroadcastActivity.java, which uses **WOWZBroadcast**, **WOWZBroadcastAPI**, and related classes and interfaces to broadcast frames from a local MP4 file.

## More Resources
* [GoCoder SDK Technical Articles](https://www.wowza.com/docs/wowza-gocoder-sdk)
* [GoCoder SDK for Android Reference Docs](https://www.wowza.com/resources/gocodersdk/docs/api-reference-android/)
* [GoCoder SDK for Android Release Notes](https://www.wowza.com/docs/wowza-gocoder-sdk-release-notes-for-android)
* [Wowza GoCoder SDK Community Forum](https://www.wowza.com/community/spaces/36/wowza-gocoder-sdk.html)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/resources/developers) to learn more about our APIs and SDK.

## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License
This code is distributed under the [BSD 3-Clause License](https://github.com/WowzaMediaSystems/gocoder-sdk-samples-android/blob/master/LICENSE.txt).

![alt tag](http://wowzalogs.com/stats/githubimage.php?plugin=gocoder-sdk-samples-android)
