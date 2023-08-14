import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.*;

enum SearchType {
    TITLE,
    AUTHOR
}

class User {
    String username;
    String password;
    List<Book> wishlist = new ArrayList<>(); 

    User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    void addToWishlist(Book book) {
        wishlist.add(book);
    }
}

class Book {
    String title;
    String author;
    double price;
    int stock;
    List<Review> reviews = new ArrayList<>(); 

    Book(String title, String author, double price, int stock) {
        this.title = title;
        this.author = author;
        this.price = price;
        this.stock = stock;
    }

    double getAverageRating() {
        if (reviews.isEmpty()) {
            return 0;
        }
        double totalRating = 0;
        for (Review review : reviews) {
            totalRating += review.rating;
        }
        return totalRating / reviews.size();
    }
}

class Review {
    User user;
    int rating;
    String comment;

    Review(User user, int rating, String comment) {
        this.user = user;
        this.rating = rating;
        this.comment = comment;
    }
}

class ShoppingCart {
    List<Book> items = new ArrayList<>();
    ReentrantLock lock = new ReentrantLock();

    void addBook(Book book) {
        lock.lock();
        try {
            items.add(book);
        } finally {
            lock.unlock();
        }
    }

    double getTotalPrice() {
        lock.lock();
        try {
            double total = 0;
            for (Book book : items) {
                total += book.price;
            }
            return total;
        } finally {
            lock.unlock();
        }
    }
}

class Order {
    int orderNumber;
    User user;
    List<Book> items;
    double totalPrice;
    Date orderDate;

    Order(int orderNumber, User user, List<Book> items, double totalPrice) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.items = items;
        this.totalPrice = totalPrice;
        this.orderDate = new Date();
    }

    void generateInvoice(List<Book> purchasedItems) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Invoice for Order #" + orderNumber);
        System.out.println("Date: " + dateFormat.format(orderDate));
        System.out.println("Customer: " + user.username);
        System.out.println("Items:");
        for (Book book : purchasedItems) {
            System.out.println("- " + book.title + " by " + book.author + " - Rs " + book.price);
        }
        System.out.println("Total Price: Rs " + totalPrice);
    }
    
}

public class SimpleBookStore {
    private static final String USER_FILE = "users.txt";
    private static final String INVENTORY_FILE = "inventory.txt";
    private static List<User> users = new ArrayList<>();
    private static User currentUser = null;
    private static List<Book> catalog = new ArrayList<>();
    private static List<Order> orders = new ArrayList<>();
    private static int orderCounter = 1;
    private static ReentrantLock catalogLock = new ReentrantLock();
    private static ReentrantLock inventoryLock = new ReentrantLock();

    public static void main(String[] args) {
        loadUsersFromFile();
        loadInventory();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Welcome to the Book Store!");

            if (currentUser == null) {
                System.out.println("1. Login");
                System.out.println("2. Sign Up");
                System.out.println("3. Exit");
                int choice = scanner.nextInt();
                scanner.nextLine(); 

                switch (choice) {
                    case 1:
                        login(scanner);
                        break;
                    case 2:
                        signUp(scanner);
                        break;
                    case 3:
                        System.out.println("Goodbye!");
                        saveInventory();
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } else {
                showBookStore(scanner);
            }
        }
    }

