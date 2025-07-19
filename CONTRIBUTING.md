# 🧩 Contributing to AevorinReports

Thank you for your interest in contributing to **AevorinReports** — a powerful and flexible reporting system for Minecraft servers developed by Aevorin Studios.

We welcome contributions that improve performance, features, and user experience for both staff and players. Please follow the guidelines below to ensure your contributions align with the project’s direction.

---

## 📐 Code Style & Guidelines

- Use **Java 17+** (preferred Java 21 for 1.21.8+ support)
- Follow **standard Spigot/Paper plugin development practices**
- Keep classes **modular** and **readable**
- Avoid **hardcoded values/messages** — use `config.yml` or `lang.yml`
- Always use **async** operations for database or heavy tasks

---

## 🛠️ What Can You Contribute?

### ✅ Good Contribution Areas
- New features (e.g., cooldown system, category editor)
- Bug fixes and edge case handling
- Storage support improvements (MySQL, YAML, etc.)
- GUI enhancements
- Performance optimization
- Translations (multi-language support)

### 🚫 Do Not Submit
- Obfuscated, minified, or copy-pasted code
- Unlicensed use of third-party code
- Plugins that violate Minecraft EULA or Terms of Service

---

## 🗂️ Project Structure Overview

- `dev.aevorinstudios.aevorinReports` — Main package
- `commands` — Handles player and admin commands like `/report` or `/reports`
- `gui` — In-game book/GUI logic for report interface
- `storage` — File or MySQL support (upcoming)
- `model` — Report object structure and statuses
- `listener` — Hooks for reporting via events or chat

---

## 🚀 Getting Started

### 1. Fork the Repository
```bash
git clone https://github.com/Aevorin-Studios/AevorinReports.git
