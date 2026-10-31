# 🎬 CineMatch Java | Intelligent Movie Recommendation & Analytics Engine

**CineMatch** is a robust, feature-rich Java console application built to deliver a personalized entertainment discovery experience. Combining **Content-Based Filtering** and **Collaborative Filtering** into a seamless **Hybrid Recommendation Engine**, CineMatch empowers users to discover, rate, filter, and track movies through an interactive, enterprise-grade command-line interface.

---

## ✨ Key Features

* **🏠 Personalized Hybrid Home Feed:** Combines user rating metrics with content metadata to serve bespoke recommendations, complete with a clean category browser (Hollywood, Bollywood, Pan-Indian).
* **🔍 Advanced Multi-Criteria Search:** Instantly search across a curated 30-movie database by **Title**, **Actor**, **Director**, or **Flexible Substring Genres** (e.g., searching "thriller" catches *Crime Thriller*, *Action Thriller*, etc.).
* **😊 AI Movie Therapist:** A natural-language sentiment analyzer that detects emotional states (stress, academic pressure, motivation, happiness, boredom) from freeform text and prescribes uplifting film recommendations with empathetic reasoning.
* **⭐ Interactive Rating System:** Rate movies from 1.0 to 5.0 stars with a built-in quick ID reference list, dynamically updating user profile vectors.
* **📋 Watchlist Management:** Save and organize custom watchlists using lightweight numerical IDs.
* **📈 Trending & View-Count Tracking:** Tracks popularity metrics dynamically as users explore and inspect deep-dive movie details.
* **💾 Flat-File Persistence Backend (`PersistenceManager`):** Safely persists user accounts, ratings, and watchlists to disk (`cinematch_secure_db.txt`) so data is preserved across sessions without requiring external SQL server setups.

---

## 🛠️ Technical Stack & Architecture

* **Language:** Java (Core OOP Principles)
* **Data Structures:** `HashMap`, `HashSet`, `ArrayList`, Custom Sorting Algorithms (`Comparator`)
* **Architecture:** Modular separation of concerns (`Movie`, `User`, `RecommenderEngine`, `PersistenceManager`, `Main`)
* **Environment:** Compatible with Eclipse IDE / IntelliJ / Command Line

---

## 🚀 Getting Started & Running in Eclipse

1. **Clone the Repository:**
   ```bash
   git clone [https://github.com/shaurya19102007-hue/cinematch-java.git](https://github.com/shaurya19102007-hue/cinematch-java.git)
