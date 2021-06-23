# dji-msdk-examples
This repository contains examples of DJI MSDK usage.

## Purpose of branch
This is a test case for confirm (or not) landing issue on M300 during the mission.

Major trajectory: 
- Launch app
- Start simulator
- Upload demo mission
- Start mission execution
- *Wait until the drone will start flying the mission*
- Press LAND one time - the drone will pause the mission but not land
- Press LAND second time - it will start landing

### Notes
V1 of SDK works normal - the drone starts landing immediately. 
