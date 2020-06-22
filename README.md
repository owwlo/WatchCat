# WatchCat
WatchCat - yet another Android Remote Surveillance Camera app.

## Features and Limitations
### Features
 * Based on a slightly modified version of [https://github.com/fyhertz/libstreaming](https://github.com/fyhertz/libstreaming)
	 * Android builtin Hardware Acceleration Video Encoding
	 * H264 & AAC Encoding
	 * Upto 1080p Video Streaming
* Automatic Service Discovery within the same LAN
* Play through Google ExoPlayer
#### TODOs
* ~~Use [Android ExoPlayer](https://github.com/google/ExoPlayer) to replace libvlc~~
* Password-based Access Control
* A better GUI?
* Logo
* tablet optimization

### Limitations
* The screen will be kept ON during streaming and standing by
	* App will set the "brightness" close to 0 to mitigate this issue
	* _I believe this is due to the Android system level policy. Personally, I haven't figured out a way to capture and to stream the video fully on background with screen off, if you know how to do this, please let me know ;)_

## Screenshots
TODO

## Tested Devices
* **[Android 10]** Xiaomi Mi 8 Lite
* **[Android 9]** Teclast T30 **_(tablet)_**
* **[Android 8]** Redmi 5 Plus

## Use of other Open Source Projects
* libstreaming [https://github.com/fyhertz/libstreaming](https://github.com/fyhertz/libstreaming)
* NsdHelper [https://github.com/rafakob/NsdHelper](https://github.com/rafakob/NsdHelper)
* fastjson [https://github.com/alibaba/fastjson](https://github.com/alibaba/fastjson)
* ExoPlayer [https://github.com/google/ExoPlayer](https://github.com/google/ExoPlayer)
* NanoHttpd [https://github.com/NanoHttpd/nanohttpd](https://github.com/NanoHttpd/nanohttpd)
