# Yukagram

Telegram X fork

* [**Latest workflow builds**](https://github.com/yukari-sys/Yukagram/actions?query=is%3Asuccess+workflow%3A%22Build+Yukagram%22+branch%3Amain)

Todo list:

- [x] Fix polystar sticker crash
- [x] Prebuild tdlib with github workflow
- [x] Github workflow for debug builds
- [x] Release builds using docker
- [x] A way to rebuild tdlib
- [x] Hide connection status bar
- [x] Yukagram settings
- [x] App logo
- [x] Disable in-app updates
- [x] Hide Help/Invite friends from drawer
- [x] Move calls, archive to the drawer
- [x] Hide trending stickers/emojis
- [x] Hide phone numbers
- [x] Hide camera button from input bar
- [x] Redesign emoji keyboard
- [ ] Redesign dialogs view (MainController)
- [ ] "My profile" drawer button
- [ ] Media/music player customizations
- [ ] Apple emojis for reactions
- [ ] Hiding premium stuff
- [ ] Font size customization
- [ ] Avatar roundness customization
- [ ] Remove not-bubble messages thingy
- [ ] Completely rewrite the "Themes and Chats" tab
- [ ] "Send with caption" for gifs
- [ ] Stickers downloading
- [ ] Video/audio messages downloading
- [ ] Send file as video/audio message
- [ ] Monet theme
- [ ] Login via Auth key
- [ ] Login via QR-Code
- [ ] Demo login servers
- [ ] Spoof device info on login
- [ ] Modernize UI
- [ ] Precompile all the JNI's
- [ ] Firebase push notifications
- [ ] First release
- [ ] CI builds in TG channel
- [ ] In-app updates from TG channel

## Build instructions

### Prerequisites

* Repository must be fetched via `git`
* **JDK** or **[Android Studio](https://developer.android.com/studio/)** (with compatible bundled JDK)
* At least **8GB** of RAM
* At least **7,32GB** of free disk space when cloning with `--shallow-submodules --depth=1`
* At least **2-5x** times more disk space for files generated during build process.

#### macOS

* [Homebrew](https://brew.sh)
* git with LFS: `$ brew install git git-lfs && git lfs install`
* JDK: `$ brew install openjdk@25`

#### Ubuntu

* git with LFS: `# apt install git git-lfs`
* Run `git lfs install` if you just installed `git-lfs`
* JDK: `apt install openjdk-25-jdk`
* If multiple JDK versions are installed: `update-java-alternatives --set java-1.25.0-openjdk-amd64`

#### Windows

* [MSYS2](https://www.msys2.org/#installation) (**UCRT64** environment)
* Update packages: `pacman -Syu`
* Run `pacman -S --needed git mingw-w64-ucrt-x86_64-git-lfs perl make diffutils`
* Run `git lfs install`
* Run `git config --global core.longpaths true`
* Clone the repository
* Set `msys2.dir` in `local.properties` after cloning

### Building

1. `git clone --recursive https://github.com/yukari-sys/Yukagram ytg`
2. In case you forgot the `--recursive` flag, `cd` into `ytg` directory and run: `git submodule update --init --recursive`
3. Open project via **[Android Studio](https://developer.android.com/studio/)** or build manually from the command line: `./gradlew assembleLatestUniversalDebug`
4. If build fails, follow the instructions provided in the error message.

#### Publishing

1. [Obtain Telegram API credentials](https://core.telegram.org/api/obtaining_api_id)
2. Create `local.properties` file in the root project folder using any text editor:

```properties
# Location where you have Android SDK installed
sdk.dir=ANDROID_SDK_FOLDER

# Telegram API credentials obtained at previous step
telegram.api_id=TELEGRAM_API_ID
telegram.api_hash=TELEGRAM_API_HASH
```
  
3. [Setup Firebase](https://firebase.google.com/docs/android/setup) and replace `google-services.json` with the one that's suitable for your `app.id`

#### Signing

1. Create keystore settings file:

```properties
# Absolute path to keytool generated keypair
keystore.file=/path/to/keytool-generated.pkcs12
keystore.password=KEYSTORE_PASS
key.alias=KEYSTORE_ALIAS
key.password=KEYSTORE_PASS
```

2. Put its path to `keystore.file` field in `local.properties`:

```properties
# Location of keystore settings file
# Can be relative here
keystore.file=/path/to/keystore.properties
```

3. Build the release: `./gradlew assembleLatestUniversalRelease`

### Building in container

1. Build the container: `docker build -t yukagram-builder .`
2. Run it: `docker run --rm -it -v "$PWD:/workspace" yukagram-builder bash`
3. Build as usual: `./gradlew assembleLatestUniversalDebug`
   
   OR:
   
   ```sh
   docker run --rm -v "$PWD:/workspace" yukagram-builder
   # runs ./gradlew assembleLatestUniversalDebug
   ```

### Tdlib rebuilding

By default, a pre-built version of TDLib from the [yukari-sys/tdlib build branch](https://github.com/yukari-sys/tdlib/tree/build) is used,
but you can also compile it yourself from the same source code if you wish.

```sh
cd tdlib
./build-tdlib.sh
```

## License

`Yukagram` is licensed under the terms of the GNU General Public License v3.0.

For more information, see [LICENSE](/LICENSE) file.

License of components and third-party dependencies it relies on might differ, check `LICENSE` file in the corresponding folder.
