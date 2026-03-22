# 🛠️ CrossSset

<p align="center">
  <img src="https://github.com/user-attachments/assets/558cac8d-5670-40ee-85d3-4e48661cfd82" width="45%" />
  <img src="https://github.com/user-attachments/assets/0a94299a-d8d4-4299-a9fc-1d3317a2f9d5" width="45%" />
  <img width="1350" height="2524" alt="1001918007" src="https://github.com/user-attachments/assets/fc216a10-e709-4b23-8e55-e48c482c0541" width="45%" />
<img width="1322" height="2495" alt="1001918009" src="https://github.com/user-attachments/assets/3844e15c-d894-41f1-84ad-c36249ae550d" width="45%" />

</p>

**CrossSset** is a modern, powerful, and elegant alternative to the well-known *SetEdit*. Built from scratch in **Kotlin**, it is an advanced Android settings explorer and manager (System, Secure, and Global), focused on extreme performance and native integration with **Shizuku**.

> 💡 **Why CrossSset?** Unlike the original SetEdit, CrossSset uses a modern asynchronous architecture (Coroutines), does not freeze with large lists, and introduces the exclusive **Watchdog** feature to lock values in real time.

![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)
![Kotlin](https://img.shields.io/badge/language-Kotlin-purple.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Shizuku](https://img.shields.io/badge/powered%20by-Shizuku-brightgreen.svg)

---

## 🔥 Evolution compared to SetEdit

| Feature | SetEdit (Original) | CrossSset (Modern) |
| :--- | :---: | :---: |
| **Language** | Java (Legacy) | **Kotlin (Modern)** |
| **Interface** | Android 4.0 Style | **Material 3 / AMOLED** |
| **Performance** | Slow on large lists | **Lazy Loading (Instant)** |
| **Protection** | None | **Watchdog (Real-time Lock)** |
| **Search** | Basic | **Instant with Cache** |
| **Stability** | Prone to ANRs | **Zero ANR (Multi-threaded)** |

---

## ✨ Main Features

### 🛡️ Watchdog (The Guardian)
- The missing feature from SetEdit: monitor specific settings and, if the system or another app tries to change them, Watchdog instantly forces them back to your custom value.

### 📦 Complete Management
- **Smart Tabs:** Intuitive organization between `System`, `Secure`, and `Global` tables.
- **History & Undo:** Track all changes with quick undo support.
- **Backup & Restore:** Export your optimizations as ZIP files and restore them on any device.

### 🎨 Design
- **Material 3:** Modern components, smooth animations, and color-based risk indicators.

---

## 🛠️ Technologies Used

- **Language:** [Kotlin](https://kotlinlang.org/)
- **Concurrency:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **UI:** ViewPager2, TabLayout, SwipeRefreshLayout, Material Components
- **Privileges:** [Shizuku API](https://shizuku.rikka.app/) (Transparent access to the `settings` shell)
- **Localization:** Supports Portuguese (BR), English, and Spanish.

---

## 🚀 How to Use

1. **Install Shizuku:** The engine that allows system modification without root. [Download here](https://shizuku.rikka.app/download/)
2. **Authorize CrossSset:** Open the app and grant permission through the Shizuku dialog.
3. **Explore and Optimize:** Search for keys like `animator_duration_scale` and feel the difference in system smoothness.
4. **Protect your keys:** Enable **Watchdog** in the edit dialog to ensure the system does not revert your changes.

---
