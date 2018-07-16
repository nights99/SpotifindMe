# SpotifindMe - *Anywhere you go, Spotify will know*

..not in a "tracking and spying on you" kinda way though.  

[Hackaday.io project page](https://hackaday.io/project/159783-spotifindme)

## Introduction

*SpotifindMe* is an Android app that looks around for specific Bluetooth beacons, and transfers your Spotify playback from one device to another as you are moving around in different places in your home.

### Background

The summer has made it also to the more Northern parts of Finland, and I've been spending most of my days in the back yard with a laptop, listening to Spotify on my headphones. But here and there I gotta do some things on the inside as well, where I also have my desktop computer that is usually handling the playback. And with Spotify letting you switch playback between different devices, it was convenient to just do that whenever I went inside, and have the music almost seamlessly continue there. However, every so often I simply forgot the switch-over, and then had to make a detour inside to get the playback back going. I know, tell me about first world problems.

I was joking with myself about the idea to have some beacons or indoor positioning in place, and since I'm usually carrying my phone with me, an app that monitors your location, and depending where you are, transfers the playback to either one device through Spotify's API.

Well, the rest is history, and here's the code.

### Why do this and not just..

..do some local streaming on both devices?  
..use wireless headphones?  
..turn the inside up and keep the door open?

Well, because why not?  
Plus mosquitoes will get inside with the door open.


## Overview

There are two parts to this: the Android app, and a bunch of Bluetooth beacons (one can be enough though). Obivously you'll also need a Spotify account and at least two devices you want to switch between, but that is outside of this project's scope.

SpotifindMe uses the [AltBeacon](https://altbeacon.org/) protocol for [BLE beacon](https://en.wikipedia.org/wiki/Bluetooth_low_energy_beacon) ranging, and your laptop, some Raspberry Pi or an actual beacon can be used for the positioning. Of course, your phone can most likely also function as a BLE beacon, but unless you have a spare one running somewhere, the distance measuring will be pointless.

The app uses the matching [AltBeacon Android library](https://github.com/AltBeacon/android-beacon-library) to read the advertisement data from the beacons and get their approximate distance. A very simple check of the beacons' distances determines if the playback needs to be switched over to the other device. If that's the case, the app will send a playback transfer via the [Spotify Web API](https://developer.spotify.com/documentation/web-api/reference/). Before getting there, the user needs to sign in with their Spotify account, and gets a list of the currently available devices.

In case the ranging is acting up, or just for other reasons, the devices can be changed manually as well. However, if the ranging itself is still in place, it might instantly switch back to the previous device. There could be a "force override" or "pause ranging" option implemented at some point, but oh well, this is really just a proof of concept.

As said, the logic that determines if the playback should be transferred to another device is very simple and could use some tweaking to be more reliable, and quicker with the switch-over. At the moment, it takes maybe 5-10 seconds longer than it would be ideal. Again, first world problems.

## Code

### Beacons

The `tools/` directory contains the `altbeacon_transmit.sh` shell script based on the one from [RadiusNetworks' AltBeacon reference implementation](https://github.com/RadiusNetworks/altbeacon-reference/blob/master/altbeacon_transmit). It requires [BlueZ](http://www.bluez.org/) and therefore Linux (I guess?). But in the end, the script is simply setting up the BLE device as AltBeacon, by setting its advertisement data accordingly. That's a rather basic BLE thing to do, so other systems should support that somehow, too.

The app code expects two beacons, with the Beacon Unit ID (ID3) set to 0x0001 ans 0x0002 respectively. Since you will run it from two different places, you can keep the script as-is on once place, and on the other place modify the `AD_Data_ID3` variable to make it ID 0x0002:

```
AD_Data_ID3="00 01"    # Beacon Unit as 2-byte value
# change to:
AD_Data_ID3="00 02"    # Beacon Unit as 2-byte value
```

For best possible results (well, as good as it gets with BLE beacons), you should find a way to measure the signal strength (RSSI) of the beacon in one meter distance, as this functions as reference value to determine the distance from the beacon. Adjust the `AD_Data_Reference_RSSI` variable with your own measured value, but note that the value is a signed one-byte integer, so you need to take the two's complement of it.

Say you measure an average RSSI value of `-54`, you'll have to set `AD_Data_Reference_RSSI` to:
```
$ printf "%2x\n" $((-54 & 0xff))
ca
$
```

You may also have to adjust the `BLUETOOTH_DEVICE` variable if your BLE interface isn't `hci0`.

To run the script, simply call it. The internal calls will require root privileges, but the script itself is using `sudo` for that, so it may prompt for your password.

```
$ ./tools/altbeacon_transmit.sh
Transmitting AltBeacon profile with the following identifiers:
ID1: 53 70 6f 74 69 66 69 6e 64 4d 65 00 00 00 00 00
ID2: 00 01
ID3: 00 02
MFG RESERVED: 01

AltBeacon Advertisement: 1b ff 18 01 be ac 53 70 6f 74 69 66 69 6e 64 4d 65 00 00 00 00 00 00 01 00 02 ca 01

LE set advertise enable on hci0 returned status 12
< HCI Command: ogf 0x08, ocf 0x0008, plen 32
  1F 02 01 1A 1B FF 18 01 BE AC 53 70 6F 74 69 66 69 6E 64 4D 
  65 00 00 00 00 00 00 01 00 02 CA 01 
> HCI Event: 0x0e plen 4
  01 08 20 00 
$
```

The script will return, and the rest is done by the BLE device itself.

### Android app

Everything else in this repository is the Android application code. It was written with [Android Studio](https://developer.android.com/studio/), so importing it into there is probably the easiest.

* open Android Studio
* update Android Studio
* restart Android Studio
* update Gradle plugin
* install latest SDK versions
* import SpotifindMe project
* resolve missing dependencies
* update something else
* resolve more missing dependencies and version conflicts until it builds
* you're good to go

Seriously though, you can probably import the project directly from GitHub via *File* ->  *New* -> *Project from Version Control* -> *GitHub*, or then just clone it and import it via *File* -> *New* -> *Import Project...* and poiting to the cloned path.


## Running it yourself

### Beacons

Like stated above, the Android app expects two devices with Beacon Unit IDs 0x0001 and 0x0002 respectively, so either set up two devices accordingly, or adjust the `myBeacons` array in `MainActivity.java`.

### Android app

Since your devices will have most certainly different names than mine, you'll have to adjust the `myDevices` array in `MainActivity.java` with your own devices' names. You may also have a bit of tweaking of the values in the `checkSituation()` method in `MainActivity.java`.

Note that SpotifindMe needs Bluetooth to make any sense, so you cannot use the Emulator from Android Studio as it doesn't handle Bluetooth, and you'll need a real device to run the app. Also, the app will bug you to allow the location permission until you do, as Bluetooth won't work without that permission being granted.

When building The app, it is signed with your debug fingerprint. The app is registered with Spotify with the debug fingerprint of my own environment, so when you built the app yourself, you'll get an `Auth error: INVALID_APP_ID` error and cannot proceed. You will have to [register your own application with Spotify](https://developer.spotify.com/documentation/general/guides/app-settings/) and adjust the `CLIENT_ID` field in `LoginActivity.java` accordingly to your own client id, and the `REDIRECT_URI` to whatever you register your application with (or use the one from `LoginActivity.java` for yourself). You'll also have to register your own app fingerprint with your Spotify application. Your debug fingerprint should be fine, [here's some instruction how to get it](https://developers.google.com/android/guides/client-auth).

## The end of it

With all in place, set up and running, you should get the available devices shown, your beacons and their distances, and when you move around enough to meet the distance criterias in `checkSituation()`, your playback device should change along the way.

### Where to go from here

Chances are rather low that development will continue here, apart from some minor, personal tweaking to the playback transfer logic.

If I keep using the app myself (summers are short up here, so let's see how long I have use for it), I might think about adding some improvements to the token handling, as it now just silently does nothing if the token expired, which happens after one hour. Ideally the app would handle the token refreshment automatically in that situation.

Of course, this is all very static without much room for customization, but on the other hand, I don't think there is going to a big user base for this app (big being larger than 2, I'm optimistic), adding all the features to provide flexible set up and handling of beacons and devices will likely be in vain.

Prove me wrong  ¯\\\_(ツ)\_/¯
