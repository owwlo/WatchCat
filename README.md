# <img src="/github_resources/logo.256x256.png" alt="[image:Logo]" width="64"> WatchCat
WatchCat - yet another Android Remote Surveillance Camera app.

## Features and Limitations
### Features
 * Based on a modified version of [https://github.com/fyhertz/libstreaming](https://github.com/fyhertz/libstreaming)
	 * Android builtin Hardware Acceleration Video Encoding
	 * H264 & AAC Encoding
	 * Upto 1080p Video Streaming
* Automatic Service Discovery within the same LAN
* Play through Google ExoPlayer

### Limitations
* The screen will be kept ON during streaming and standing by
	* App will set the "brightness" close to 0 to mitigate this issue
	* _I believe this is due to the Android system level policy. Personally, I haven't figured out a way to capture and to stream the video fully on background with screen off, if you know how to do this, please let me know ;)_

## Screenshots
<img src="/github_resources/screenshots/watchcat.screenshot.main.png" alt="[image:List of Cameras]" width="400"/>
<img src="/github_resources/screenshots/watchcat.screenshot.main.landscape.png" alt="[image:List of Cameras(Landscape)]" width="400"/>
<img src="/github_resources/screenshots/watchcat.screenshot.camera_mode.png" alt="[image:Camera Mode]" width="400"/>

## Tested Devices
* **[Android 10]** Teclast T30 **_(tablet)_** _(via [treble_experimentations](https://github.com/phhusson/treble_experimentations))_
* **[Android 10]** Xiaomi Mi 8 Lite
* **[Android 9]** Teclast T30 **_(tablet)_**
* **[Android 8]** Redmi 5 Plus

## Use of other Open Source Projects
* libstreaming [https://github.com/fyhertz/libstreaming](https://github.com/fyhertz/libstreaming)
* NsdHelper [https://github.com/rafakob/NsdHelper](https://github.com/rafakob/NsdHelper)
* fastjson [https://github.com/alibaba/fastjson](https://github.com/alibaba/fastjson)
* ExoPlayer [https://github.com/google/ExoPlayer](https://github.com/google/ExoPlayer)
* NanoHttpd [https://github.com/NanoHttpd/nanohttpd](https://github.com/NanoHttpd/nanohttpd)
* Dexter [https://github.com/Karumi/Dexter](https://github.com/Karumi/Dexter)
* FloatingActionButtonSpeedDial [https://github.com/leinardi/FloatingActionButtonSpeedDial](https://github.com/leinardi/FloatingActionButtonSpeedDial)
* PinLockView [https://github.com/aritraroy/PinLockView](https://github.com/aritraroy/PinLockView)
* EventBus [https://github.com/greenrobot/EventBus](https://github.com/greenrobot/EventBus)
