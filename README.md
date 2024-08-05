# TransparentProxy

A simple transparent proxy server and graphical user interface (GUI) application implemented in Java. This project allows users to start and stop a proxy server, add and display filtered hosts, and generate reports of request logs. The GUI is built using JavaFX.

## Features

- **Proxy Server**:
  - Starts and stops a proxy server.
  - Handles different HTTP methods (`GET`, `POST`, `HEAD`, `OPTIONS`, `CONNECT`).
  - Caches responses to optimize performance.
  - Blocks requests to specified hosts.
  - Logs requests and generates reports.

- **GUI Application**:
  - Menu options to start/stop the proxy server, add/display filtered hosts, and generate reports.
  - About dialog providing information about the application and its developer.

## Requirements

- Java 11 or later
- JavaFX SDK

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/transparent-proxy.git
2. Navigate to the project directory:
   ```bash
   cd transparent-proxy
3. Build the project using Maven or your preferred build tool. If using Maven, run:
   ```bash
   mvn clean install

## Screenshots

![Ekran görüntüsü 2024-08-05 184912](https://github.com/user-attachments/assets/86154898-e559-4520-8b9a-ded62d1fadf7)


![Ekran görüntüsü 2024-08-05 184958](https://github.com/user-attachments/assets/44c37692-cd44-43e2-bf74-a431cb3468cc)



## Usage

### File Menu:
- **Start:** Starts the proxy server.
- **Stop:** Stops the proxy server.
- **Report:** Generates a report of request logs and saves it as `report.txt`.
- **Exit:** Exits the application.

### Filter Menu:
- **Add host to filter:** Opens a dialog to add a host to the filter list.
- **Display current filtered hosts:** Shows a dialog listing all currently blocked hosts.

### Help Menu:
- **About:** Displays information about the application and its developer.

## Contributing

Feel free to open issues and submit pull requests for improvements or bug fixes. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License - see the `LICENSE` file for details.

## Acknowledgements

- JavaFX for GUI development.
- Java for the underlying server and client handling.
