# Inventory Manager

A desktop inventory application built with **Java Swing** and **CSV files**. It brings everyday stock, purchasing and sales tasks into one window, with separate workspaces for customers, inventory staff and suppliers.

This project started as a single merged Java file. It has since been split into smaller source files so the UI, data models, CSV storage and inventory rules are easier to follow, test and explain in a lab assessment.

![Inventory Manager dashboard showing the stock threshold warning](assets/dashboard-stock-alerts.png)

## What it does

### Inventory and seller workspace

- View a dashboard with product counts, units on hand, stock value, sales and recent activity.
- Add, edit, search, import and export products using CSV.
- Record sales, apply discounts and select payment methods.
- Process returns, with the option to put returned items back into stock.
- Adjust stock after a physical count.
- Review sales, purchases, refunds and estimated gross profit.

### Supplier workspace

- Maintain supplier records.
- Create purchase orders and track their status.
- Record full or partial deliveries.
- Review restock requests created by customer orders that could not be fulfilled.

### Customer workspace

- Register and sign in to a customer account.
- Browse available products and place orders.
- Follow order and restock-request status.

### Live stock warnings

The application uses each product's existing **reorder level**. A product is flagged when its available quantity is less than or equal to that level. Zero-stock items are shown as critical; other items at or below the reorder point are shown as low stock. The alert count and dashboard warning refresh after stock-changing operations, including sales, returns, deliveries, adjustments and CSV imports.

![Customer registration screen with readable input fields](assets/customer-registration.png)

## Built with

- Java 17+
- Java Swing and AWT for the desktop interface
- Java I/O and NIO for CSV persistence
- Standard Java cryptography APIs for salted password hashing
- Java2D for the small interface icons and charts

There are **no third-party dependencies**. You do not need Maven, Gradle, a database server or an internet connection to run the application.

## Run it

Install a JDK (17 or later), download or clone this repository, and open a terminal in the project directory.

### Compile and launch

```bash
javac -encoding UTF-8 Inventory.java
java Inventory
```

The Java compiler automatically compiles the other referenced source files in the same directory. Run `java Inventory` after compilation; `java Inventory.java` is Java's single-file launcher and is not the launcher for this multi-file project.

### Build a runnable JAR

**Windows**

```bat
build.bat
run.bat
```

**Linux / macOS**

```bash
chmod +x build.sh run.sh
./build.sh
./run.sh
```

The build creates `dist/InventoryManager.jar`. It can also be launched directly:

```bash
java -jar dist/InventoryManager.jar
```

## Data and CSV files

On first launch, the application creates a `data/` folder in the current working directory. The CSV files are the application's local store:

| File | Contents |
| --- | --- |
| `products.csv` | Product details, prices, quantities and reorder levels |
| `suppliers.csv` | Supplier contact details |
| `transactions.csv` | Purchases, sales, returns and stock adjustments |
| `purchase_orders.csv` | Purchase orders and delivery progress |
| `customers.csv` | Customer accounts and password hashes |
| `customer_orders.csv` | Customer orders and their status |
| `restock_requests.csv` | Requests raised when an order cannot be fulfilled |
| `role_accounts.csv` | Inventory/seller and supplier login records |

Keep the `data/` folder to retain records between launches. Back it up before manually editing or replacing CSV files. The application validates CSV headers and reports invalid rows rather than silently accepting malformed data.

## First-time staff accounts

When no staff account exists, the application creates local starter accounts:

| Workspace | Username | Initial password |
| --- | --- | --- |
| Inventory / Seller | `seller` | `Seller@123` |
| Supplier | `supplier` | `Supplier@123` |

These credentials are included for local demonstration and lab use. Change them before using the application with real business data. Customer accounts are created through the customer registration screen.

## Project layout

All production `.java` files are in the repository root and use the default package. This keeps the required compile command straightforward.

- `Inventory.java` — application entry point and startup handling.
- `Product.java`, `Supplier.java`, `PurchaseOrder.java`, `TransactionRecord.java` — domain models.
- `CsvUtil.java`, `CsvDatabase.java`, `FileReplacer.java` — CSV formatting and persistence.
- `InventoryService.java` — stock, sales, returns, purchasing and reporting rules.
- `CustomerDataStore.java`, `RoleAuthStore.java` — customer and staff account persistence.
- `MainFrame.java`, `*Panel.java`, `UiTheme.java`, `AppIcon.java` — Swing windows, screens and shared styling.
- `StockAlertBanner.java`, `StockAlertDialog.java` — reorder-level warning UI.
- `qa/` — smoke tests for the inventory workflows and GUI.
- `dist/` — prebuilt runnable JAR.

Each production Java source file is kept below 500 lines. `SOURCE_MAP.txt` and `SOURCE_LINE_COUNTS.txt` provide a quick guide to the split.

## Run the smoke tests

Compile the application and test classes:

```bash
mkdir -p build
javac -encoding UTF-8 -d build Inventory.java
javac -encoding UTF-8 -cp build -d build qa/InventorySmokeTest.java qa/GuiSmokeTest.java
```

Run the data and business-logic checks:

```bash
java -cp build InventorySmokeTest
```

Run the GUI check on a desktop session:

```bash
java -cp build GuiSmokeTest
```

On a headless Linux machine, the GUI check needs a virtual display (for example, `xvfb-run -a java -cp build GuiSmokeTest` when `xvfb-run` is installed).

## A note on scope

This is a local desktop application for learning, demonstration and small-scale workflow practice. CSV files are stored on the computer running the program; this is not a hosted multi-user SaaS service or a cloud-synchronised database. Treat the starter credentials as demo credentials and do not use real customer information without adding the access controls, backup, audit and deployment safeguards appropriate to that environment.
