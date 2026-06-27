# GhanaRide Spring Boot Project Explanation

Welcome to the beginner's guide to your GhanaRide application! Spring Boot projects can look overwhelming because they contain many files, but they follow a strict, logical pattern called **MVC** (Model-View-Controller) plus Services and Repositories.

Think of your app like a restaurant:
* **Database/Entities** = The pantry where ingredients are stored.
* **Repositories** = The pantry manager who fetches ingredients.
* **Services** = The chef who prepares the food (business logic).
* **Controllers** = The waiters who take orders from customers and bring the food out.
* **Views (HTML)** = The tables and menus the customers interact with.

Let's break down all the files we created into these categories!

---

## 1. The Starting Point
These files tell the app how to launch and what it needs to run.

### `pom.xml`
This is your "shopping list" for dependencies. When you write Java, you don't want to reinvent the wheel. `pom.xml` tells a tool called Maven: *"Hey, go download Spring Web, Spring Security, MySQL Connector, and Thymeleaf for me."* 

### `GhanaRideApplication.java`
This is the ignition switch. It has the standard `public static void main(String[] args)` method. When you run this file, Spring Boot wakes up, scans all your other files, connects to the database, and starts a local web server on port `8088`.

### `application.properties`
This is your settings file. It contains the URL for your MySQL database, your database username/password, the server port (`8088`), and the setting that tells the app where to save uploaded car images (`uploads/cars/`).

---

## 2. The Database Layer (Entities & Enums)
*Location: `src/main/java/com/ghanaride/entity`*

Entities are Java blueprints that turn directly into MySQL database tables.

### `User.java`, `Trip.java`, `Car.java`, `Booking.java`
Look inside these files and you'll see annotations like `@Entity` and `@Table`. 
* When Spring Boot sees `User.java` with a string variable called `email`, it goes to MySQL and creates a column named `email`.
* **Foreign Keys**: You'll see `@ManyToOne` annotations. For example, in `Booking.java`, there is a `@ManyToOne` link to a `User`. This tells the database: *"Every booking belongs to exactly one user."*

### Enums (`Role.java`, `TripStatus.java`, etc.)
Enums are fixed lists of options. Instead of letting a user type any random role (which could cause typos), the `Role.java` enum restricts roles to exactly `USER`, `DRIVER`, or `ADMIN`.

---

## 3. The Data Access Layer (Repositories)
*Location: `src/main/java/com/ghanaride/repository`*

### `UserRepository.java`, `TripRepository.java`, etc.
Notice how these are `interfaces` (empty contracts) and not normal classes. They extend `JpaRepository`.
* **What it does:** It gives you free database commands without writing SQL! Because you extended JpaRepository, Spring automatically writes the code to `.save()`, `.delete()`, or `.findAll()` users.
* **Custom methods:** When we write `Optional<User> findByUsername(String username);`, Spring is smart enough to read the method name and automatically generate the SQL query behind the scenes.

---

## 4. The Brain / Business Logic (Services)
*Location: `src/main/java/com/ghanaride/service`*

Services are where the actual "work" gets done. 

### `BookingService.java`
This handles the logic of booking a trip. It doesn't just save a booking to the database. First, it checks if there are seats available. If yes, it creates a unique `GR-XXXX` booking reference, decreases the available seats by 1, and saves the new data.

### `UserService.java`
This manages users. When someone registers, it takes their password, encrypts it securely using `BCrypt`, and then asks the `UserRepository` to save them.

### `FileStorageService.java`
When a driver uploads a picture of their car, this service takes the file, generates a random unique name for it, and saves it to a local folder so it can be viewed later.

---

## 5. The Traffic Cops (Controllers)
*Location: `src/main/java/com/ghanaride/controller`*

Controllers receive the HTTP requests (when you click a link or submit a form) and decide what page to show you next.

### `AuthController.java`
* If a user goes to your website `/login`, this controller catches the request and says, *"Okay, show them the `login.html` file."*
* If they submit the registration form, it catches the data, sends it to the `UserService` to be saved, and redirects them to the login page.

### `DriverController.java`
All methods here start with `/driver`. If a user goes to `/driver/dashboard`, it fetches the currently logged-in driver, asks the `TripService` for all trips belonging to that driver, and passes that data to the dashboard HTML template.

---

## 6. Security and Configuration
*Location: `src/main/java/com/ghanaride/config`*

### `SecurityConfig.java`
This is your bouncer. It looks at every incoming user and checks their ID.
* It says the `/login` and `/register` pages are `permitAll()` (anyone can view them).
* It says anything starting with `/admin/**` requires the `ADMIN` role. If a normal user tries to go there, they get blocked.

### `CustomAuthenticationSuccessHandler.java`
After you type your password and hit "Login", Spring needs to know where to send you. This file checks your role: if you are a driver, it forces your browser to redirect to `/driver/dashboard`. If you are a user, it sends you to `/dashboard`.

---

## 7. The User Interface (Views / HTML)
*Location: `src/main/resources/templates`*

These are your frontend files built with **Thymeleaf**. Thymeleaf allows you to inject Java data directly into HTML.

### `layout.html`
Instead of copying and pasting the Navigation Bar into 10 different files, we put it in `layout.html`. Every other page basically says: *"Use layout.html, but just swap out the middle part with my specific content."*

### `dashboard.html` (Thymeleaf in action)
You will see code like `th:each="trip : ${trips}"`. This is a loop. It tells the HTML: *"For every trip in the database, duplicate this HTML card and fill it with the trip's destination and price."*

### `style.css` (in `static/css`)
This makes everything look pretty! We defined the Ghana national colors (Red, Gold, Green) as variables and applied them to buttons, badges, and the navigation bar to give it a premium, branded look.
