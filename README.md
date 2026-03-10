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
    A social fitness app that turns your walks into territory.
    <br />
    <strong>Claim Territory • Challenge Friends • Own Your City</strong>
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
    <li><a href="#about-the-project">About The Project</a></li>
    <li><a href="#walk-modes">Walk Modes</a></li>
    <li><a href="#social-system">Social System</a></li>
    <li><a href="#technical-design">Technical Design</a></li>
    <li><a href="#built-with">Built With</a></li>
    <li><a href="#getting-started">Getting Started</a></li>
    <li><a href="#availability">Availability</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

---

## About The Project

WalkOver is a social fitness application that makes walking competitive and territorial. The core idea is simple: the areas you walk become yours. Every session you complete draws a polygon from your GPS path, claims that region on a shared map, and marks it under your name — visible to all users. Walk somewhere someone else already owns, and ownership transfers to you.

Beyond territory, WalkOver layers in friend challenges, daily goals, XP progression, and multiple walk modes to suit how you want to move on any given day.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Walk Modes

WalkOver offers three distinct modes, each serving a different intent:

### Ghost Mode
A personal, distraction-free tracking experience. Your walk is recorded — steps, distance, duration, and route — but nothing is shared. Territory is not claimed and no social data is updated. Useful for casual walks where you just want a log.

### Duel Mode
A head-to-head challenge between you and a friend. You issue a challenge for either 3 or 7 days. Whoever accumulates more total kilometers by the end wins. Duels are tracked in real-time so both players can see the current standings throughout the challenge window.

### Competitive Mode
The core WalkOver experience. Every walk you complete in this mode traces a polygon from your GPS path and claims that area as your territory on the shared map. Territory is visible to all users. If another user walks through your claimed area in Competitive Mode, ownership is reassigned to them. Holding territory requires you to keep walking it.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Social System

WalkOver includes a friend system built around following — referred to in-app as **Stalking**, a deliberate design choice to keep the tone playful. You can stalk any user to track their activity, compare stats, and challenge them to duels.

Additional social features:

- **Global and Friends Leaderboards** — ranked by total kilometers and XP
- **Daily Challenges** — server-generated goals synced across all users at the same level, ensuring everyone competing at a given tier faces identical targets
- **XP and Leveling** — earned through walks, challenges, and streaks
- **Streak System** — rewards consistency with milestone bonuses

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Technical Design

### Architecture
WalkOver is built on **MVVM (Model-View-ViewModel)** with Jetpack Compose handling the UI layer. Data flows reactively through Kotlin Coroutines and Flow, keeping the ViewModel and UI decoupled and testable.

### Territory System
The most technically involved part of the project. During a Competitive Mode session, the app continuously captures GPS coordinates as GeoPoints. On session end, these points are used to construct a polygon representing the walked area, which is stored in Firebase Firestore.

The challenge is ownership resolution: when a new polygon is submitted, the system checks whether it intersects with any existing claimed territory. If overlap is detected, ownership of the affected region is reassigned to the new walker. This is a computational geometry problem — polygon intersection on top of a NoSQL database that has no native spatial query support — solved client-side with coordinate-based boundary logic before writing the result back to Firestore.

### Background Tracking
GPS tracking and step counting run inside a **foreground service**, keeping the session alive reliably even when the app is backgrounded or the screen is off. This is handled carefully to maintain accuracy without excessive battery drain.

### Challenge Consistency
Daily challenges are generated server-side and keyed by user level, ensuring all users at the same tier receive identical goals simultaneously. This prevents any advantage from client-side timing differences and keeps leaderboard competition fair.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Built With

* [![Kotlin][Kotlin-badge]][Kotlin-url]
* [![Android][Android-badge]][Android-url]
* [![Firebase][Firebase-badge]][Firebase-url]
* [![Jetpack Compose][Compose-badge]][Compose-url]

**Key Technologies:**
- **Kotlin** — Primary development language
- **Jetpack Compose** — Modern declarative UI toolkit following Material Design 3
- **Firebase Auth & Firestore** — Authentication and real-time cloud database
- **OSMDroid** — Open source map rendering and route visualization
- **Coroutines & Flow** — Asynchronous programming and reactive data streams

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Getting Started

### Prerequisites

- **Android Studio** — Iguana or newer recommended
- **JDK 17** — Required for building the project
- **Android Device/Emulator** — Running Android 8.0 (API Level 26) or higher

### Installation

1. Clone the repository
   ```sh
   git clone https://github.com/SidhartSami/WalkOver.git
   ```

2. Open the project in Android Studio.

3. Sync Gradle files to download dependencies.

4. Configuration:
   - This project uses Firebase. Place your own `google-services.json` file in the `app/` directory.
   - Configure local properties if necessary for API keys.

5. Build and Run:
   - Select your target device and click **Run** (Shift+F10).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Availability

**WalkOver is live on the Google Play Store.**

<a href='https://play.google.com/store/apps/details?id=com.sidhart.walkover'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='200'/></a>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## License

Distributed under the MIT License. See `LICENSE` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Contact

**Sidhart Sami** — [LinkedIn](https://www.linkedin.com/in/sidhart-sami/) — sidhartsami@gmail.com

Project Link: [https://github.com/SidhartSami/WalkOver](https://github.com/SidhartSami/WalkOver)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

## Acknowledgments

- [Android Developers](https://developer.android.com/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [OSMDroid Library](https://github.com/osmdomain/osmdroid)
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