    private static void showBookStore(Scanner scanner) {
        ShoppingCart cart = new ShoppingCart();

        while (true) {
            System.out.println("Select an option:");
            System.out.println("1. Browse Book Catalog");
            System.out.println("2. Search for Books");
            System.out.println("3. View Cart");
            System.out.println("4. Process Order");
            System.out.println("5. View Wishlist");
            System.out.println("6. Rate and Review a Book");
            System.out.println("7. Log Out");
            System.out.println("8. Exit");
            int choice = scanner.nextInt();
            scanner.nextLine(); 

            switch (choice) {
                case 1:
                    browseBookCatalog(scanner, cart);
                    break;
                case 2:
                    searchBooks(scanner);
                    break;
                case 3:
                    viewCart(cart);
                    break;
                case 4:
                    processOrder(scanner, cart);
                    break;
                case 5:
                    viewWishlist(currentUser);
                    break;
                case 6:
                    rateAndReviewBook(scanner);
                    break;
                case 7:
                    logOut();
                    return;
                case 8:
                    System.out.println("Goodbye!");
                    saveInventory();
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void rateAndReviewBook(Scanner scanner) {
        System.out.println("Enter the title of the book you want to rate and review:");
        String title = scanner.nextLine();

        catalogLock.lock();
        try {
            Book book = findBookByTitle(title);
            if (book == null) {
                System.out.println("Book not found.");
                return;
            }

            if (book.stock <= 0) {
                System.out.println("Book is out of stock. You cannot rate and review it at the moment.");
                return;
            }

            System.out.println("Enter your rating (1-5):");
            int rating = scanner.nextInt();
            scanner.nextLine(); 

            if (rating < 1 || rating > 5) {
                System.out.println("Invalid rating. Please enter a rating between 1 and 5.");
                return;
            }

            System.out.println("Enter your review:");
            String reviewText = scanner.nextLine();

            Review review = new Review(currentUser, rating, reviewText);
            book.reviews.add(review);
            System.out.println("Thank you for your review!");
        } finally {
            catalogLock.unlock();
        }
    }

    private static Book findBookByTitle(String title) {
        for (Book book : catalog) {
            if (book.title.equalsIgnoreCase(title)) {
                return book;
            }
        }
        return null;
    }

    private static void viewWishlist(User user) {
        System.out.println("Wishlist for " + user.username + ":");
        for (Book book : user.wishlist) {
            System.out.println("- " + book.title + " by " + book.author + " - Rs " + book.price);
        }
    }

    private static void addToWishlist(User user, Book book) {
        user.addToWishlist(book);
        System.out.println(book.title + " has been added to your Wishlist.");
    }

    private static void browseBookCatalog(Scanner scanner, ShoppingCart cart) {
        catalogLock.lock();
        try {
            System.out.println("Book Catalog:");
            for (int i = 0; i < catalog.size(); i++) {
                Book book = catalog.get(i);
                System.out.println((i + 1) + ". " + book.title + " by " + book.author
                        + " - Rs " + book.price + " (Stock: " + book.stock + ")");
            }
        } finally {
            catalogLock.unlock();
        }

        System.out.println("Enter the number of the book to add to cart (0 to go back):");
        int choice = scanner.nextInt();
        scanner.nextLine(); 

        if (choice > 0 && choice <= catalog.size()) {
            catalogLock.lock();
            try {
                Book selectedBook = catalog.get(choice - 1);
                if (selectedBook.stock > 0) {
                    cart.addBook(selectedBook);
                    selectedBook.stock--;
                    System.out.println("Added to cart: " + selectedBook.title);
                    System.out.println("Do you want to add it to your Wishlist? (y/n): ");
                    String addToWishlist = scanner.nextLine();
                    if (addToWishlist.equalsIgnoreCase("y")) {
                        addToWishlist(currentUser, selectedBook);
                    }
                } else {
                    System.out.println("Selected book is out of stock.");
                }
            } finally {
                catalogLock.unlock();
            }
        } else if (choice == 0) {
            return;
        } else {
            System.out.println("Invalid choice. Please try again.");
        }
    }
    private static void searchBooks(Scanner scanner) {
        System.out.println("Search by:");
        System.out.println("1. Title");
        System.out.println("2. Author");
        int searchChoice = scanner.nextInt();
        scanner.nextLine();
        switch (searchChoice) {
            case 1:
                System.out.println("Enter the title:");
                String title = scanner.nextLine();
                performSearch(SearchType.TITLE, title);
                break;
            case 2:
                System.out.println("Enter the author:");
                String author = scanner.nextLine();
                performSearch(SearchType.AUTHOR, author);
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void performSearch(SearchType type, String keyword) {
        catalogLock.lock();
        try {
            List<Book> results = new ArrayList<>();
            for (Book book : catalog) {
                String field = (type == SearchType.TITLE) ? book.title : book.author;
                if (field.toLowerCase().contains(keyword.toLowerCase())) {
                    results.add(book);
                }
            }

            if (results.isEmpty()) {
                System.out.println("No books found.");
            } else {
                System.out.println("Search results:");
                for (int i = 0; i < results.size(); i++) {
                    Book book = results.get(i);
                    System.out.println((i + 1) + ". " + book.title + " by " + book.author
                            + " - Rs " + book.price + " (Stock: " + book.stock + ")");
                }
            }
        } finally {
            catalogLock.unlock();
        }
    }

    private static void viewCart(ShoppingCart cart) {
        System.out.println("Shopping Cart:");
        cart.lock.lock();
        try {
            for (Book book : cart.items) {
                System.out.println("- " + book.title + " by " + book.author
                        + " - Rs " + book.price);
            }
            System.out.println("Total Price: Rs " + cart.getTotalPrice());
        } finally {
            cart.lock.unlock();
        }
    }

    private static void processOrder(Scanner scanner, ShoppingCart cart) {
        if (cart.items.isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }
    
        double totalPrice = cart.getTotalPrice();
        if (currentUser != null) {
            System.out.println("Total Price: Rs " + totalPrice);
            System.out.println("Confirm order? (y/n): ");
            String confirm = scanner.nextLine();
            if (confirm.equalsIgnoreCase("y")) {
                User user = currentUser;
                Order order = new Order(orderCounter++, user, cart.items, totalPrice);
                orders.add(order);
    
                for (Book book : cart.items) {
                    book.stock--;
                    if (!hasPurchasedBook(user, book)) {
                        user.addToWishlist(book); 
                    }
                }
    
                System.out.println("Order placed successfully!");
                order.generateInvoice(cart.items);
                cart.items.clear();
                updateInventory();
            } else {
                System.out.println("Order cancelled.");
            }
        } else {
            System.out.println("You need to log in or sign up to place an order.");
        }
    }
    

    private static boolean hasPurchasedBook(User user, Book book) {
        for (Order order : orders) {
            if (order.user == user && order.items.contains(book)) {
                return true;
            }
        }
        return false;
    }

    private static void logOut() {
        currentUser = null;
        System.out.println("Logged out successfully.");
    }

    private static void loadUsersFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String username = parts[0];
                    String password = parts[1];
                    users.add(new User(username, password));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading user data.");
        }
    }

    private static void saveUsersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_FILE))) {
            for (User user : users) {
                writer.write(user.username + "," + user.password);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving user data.");
        }
    }

