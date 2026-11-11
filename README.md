# Well Control: Android app

Controls your wells, water motor, compressor and other devices from one screen.
**No internet and no SIM are used.** The phone talks to the home box over local Wi-Fi.

The app starts in **Demo mode**, so you can try everything before the hardware is built.

---

## Get the APK: choose ONE way

### Way 1: GitHub builds it for you (no software to install)

1. Make a free account at **github.com** (if you don't have one).
2. Click **+ → New repository**, name it `WellControl`, choose **Private**, and click **Create**.
3. On the new repository page, click **"uploading an existing file"**.
4. Unzip `WellControl.zip` on your computer. Drag **everything inside the `WellControl` folder**
   into the upload box (including the hidden `.github` folder; see the tip below). Click **Commit changes**.
5. Open the **Actions** tab. A job called **"Build APK"** starts. Wait about 5–8 minutes until it shows a green ✓.
6. Click the finished job. At the bottom, under **Artifacts**, download **WellControl-apk**.
   It's a zip file with `app-debug.apk` inside.

> **Tip:** Your computer may hide folders whose names start with a dot. If the `.github` folder
> doesn't upload, create it on GitHub instead: **Add file → Create new file**, type the name
> `.github/workflows/build-apk.yml`, and paste in the contents of that file from the zip.

### Way 2: Android Studio (on a laptop)

1. Install **Android Studio** (free) from developer.android.com/studio.
2. **File → Open** and choose the `WellControl` folder. Wait for "Gradle sync" to finish (the first time takes a while).
3. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. Click **locate** in the pop-up. That folder contains `app-debug.apk`.
   (Or plug your phone in with USB debugging on and press the green ▶ Run button.)

---

## Install on the phone

1. Copy `app-debug.apk` to the phone (WhatsApp it to yourself, use a USB cable, or Google Drive).
2. Tap the file. Android asks to **"Allow from this source"**. Allow it, then tap **Install**.
3. Open **Well Control**. It starts in Demo mode.

Share the same APK with family members. Everyone's app talks to the same home box.

---

## Using the app

* **Devices tab:** a water-themed top card shows how many motors are running, the total load, the
  longest run and the next auto-off, with **Stop all motors**. Below it every motor is a tile with an
  iOS-style toggle switch; a running tile turns into moving blue water. After you flip a switch the tile
  shows *Starting… → Waiting for sensor → Confirmed ON/OFF* or *No response* (red), with a pop-up and a
  small vibration.
* **Tap a motor** to open its details: a round gauge with the run time and auto-off countdown, a
  **Current level** meter (the amps measured by the clamp sensor, shown whether the motor is on or off),
  the confirmation message and a big **Turn on / Turn off** button.
* **Setup tab:**
  * **View:** show the motors on the Devices tab as **Tiles** (two per row) or as a **List** (one per row).
  * **Demo mode** switch: turn it OFF when the real home box is ready.
  * **Home box address:** usually `192.168.4.1` (shown on the home box screen).
  * **Devices:** add, edit or delete them. For each one, set:
    * *What is it?* (well, water motor, compressor, other)
    * *How is it switched?*
      **Starter** = 2 relays that press the ON and OFF buttons.
      **Direct** = 1 relay that holds a contactor.
    * *Box no.* and *Relay no.* (which box and which relay it is wired to)
    * *Auto-off* in minutes (0 = never)
  * **Send setup to box:** copies your device list into the home box.

---

## For the firmware: home box API

The home box (ESP32) must serve this small JSON API over HTTP on port 80.

| Method | Path | Body | Reply |
|---|---|---|---|
| GET | `/api/status` | – | `{"devices":[{"id":1,"online":true,"running":true,"amps":6.4,"runSecs":120,"fault":""}]}` |
| POST | `/api/cmd` | `{"id":1,"action":"on","autoOffMin":45}` | `{"ok":true}` |
| POST | `/api/devices` | `{"devices":[{"id":1,"name":"Well 1","kind":"WELL","type":"STARTER","box":1,"channel":1,"autoOffMin":45}]}` | `{"ok":true}` |

* `action` is `"on"` or `"off"`.
* `fault` is an empty string when everything is OK, otherwise a short message such as `"Dry run - stopped"`.
* `online:false` means the home box couldn't reach that device's well box over LoRa.

The app polls `/api/status` every 2 seconds while it is open.

---

## Technical details

* Kotlin + Jetpack Compose (Material 3), minSdk 26 (Android 8.0+), targetSdk 35
* Only standard Android/Jetpack libraries. The INTERNET permission is used only to reach the home box on local Wi-Fi.
* Requests are sent over the Wi-Fi network even when it has no internet, so Android doesn't send them over mobile data

## Credits

The app uses the **Outfit** typeface (SIL Open Font License 1.1, see `licenses/Outfit-OFL.txt`).
The water artwork was generated for this project.
