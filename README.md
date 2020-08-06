[![Project Status: WIP – Initial development is in progress, but there has not yet been a stable, usable release suitable for the public.](https://www.repostatus.org/badges/latest/wip.svg)](https://www.repostatus.org/#wip)

# EWPESmart Binding

This binding allows you too add EWPESmart Air Conditioners as things (Gree, Sinclair, maybe others as well). Once added as a thing, the user can control the Air Conditioner, similarly to how the Air Conditioner is controlled using the remote control or smartphone app.

Note : The EWPESmart Air Conditioner must already be setup on the wifi network and must have a fixed IP Address.

## Supported Things

This binding supports one Thing type: EWPEAirCon.

## Discovery

Discovery is possible but as yet is not supported in this binding.

## Thing Configuration

Currently can only be set up via thing-file. `ipAddress` and `broadcastIp` are required, `refresh` is optional (by default 2 seconds).

## Channels

The following channels are supported:

| channel        | type      | description                                             |
|----------------|-----------|---------------------------------------------------------|
| power          | Switch    | Power on/off the Air Conditioner                        |
| mode           | Number    | Sets the operating mode of the Air Conditioner<br> Mode: Auto: 0, Cool: 1, Dry: 2, Fan: 3, Heat: 4 |
| turbo          | Switch    | Set on/off the Air Conditioner's Turbo mode.            |
| light          | Switch    | Enable/disable the front display on the Air Conditioner |
| temp           | Number    | Sets the desired room temperature                       |
| tempSensor     | Number    | Shows the room temperature                              |
| swingVertical  | Number    | Sets the vertical swing action on the Air Conditioner<br> Full Swing: 1, Up: 2, MidUp: 3, Mid: 4, Mid Down: 5, Down: 6 |
| windSpeed      | Number    | Sets the fan speed on the Air conditioner<br> Auto:0, Low:1, MidLow:2, Mid:3, MidHigh:4, High:5 |
| air            | Switch    | Set on/off the Air Conditioner's Air function           |
| dry            | Switch    | Set on/off the Air Conditioner's Dry function           |
| health         | Switch    | Set on/off the Air Conditioner's Health function        |
| powerSave      | Switch    | Set on/off the Air Conditioner's Power Saving function  |

## Full Example

Things:

```
Thing ewpesmart:EWPEAirCon:000001 "AirCon" @ "Hall Way" [ ipAddress="192.168.1.123", broadcastIp="192.168.1.255", refresh=2 ]
```

Items:

```
Switch AirconPower                         { channel="ewpesmart:EWPEAirCon:000001:power" }
Number AirconMode                          { channel="ewpesmart:EWPEAirCon:000001:mode" }
Switch AirconTurbo                         { channel="ewpesmart:EWPEAirCon:000001:turbo" }
Switch AirconLight                         { channel="ewpesmart:EWPEAirCon:000001:light" }
Number AirconTemp "Temperature [%.1f °C]"  { channel="ewpesmart:EWPEAirCon:000001:temp" }
Number AirconRoomTemp "Room Temp[%.1f °C]" { channel="ewpesmart:EWPEAirCon:000001:tempSensor" }
Number AirconTempSet                       { channel="ewpesmart:EWPEAirCon:000001:temp" }
Number AirconSwingVertical                 { channel="ewpesmart:EWPEAirCon:000001:swingVertical" }
Number AirconFanSpeed                      { channel="ewpesmart:EWPEAirCon:000001:windSpeed" }
Switch AirconAir                           { channel="ewpesmart:EWPEAirCon:000001:air" }
Switch AirconDry                           { channel="ewpesmart:EWPEAirCon:000001:dry" }
Switch AirconHealth                        { channel="ewpesmart:EWPEAirCon:000001:health" }
Switch AirconPowerSaving                   { channel="ewpesmart:EWPEAirCon:000001:powerSave" }
```

Sitemap:

```
sitemap demo label="Demo Sitemap" {
  Frame label="Controls"
  {
     Switch item=AirconPower label="Power" icon=switch
     Switch item=AirconMode label="Mode" mappings=[0="Auto", 1="Cool", 2="Dry", 3="Fan", 4="Heat"]
     Setpoint item=AirconTemp label="Set temperature" icon=temperature minValue=16 maxValue=30 step=1
  }
  Frame label="Fan Speed"
  {
     Switch item=AirconFanSpeed label="Fan Speed" mappings=[0="Auto", 1="Low", 2="Medium Low", 3="Medium", 4="Medium High", 5="High"] icon=fan
  }
  Frame label="Fan-Swing Direction"
  {
     Switch item=AirconSwingVertical label="Direction" mappings=[0="Off", 1="Full", 2="Up", 3="Mid-up", 4="Mid", 5="Mid-low", 6="Down"] icon=flow
  }
  Frame label="Options"
  {
     Switch item=AirconTurbo label="Turbo" icon=fan
     Switch item=AirconLight label="Light" icon=light
     Switch item=AirconAir label="Air" icon=flow
     Switch item=AirconDry label="Dry" icon=rain
     Switch item=AirconHealth label="Health" icon=smiley
     Switch item=AirconPowerSaving label="Power Saving" icon=poweroutlet
  }
}
```

## Credits

Most of the initial code was taken from [jllcunha/openhab-greeair-binding](https://github.com/jllcunha/openhab-greeair-binding).

## Contribution

Always welcome.

## Warranty

The software is provided "as is", without warranty of any kind.