    private static void login(Scanner scanner) {
        System.out.println("Enter your username: ");
        String username = scanner.nextLine().trim();

        System.out.println("Enter your password: ");
        String password = scanner.nextLine().trim();

        for (User user : users) {
            if (user.username.equals(username) && user.password.equals(password)) {
                currentUser = user;
                System.out.println("Login successful");
                return;
            }
        }

        System.out.println("Invalid login credentials.");
    }

    private static void signUp(Scanner scanner) {
        System.out.println("Enter a new username: ");
        String newUsername = scanner.nextLine();

        for (User user : users) {
            if (user.username.equals(newUsername)) {
                System.out.println("Username already exists. Please choose a different username.");
                return;
            }
        }

        System.out.println("Enter a password (at least 6 characters): ");
        String newPassword = scanner.nextLine();

        if (newPassword.length() < 6) {
            System.out.println("Password must be at least 6 characters long.");
            return;
        }

        User newUser = new User(newUsername, newPassword);
        users.add(newUser);
        saveUsersToFile();
        System.out.println("Account created. You can now log in.");
    }

    private static void loadInventory() {
        try (BufferedReader reader = new BufferedReader(new FileReader(INVENTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String title = parts[0];
                    String author = parts[1];
                    double price = Double.parseDouble(parts[2]);
                    int stock = Integer.parseInt(parts[3]);
                    catalog.add(new Book(title, author, price, stock));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading inventory.");
        }
    }

    private static void updateInventory() {
        inventoryLock.lock();
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(INVENTORY_FILE))) {
                for (Book book : catalog) {
                    writer.write(book.title + "," + book.author + "," + book.price + "," + book.stock);
                    writer.newLine();
                }
            } catch (IOException e) {
                System.out.println("Error updating inventory.");
            }
        } finally {
            inventoryLock.unlock();
        }
    }

    public static void saveInventory() {
        inventoryLock.lock();
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(INVENTORY_FILE))) {
                for (Book book : catalog) {
                    writer.write(book.title + "," + book.author + "," + book.price + "," + book.stock);
                    writer.newLine();
                }
            } catch (IOException e) {
                System.out.println("Error saving inventory data.");
            }
        } finally {
            inventoryLock.unlock();
        }
    }
}
