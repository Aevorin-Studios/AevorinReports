# AevorinReports
<p align="center">
  <img src="https://i.postimg.cc/9RR2yP4Z/aevorin-reports-title.png" alt="AevorinReports Banner" width="200"/>
</p>

*A strong and flexible reporting system for Minecraft servers that is easy to use and great for serious moderation.*

---

## Features

- **Interactive Admin GUI** — Manage and update reports effortlessly with an intuitive click-based interface.
- **Player Report History** — Players can view the status of their own reports with `/reports`.
- **MiniMessage Support** — create beautiful, colorful messages (gradients, hover effects) with full MiniMessage support.
- **Multiple GUI Types** — Choose between a classic **Book GUI** or a modern **Container (Chest) GUI**.
- **Real-Time Staff Alerts** — Staff are instantly notified when a report is filed.
- **Status Tracking** — Reports go through a clear status flow: **Pending** → **Resolved** / **Rejected**.
- **Custom Reasons** — Players can submit custom reasons via chat if the predefined categories don't fit.
- **bStats Integration** — Anonymous usage metrics to help improve the plugin.
- **Database Support** — Supports both **SQLite** (local file) and **MySQL** for cross-server synchronization.
- **Optimized Performance** — Async processing and smart caching ensure smooth gameplay.

---

## Commands

### User Commands
- `/report <player> [reason]` — File a report. If no reason is provided, a GUI opens.
  - *Example:* `/report Notch hacking` or just `/report Notch`
- `/reports` — View your submitted reports and their status.

### Staff Commands
- `/reports` — Open the Admin Report Management GUI (requires permission).
- `/viewreport <id>` — View detailed information about a specific report.
- `/setreportstatus <id> to <status>` — Manually update a report's status (PENDING, RESOLVED, REJECTED).
- `/ar reload` — Reload the configuration file.

---

## Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `aevorinreports.report` | Allows players to file reports | `true` |
| `aevorinreports.manage` | Allows staff to manage reports (Admin GUI, set status) | `op` |
| `aevorinreports.notify` | Receive notifications when a report is filed | `op` |
| `aevorinreports.reload` | Allows reloading the plugin config | `op` |

---

## Book GUI Preview

<p align="center">
  <img src="https://i.postimg.cc/Gh4mVFNK/aevorinreports123.png" alt="AevorinReports GUI" width="500"/>
</p>

---

## Support & Feedback

Found a bug? Have suggestions?  
Please contact **[Aevorin Studio](https://discord.gg/r2VbJEb8Pj)** — your feedback helps shape the future of AevorinReports.

---

## License

**AevorinReports** is licensed under **[MIT License](https://github.com/Aevorin-Studios/AevorinReports/?tab=MIT-1-ov-file)**.  
You are free to use, modify, distribute, and sublicense this software, provided that the original copyright and license notice are included in all copies.

---

**Made with ❤️ by Aevorin Studios**
