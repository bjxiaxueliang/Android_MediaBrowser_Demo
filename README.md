## 如果此工程代码对您有帮助，请扫描以下二维码支持我！
![在这里插入图片描述](https://raw.githubusercontent.com/xiaxveliang/GLES2_Anima_LoadFrom_Obj/master/image/pay_2rmb.png)

csdn 原文地址：
https://blog.csdn.net/xiaxl/article/details/78780691

# MediaBrowserCompat MediaBrowserServiceCompat


学习代码参考：
[googlesamples/android-MediaBrowserService](https://github.com/googlesamples/android-MediaBrowserService)

我的源码注释与简单代码修改：
[AndroidHighQualityCodeStudy/android-GoogleSample-MediaBrowserService](https://github.com/AndroidHighQualityCodeStudy/android-GoogleSample-MediaBrowserService)

`MediaBrowserCompat`与`MediaBrowserServiceCompat`为Android Support Library 23.2新增API，为一套CS架构的媒体播放框架。

+ MediaBrowserServiceCompat继承字Service，为运行在后台的音频服务。
+ MediaBrowserCompat 运行与前台，通过MediaSessionCompat与MediaBrowserServiceCompat建立连接，从而获取到一个MediaControllerCompat用于控制前台音频播放。
+ MediaControllerCompat UI界面控制音频播放进度、播放速度、上一曲、下一曲音频播放等等。
+ MediaSessionCompat 运行于后台的MediaBrowserServiceCompat与运行于前台的MediaBrowserCompat通过MediaSessionCompat来建立连接。
+ MediaSessionCompat.Callback 用户通过MediaControllerCompat对UI的操作，会通过MediaSessionCompat.Callback 回调到Service端，来操纵“播放器”进行播放、暂定、快进、上一曲、下一曲等操作
+ MediaControllerCompat.Callback Service端播放器播放完成、播放下一曲等操作，通过MediaControllerCompat.Callback回调到UI页面，操纵UI的变化。


因此 `MediaBrowserCompat`与`MediaBrowserServiceCompat` 仅为一套CS架构的播放框架，其对应的播放器，既可以为系统播放器`MediaPlayer`亦可以为其他自定义的播放器 `XXXMediaPlayer`。


## 关于MediaBrowserService官方demo中这样写道

+ Set a MediaSession.Callback class on the MediaSession. The callback class will receive all the user's actions, like play, pause, etc; **用户的UI操作，最终通过MediaSession.Callback回调到Service端**
+ Handle all the actual music playing using any method your app prefers (for example, the Android MediaPlayer class) **MediaBrowserService可以控制任意播放器播放，例：MediaPlayer**
+ Whenever it changes, update info about the playing item and the playing queue using MediaSession corresponding methods (setMetadata, setPlaybackState, setQueue, setQueueTitle, etc) **这里通过阅读源码得知(setMetadata, setPlaybackState, setQueue, setQueueTitle, etc) 这些方法最终回调到了Client端的MediaControllerCompat.Callback方法。**
+ Handle AudioManager focus change events and react appropriately (e.g. pause when audio focus is lost) **音频焦点变化的管理**

##MediaSession架构
来自https://github.com/kevinshine/android-UniversalMusicPlayer-Analysis
的MediaSession架构图
![这里写图片描述](https://raw.githubusercontent.com/xiaxveliang/Android_MediaBrowser_Demo/master/image/0001.png)

## 官方demo android-MediaBrowserService的架构图
![这里写图片描述](https://raw.githubusercontent.com/xiaxveliang/Android_MediaBrowser_Demo/master/image/0002.png)

## 源码阅读，可以参考我的注释demo

[AndroidHighQualityCodeStudy/android-GoogleSample-MediaBrowserService](https://github.com/AndroidHighQualityCodeStudy/android-GoogleSample-MediaBrowserService)

## 播放网络音频 封装

[Android_MediaBrowserCompat_Demo](https://github.com/xiaxveliang/Android_MediaBrowserCompat_Demo)




