# LolaDrives ReadMe

## Overview

1. Setup
2. RDE
3. Live Monitoring 


## 1. Setup

After opening LolaDrives, you will find yourself on the home screen. 
Here, you are able to see a list of available Bluetooth devices and your current Bluetooth connection status.
The first thing you want to do is to select the OBD Bluetooth adapter that is plugged into your car.
If your adapter does not appear in this list, try to connect to it manually via the Bluetooth settings of your smartphone (Settings -> Connections -> Bluetooth).
Once your adapter is shown, you can connect by clicking its MAC address.
A successful connection will be signaled by the keyword "connected", highlighted in blue.

A swipe from the left edge of the screen opens the main menu of LolaDrives, which helps you to navigate through the app.


## 2. RDE

By clicking on the RDE tab in the main menu you will be taken to the RDE Pre-Screen, where you will receive some introductory information.
If your Bluetooth connection is still active, you may switch from the RDE Pre-Screen to the RDE Configuration Screen by pressing the Start button at the bottom of the screen.
Here, you have the option to set the total distance of your RDE drive by entering it into the empty text field.
If you do not set a total distance and leave the text field empty, the recommended distance of 83km will be set automatically.

By clicking the Start button again, LolaDrives will begin to communicate with the connected OBD adapter and request information about your vehicle.
LolaDrives receives data packets and determines the sensor profile of the car.
If the data suffices, the app selects an appropriate specification and initializes the RTLola monitor.

After successful initialization, the UI switches to an RDE guiding view. 
From top to bottom, it shows the total time, which must be between 90 and 120min to finish the test, and the total distance travelled.
The next line indicates the current state of the conditions for a valid rde test drive disregarding emission data.
A yellow question mark indicates a RDE drive that is not yet valid, a green rake represents a valid RDE drive and a red cross marks a drive that is definitely not valid.
The latter verdict can occur far before the time limit is reached, caused by an irrecoverable situation such as transgression of the 160km/h speed limit.
Note that the indicator reports the current status if the test drive were to end in this moment.
Together with the regulatory constraints, this implies that the current verdict can alternate between success and failure from minute 90 to 120.
As there is no specific point in time when the test ends, the app continues to compute statistics until the tester manually stops it or the 120min mark is reached. 

Beneath the status indicator is the green NOx bar displaying the total NOx emission.
The vertical red bar denotes the permitted threshold of 168mg/km.

The next three UI groups represent the progress in each of the distinct segments: urban, rural, and motorway.
Each group consists of two horizontal bars. 
The gray progress bar displays the distance covered in the respective segment. 
The vertical blue indicators denote lower and upper bounds as per official regulation, for an expected trip length of 83km. 
The blue bar below the gray one illustrates two different metrics for the driving dynamics. 
Both dots need to remain below/above their thresholds. 
A more aggressive acceleration behavior shifts the dots to the right and a passive driving style to the left.


## 3. Live Monitoring

The app can display the received data in real-time.

By clicking the item "Profiles" in the main menu you can manage a list of profiles.
Each profile represents a set of sensors marked for logging.
You can add profiles by clicking on the plus symbol in the upper right corner and selecting your choice of sensors.
They can be edited by pressing the pen symbol in the respective row and selected by a click on its name.
After selecting a profile you can access the Live Monitoring screen over the main menu.
Here, LolaDrives displays the name of each selected sensor, the last measured value, and its unit.

You can start the Live Monitoring by clicking the start button granted your OBD adapter is connected.

You can access previous drives over the history interface accessible via the main menu.
By clicking on the respective drive in the list, you can scroll through the trip event by event in the detail view.
You have the option to display graphs such as the speed-time diagram of the selected ride, by using the Chart switch at the bottom of the screen.

## License
Distributed under the MIT License. See **LICENSE** for more information.

## Contact 

- Yannik Schnitzer - s8yaschn [[at]] stud.uni-saarland.de
- Sebastian Biewer - biewer [[at]] depend.uni-saarland.de

Project Link: https://github.com/udsdepend/loladrives-android
