# based on _redburn @ Stack Overflow

# Query Windows Location Service via Python winsdk

import asyncio
import winsdk.windows.devices.geolocation as wdg

async def getCoords():
    locator = wdg.Geolocator()
    pos = await locator.get_geoposition_async()
    return [pos.coordinate.longitude, pos.coordinate.latitude, pos.coordinate.position_source.name]

def getLoc():
    try:
        return asyncio.run(getCoords())
    except PermissionError:
        print("ERROR: You need to allow applications to access you location in Windows settings")

print(getLoc())
