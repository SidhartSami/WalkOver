<a id="readme-top"></a>

[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

<br />
<div align="center">
<a href="https://play.google.com/store/apps/details?id=com.sidhart.walkover&hl=en">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="200" alt="Get it on Google Play" />
  </a>
  <h3 align="center">WalkOver</h3>

  <p align="center">
    A modern, social fitness application reimaging the walking experience.
    <br />
    <strong>Social Fitness Tracking • Real-time Maps • Gamified Challenges</strong>
    <br />
    <br />
    <a href="https://github.com/SidhartSami/WalkOver/issues/new?labels=bug&template=bug-report---.md">Report Bug</a>
    ·
    <a href="https://github.com/SidhartSami/WalkOver/issues/new?labels=enhancement&template=feature-request---.md">Request Feature</a>
  </p>
</div>

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
        <li><a href="#key-features">Key Features</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#availability">Availability</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

## About The Project

WalkOver is a comprehensive social fitness application designed to make walking fun, engaging, and competitive. Unlike standard step counters, WalkOver integrates real-time social features, interactive mapping, and gamification to motivate users to stay active.

The application allows users to track their daily activity, compete with friends on leaderboards, participate in daily generated challenges, and visualize their journeys on an interactive map.

### Key Features

**Core Tracking:**
- **Precise Step Counting**: Accurate daily step and distance tracking using device sensors.
- **Live Walk Mode**: Real-time GPS tracking of walking sessions with detailed stats (speed, distance, duration).
- **History & Analysis**: Comprehensive weekly and monthly statistics with visual graphs.

**Social & Gamification:**
- **Leaderboards**: Global and friends-only leaderboards to foster healthy competition.
- **Daily Challenges**: System-generated daily goals (e.g., "Walk 5km", "Walk for 30 mins") that award XP.
- **XP & Leveling System**: Earn experience points for activities to level up your profile.
- **Streak System**: Logic that rewards consistency with streak bonuses and milestones.

**Technical Highlights:**
- **Modern UI/UX**: Built entirely with **Jetpack Compose** following Material Design 3 guidelines.
- **Cloud Sync**: Full data synchronization using **Firebase Firestore**.
- **Interactive Maps**: Integration with **OSMDroid** for rich map interfaces and route visualization.
- **Background Services**: Robust foreground services for reliable tracking even when the app is closed.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

* [![Kotlin][Kotlin-badge]][Kotlin-url]
* [![Android][Android-badge]][Android-url]
* [![Firebase][Firebase-badge]][Firebase-url]
* [![Jetpack Compose][Compose-badge]][Compose-url]

**Key Technologies:**
- **Kotlin** - Primary development language.
- **Jetpack Compose** - Modern toolkit for building native UI.
- **Firebase Auth & Firestore** - Authentication and real-time database.
- **OSMDroid** - Open source alternative to Google Maps for rendering map views.
- **Coroutines & Flow** - Asynchronous programming and reactive data streams.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Getting Started

### Prerequisites

Ensure you have the following installed on your development environment:

* **Android Studio** - Iguana or newer recommended.
* **JDK 17** - Required for building the project.
* **Android Device/Emulator** - Running Android 8.0 (API Level 26) or higher.

### Installation

1. Clone the repository
   ```sh
   git clone https://github.com/SidhartSami/WalkOver.git
   ```

2. Open the project in Android Studio.

3. Sync Gradle files to download dependencies.

4. Configuration:
   - This project uses Firebase. You must place your own `google-services.json` file in the `app/` directory.
   - Configure local properties if necessary for API keys.

5. Build and Run:
   - Select your target device and click "Run" (Shift+F10).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Availability

**WalkOver is available on the Google Play Store!**

Download it today to start your fitness journey.

<a href='https://play.google.com/store/apps/details?id=com.sidhart.walkover'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='200'/></a>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## License

Distributed under the MIT License. See `LICENSE` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Contact

**Sidhart Sami** - [LinkedIn](https://www.linkedin.com/in/sidhart-sami/) - sidhartsami@gmail.com

Project Link: [https://github.com/SidhartSami/WalkOver](https://github.com/SidhartSami/WalkOver)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Acknowledgments

- [Android Developers](https://developer.android.com/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [OSMDroid Library](https://github.com/osmdroid/osmdroid)
- [Jetpack Compose Guidelines](https://developer.android.com/jetpack/compose)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

[forks-shield]: https://img.shields.io/github/forks/SidhartSami/WalkOver.svg?style=for-the-badge
[forks-url]: https://github.com/SidhartSami/WalkOver/network/members
[stars-shield]: https://img.shields.io/github/stars/SidhartSami/WalkOver.svg?style=for-the-badge
[stars-url]: https://github.com/SidhartSami/WalkOver/stargazers
[issues-shield]: https://img.shields.io/github/issues/SidhartSami/WalkOver.svg?style=for-the-badge
[issues-url]: https://github.com/SidhartSami/WalkOver/issues
[license-shield]: https://img.shields.io/github/license/SidhartSami/WalkOver.svg?style=for-the-badge
[license-url]: https://github.com/SidhartSami/WalkOver/blob/main/LICENSE
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://www.linkedin.com/in/sidhart-sami/
[Kotlin-badge]: https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white
[Kotlin-url]: https://kotlinlang.org/
[Android-badge]: https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white
[Android-url]: https://developer.android.com/
[Firebase-badge]: https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black
[Firebase-url]: https://firebase.google.com/
[Compose-badge]: https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white
[Compose-url]: https://developer.android.com/jetpack/compose
