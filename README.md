# Turn-by-Turn Navigation Module for Android
#### A turn-by-turn navigation module to integrate GPS navigation in Android apps

![](Readme-imgs/preview.png)



## Quick setup
### 1. Set your API keys
If you want to use Bing's routing API, please set it in the NavConfig.class.
You may build your own adapter for your desired routing API as well.
Note that this is not required to use the app. It will calculate routes in offline mode if it is not able to perform a request to a remote API.

![](Readme-imgs/key.png)


Don't forget to set you Google API key in the manifest as well.


### 2. Use the provided maps
As an example, a map of Portugal is provided. You can find the map and matching graph files under "\Maps".
Since the main map file (Portugal.map) was over 100MB, it was compressed. Unzip it before using.

Copy all the files in the "\Maps\Portugal" folder (after unzipping Portugal.map, for a total of 9 files) to the "download\TbtModuleMaps\Portugal" folder in your device.

You can also specify another location for the maps in the NavConfig.class.


### 3. Start the app
Start the app and, in overview mode, longpress the map itself until the drive promp appears. Choose one of the options to start driving:

![](Readme-imgs/drive.jpg)


Change to turn-by-turn mode if you want:

![](Readme-imgs/tbt.jpg)


You can view a list of the instructions at any time you are navigating by pressing the navigation bar in turn-by-turn mode:

![](Readme-imgs/list.jpg)



## History and goal
This project was developed from a navigation proof of concept I created for a larger comercial project during an internship. After the intership, I asked the company if they would mind if I released the code. They didn't. So I used it as a learning opportunity and (hopefully) created a turn-by-turn navigation module for Android applications, which anybody can use and improve.

The goal is to develop a free open-source turn-by-turn navigation alternative which can be integrated in any Android application.


## Using your own maps
I created a script to prepare the map files automatically. The script and the instructions to use it are provided in the project's "\Maps" folder.


## Key features
- May work enterely offline
    The system is prepared to work enterely offline, if required
- Tries to use as little mobile data as possible
    Since the core part of the navigation functionality was developed for another application, it is bound to some of the requirements defined for that app
    That is why the routing system tries to use as little mobile data as possible, by performing most route adjustments offline using the Graphhopper library
- The routing system was developed to use Adapters, so you can make your own adapter dor any Routing API
- If online, the system has the potential to display traffic information on the map



## Used Libraries
[GraphHopper for Android](https://github.com/graphhopper/graphhopper)
    
[OpenScienceMaps for Android](https://github.com/opensciencemap/vtm)

[GraphHopper and Vtm project](https://github.com/graphhopper/graphhopper/tree/master/android)
