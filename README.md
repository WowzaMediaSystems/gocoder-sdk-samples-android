# Wowza GoCoder SDK for Android Sample App
This repository contains a sample application that demonstrates the capabilities of the [Wowza GoCoder™ SDK for Android](https://www.wowza.com/products/gocoder/sdk) and is provided for developer educational purposes. To build your own application, you must use the GoCoder SDK.

## Development Requirements
1. **GoCoder SDK for Android v1.0.0.141**  

    :point_right: _**PLEASE NOTE** :point_left: The GoCoder SDK binaries necessary to build the sample apps are not provided here. To request a free trial of the SDK, send a message to [sdkteam@wowza.com](mailto:sdkteam@wowza.com) and you will receive a trial license key along with a link to download the SDK binaries._

2. Android SDK 4.4.2 or later.
3. Android Studio 1.2.0 or later.

## Sample App Activities
When you first run the sample app, it displays a list of activities that demonstrate the features of the GoCoder SDK.

![Sample App Activities](activities.png)

| Title | Activity Class | Description |
| --- | --- | --- |
|**Stream live video and audio** | `CameraActivity.java` |This activity demonstrates the primary camera, audio, and live streaming capabilities of the GoCoder SDK. |
| **Stream an MP4 file** | `MP4BroadcastActivity.java` |This activity demonstrates how to broadcast the frames from a local MP4 file using the **WZBroadcast**, **WZBroadcastComponent**, and related classes and interfaces, which provide lower-level access to the various components involved in a Wowza streaming broadcast. |
| **Capture an MP4 file** | `MP4CaptureActivity.java` |This activity saves the video from the camera to an MP4 file on the device's local storage as it's streamed. |

## More resources
[Wowza GoCoder SDK Developer Documentation](https://www.wowza.com/resources/gocodersdk/docs/1.0/)

[Wowza GoCoder SDK for Android API Reference](https://www.wowza.com/resources/gocodersdk/docs/1.0/api-reference-android/)

[Wowza GoCoder SDK for Android Release Notes](https://www.wowza.com/resources/gocodersdk/docs/1.0/release-notes-android/)

[Wowza GoCoder Product Page](https://www.wowza.com/products/gocoder)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/resources/developers) to learn more about our APIs and SDK.

## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

## License
This code is distributed under the [BSD 3-Clause License](https://github.com/WowzaMediaSystems/gocoder-sdk-samples-android/blob/master/LICENSE.txt).
