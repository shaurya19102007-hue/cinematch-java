package movie;

import java.io.*;
import java.util.*;

// ==========================================
// 1. DATA MODELS
// ==========================================
class Movie {
    private int id;
    private String title;
    private String poster;
    private String genre;
    private int releaseYear;
    private double rating; // 5.0 Scale
    private String cast;
    private String director;
    private String description;
    private String mood; 
    private int viewCount;

    public Movie(int id, String title, String poster, String genre, int releaseYear, double rating, String cast, String director, String description, String mood) {
        this.id = id;
        this.title = title;
        this.poster = poster;
        this.genre = genre;
        this.releaseYear = releaseYear;
        this.rating = rating;
        this.cast = cast;
        this.director = director;
        this.description = description;
        this.mood = mood;
        this.viewCount = 0;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getPoster() { return poster; }
    public String getGenre() { return genre; }
    public int getReleaseYear() { return releaseYear; }
    public double getRating() { return rating; }
    public String getCast() { return cast; }
    public String getDirector() { return director; }
    public String getDescription() { return description; }
    public String getMood() { return mood; }
    public int getViewCount() { return viewCount; }
    public void incrementViewCount() { this.viewCount++; }

    @Override
    public String toString() {
        return String.format("[%d] %s (%d) - ⭐ %.1f / 5.0 | Genre: %s | Dir: %s\n     Cast: %s\n     Plot: %s", 
                id, title, releaseYear, rating, genre, director, cast, description);
    }
}

class User {
    private String username;
    private String password;
    private Set<Integer> watchlist; // ID-based watchlist
    private Map<Integer, Double> ratings; // MovieID -> Stars (1-5)
    
    // Personalized Profile Tracking
    private String favoriteGenre;
    private String favoriteActor;
    private String favoriteDirector;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.watchlist = new HashSet<>();
        this.ratings = new HashMap<>();
        this.favoriteGenre = "None yet";
        this.favoriteActor = "None yet";
        this.favoriteDirector = "None yet";
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Set<Integer> getWatchlist() { return watchlist; }
    public Map<Integer, Double> getRatings() { return ratings; }
    
    public String getFavoriteGenre() { return favoriteGenre; }
    public String getFavoriteActor() { return favoriteActor; }
    public String getFavoriteDirector() { return favoriteDirector; }

    public void updateProfileMetrics(List<Movie> allMovies) {
        if (ratings.isEmpty()) return;

        Map<String, Integer> genreCount = new HashMap<>();
        Map<String, Integer> actorCount = new HashMap<>();
        Map<String, Integer> directorCount = new HashMap<>();

        for (Map.Entry<Integer, Double> entry : ratings.entrySet()) {
            if (entry.getValue() >= 4.0) { // Only count movies liked (>= 4 stars)
                Movie m = findMovieById(allMovies, entry.getKey());
                if (m != null) {
                    genreCount.put(m.getGenre(), genreCount.getOrDefault(m.getGenre(), 0) + 1);
                    for (String actor : m.getCast().split(",")) {
                        String cleanActor = actor.trim();
                        actorCount.put(cleanActor, actorCount.getOrDefault(cleanActor, 0) + 1);
                    }
                    directorCount.put(m.getDirector(), directorCount.getOrDefault(m.getDirector(), 0) + 1);
                }
            }
        }

        this.favoriteGenre = getTopKey(genreCount, "General");
        this.favoriteActor = getTopKey(actorCount, "Various");
        this.favoriteDirector = getTopKey(directorCount, "Various");
    }

    private Movie findMovieById(List<Movie> list, int id) {
        for (Movie m : list) {
            if (m.getId() == id) return m;
        }
        return null;
    }

    private String getTopKey(Map<String, Integer> map, String defaultVal) {
        String top = defaultVal;
        int max = -1;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                top = entry.getKey();
            }
        }
        return top;
    }
}

// ==========================================
// 2. FIXED FILE PERSISTENCE MANAGER
// ==========================================
class PersistenceManager {
    private static final String FILE_NAME = "cinematch_secure_db.txt";

    public static void saveUsers(Map<String, User> userMap) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (User u : userMap.values()) {
                writer.println("USER:" + u.getUsername() + ":" + u.getPassword());
                for (int wId : u.getWatchlist()) {
                    writer.println("WATCHLIST:" + u.getUsername() + ":" + wId);
                }
                for (Map.Entry<Integer, Double> entry : u.getRatings().entrySet()) {
                    writer.println("RATING:" + u.getUsername() + ":" + entry.getKey() + ":" + entry.getValue());
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error saving data to disk: " + e.getMessage());
        }
    }

    public static Map<String, User> loadUsers() {
        Map<String, User> userMap = new HashMap<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return userMap;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            User activeUser = null;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 3);
                if (parts.length < 2) continue;
                
                String type = parts[0];
                String username = parts[1];

                if (type.equals("USER") && parts.length >= 3) {
                    String password = parts[2];
                    activeUser = new User(username, password);
                    userMap.put(username, activeUser);
                } else if (activeUser != null) {
                    if (type.equals("WATCHLIST") && parts.length >= 3) {
                        activeUser.getWatchlist().add(Integer.parseInt(parts[2]));
                    } else if (type.equals("RATING")) {
                        String[] rateParts = line.split(":");
                        if (rateParts.length >= 4) {
                            activeUser.getRatings().put(Integer.parseInt(rateParts[2]), Double.parseDouble(rateParts[3]));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Could not load data from disk. Starting fresh.");
        }
        return userMap;
    }
}

// ==========================================
// 3. 30-MOVIE DATABASE LOADER
// ==========================================
class MovieDatabaseLoader {
    public static List<Movie> loadFullDatabase() {
        List<Movie> db = new ArrayList<>();
        
        // Hollywood Blockbusters
        db.add(new Movie(1, "Interstellar", "p1.jpg", "Sci-Fi Drama", 2014, 4.9, "Matthew McConaughey, Anne Hathaway", "Christopher Nolan", "A team of explorers travel through a wormhole in space to ensure humanity's survival.", "Motivational"));
        db.add(new Movie(2, "Inception", "p2.jpg", "Sci-Fi Action", 2010, 4.8, "Leonardo DiCaprio, Elliot Page", "Christopher Nolan", "A thief who steals corporate secrets through dream-sharing technology.", "Excited"));
        db.add(new Movie(3, "The Dark Knight", "p3.jpg", "Action Crime", 2008, 4.9, "Christian Bale, Heath Ledger", "Christopher Nolan", "When the menace known as the Joker wreaks havoc and chaos on Gotham.", "Excited"));
        db.add(new Movie(4, "Avengers: Endgame", "p4.jpg", "Action Sci-Fi", 2019, 4.7, "Robert Downey Jr., Chris Evans", "Anthony and Joe Russo", "After devastating events, the Avengers assemble once more to reverse Thanos' actions.", "Excited"));
        db.add(new Movie(5, "The Shawshank Redemption", "p5.jpg", "Drama", 1994, 5.0, "Tim Robbins, Morgan Freeman", "Frank Darabont", "Two imprisoned men bond over a number of years, finding solace and eventual redemption.", "Motivational"));
        db.add(new Movie(6, "Gladiator", "p6.jpg", "Action Epic", 2000, 4.6, "Russell Crowe, Joaquin Phoenix", "Ridley Scott", "A former Roman general sets out to exact vengeance against the corrupt emperor.", "Excited"));
        db.add(new Movie(7, "The Martian", "p7.jpg", "Sci-Fi Adventure", 2015, 4.5, "Matt Damon, Jessica Chastain", "Ridley Scott", "An astronaut is mistakenly presumed dead and left behind on Mars by his team.", "Motivational"));
        db.add(new Movie(8, "Spider-Man: No Way Home", "p8.jpg", "Action Sci-Fi", 2021, 4.6, "Tom Holland, Zendaya", "Jon Watts", "With Spider-Man's identity now revealed, Peter asks Doctor Strange for help.", "Excited"));
        db.add(new Movie(9, "Oppenheimer", "p9.jpg", "Biography Drama", 2023, 4.8, "Cillian Murphy, Emily Blunt", "Christopher Nolan", "The story of American scientist J. Robert Oppenheimer and the atomic bomb.", "Motivational"));
        db.add(new Movie(10, "Top Gun: Maverick", "p10.jpg", "Action Drama", 2022, 4.6, "Tom Cruise, Miles Teller", "Joseph Kosinski", "After thirty years, Maverick is still pushing the envelope as a top naval aviator.", "Excited"));

        // Bollywood Masterpieces
        db.add(new Movie(11, "3 Idiots", "p11.jpg", "Comedy Drama", 2009, 4.8, "Aamir Khan, Madhavan, Sharman Joshi", "Rajkumar Hirani", "Two friends search for their long lost college companion who inspired them.", "Happy"));
        db.add(new Movie(12, "Dangal", "p12.jpg", "Sports Drama", 2016, 4.8, "Aamir Khan, Fatima Sana Shaikh", "Nitesh Tiwari", "A former national wrestling champion trains his daughters to become world-class wrestlers.", "Motivational"));
        db.add(new Movie(13, "Zindagi Na Milegi Dobara", "p13.jpg", "Adventure Comedy", 2011, 4.6, "Hrithik Roshan, Farhan Akhtar, Abhay Deol", "Zoya Akhtar", "Three friends vacation in Spain before one of their weddings and transform their lives.", "Happy"));
        db.add(new Movie(14, "Andhadhun", "p14.jpg", "Crime Thriller", 2018, 4.6, "Ayushmann Khurrana, Tabu", "Sriram Raghavan", "A series of mysterious events changes the life of a blind piano player.", "Excited"));
        db.add(new Movie(15, "PK", "p15.jpg", "Comedy Sci-Fi", 2014, 4.5, "Aamir Khan, Anushka Sharma", "Rajkumar Hirani", "A stranger lands on Earth and asks innocent, childlike questions that challenge dogma.", "Happy"));
        db.add(new Movie(16, "Shershaah", "p16.jpg", "Action Biography", 2021, 4.7, "Sidharth Malhotra, Kiara Advani", "Vishnuvardhan", "The life of PVC awardee Indian soldier Captain Vikram Batra.", "Motivational"));
        db.add(new Movie(17, "Drishyam", "p17.jpg", "Crime Thriller", 2015, 4.6, "Mohanlal / Ajay Devgn", "Jeethu Joseph", "A man goes to extreme lengths to protect his family after a tragic accident.", "Excited"));
        db.add(new Movie(18, "Chhichhore", "p18.jpg", "Comedy Drama", 2019, 4.7, "Sushant Singh Rajput, Shraddha Kapoor", "Nitesh Tiwari", "A tragic incident forces a middle-aged man to take a trip down memory lane.", "Happy"));
        db.add(new Movie(19, "Lagaan", "p19.jpg", "Drama Sport", 2001, 4.5, "Aamir Khan, Gracy Singh", "Ashutosh Gowariker", "Villagers in Victorian India wager their heavy taxes on a game of cricket against British officers.", "Motivational"));
        db.add(new Movie(20, "Queen", "p20.jpg", "Comedy Drama", 2013, 4.5, "Kangana Ranaut, Rajkummar Rao", "Vikas Bahl", "A Delhi girl sets out on her honeymoon alone after her fiancé calls off the wedding.", "Happy"));

        // Pan-Indian Epics
        db.add(new Movie(21, "Baahubali: The Beginning", "p21.jpg", "Epic Action", 2015, 4.4, "Prabhas, Rana Daggubati", "S.S. Rajamouli", "In ancient India, an adventurous and daring man becomes involved in a royal feud.", "Excited"));
        db.add(new Movie(22, "Baahubali 2: The Conclusion", "p22.jpg", "Epic Action", 2017, 4.6, "Prabhas, Anushka Shetty", "S.S. Rajamouli", "When Shiva learns about his heritage, he begins to search for answers about his family.", "Excited"));
        db.add(new Movie(23, "RRR", "p23.jpg", "Action Drama", 2022, 4.5, "N.T. Rama Rao Jr., Ram Charan", "S.S. Rajamouli", "A fictional story about two legendary revolutionaries and their journey away from home.", "Excited"));
        db.add(new Movie(24, "Pushpa: The Rise", "p24.jpg", "Action Crime", 2021, 4.3, "Allu Arjun, Rashmika Mandanna", "Sukumar", "Violence erupts in the red sandalwood smuggling syndicate of Seshachalam hills.", "Excited"));
        db.add(new Movie(25, "Eega", "p25.jpg", "Fantasy Action", 2012, 4.3, "Nani, Samantha Ruth Prabhu", "S.S. Rajamouli", "A murdered man is reincarnated as a housefly to avenge his death against his killer.", "Happy"));
        db.add(new Movie(26, "Jersey", "p26.jpg", "Sports Drama", 2019, 4.7, "Nani / Shahid Kapoor", "Gowtam Tinnanuri", "A failed cricketer decides to revive his career in his late thirties for his son.", "Motivational"));
        db.add(new Movie(27, "Kantara", "p27.jpg", "Action Thriller", 2022, 4.6, "Rishab Shetty, Sapthami Gowda", "Rishab Shetty", "When greed clouds a local forest officer's judgement, a fearless villager clashes with him.", "Excited"));
        db.add(new Movie(28, "Salaar", "p28.jpg", "Action Crime", 2023, 4.1, "Prabhas, Prithviraj Sukumaran", "Prashanth Neel", "A gang leader tries to make a promise to a dying friend and takes on other criminal gangs.", "Excited"));
        db.add(new Movie(29, "Arjun Reddy", "p29.jpg", "Romance Drama", 2017, 4.5, "Vijay Deverakonda, Shalini Pandey", "Sandeep Reddy Vanga", "A fiery young surgeon spirals into self-destruction after his girlfriend is forced to marry another.", "Romantic"));
        db.add(new Movie(30, "Maharshi", "p30.jpg", "Action Drama", 2019, 4.1, "Mahesh Babu, Allari Naresh", "Vamshi Paidipally", "A billionaire software businessman returns to India to help reform his distressed rural friend.", "Motivational"));

        return db;
    }
}

// ==========================================
// 4. RECOMMENDATION ENGINE (HYBRID & CONTENT + THERAPIST)
// ==========================================
class RecommenderEngine {
    private List<Movie> movies;

    public RecommenderEngine(List<Movie> movies) {
        this.movies = movies;
    }

    public List<Movie> searchByTitle(String query) {
        List<Movie> results = new ArrayList<>();
        for (Movie m : movies) {
            if (m.getTitle().toLowerCase().contains(query.toLowerCase())) results.add(m);
        }
        results.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return results;
    }

    public List<Movie> searchByActor(String actor) {
        List<Movie> results = new ArrayList<>();
        for (Movie m : movies) {
            if (m.getCast().toLowerCase().contains(actor.toLowerCase())) results.add(m);
        }
        results.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return results;
    }

    public List<Movie> searchByGenre(String genre) {
        List<Movie> results = new ArrayList<>();
        for (Movie m : movies) {
            if (m.getGenre().toLowerCase().contains(genre.toLowerCase())) {
                results.add(m);
            }
        }
        results.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return results;
    }

    public List<Movie> searchByDirector(String director) {
        List<Movie> results = new ArrayList<>();
        for (Movie m : movies) {
            if (m.getDirector().toLowerCase().contains(director.toLowerCase())) results.add(m);
        }
        results.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return results;
    }

    public List<Movie> getMoodRecommendations(String mood) {
        List<Movie> results = new ArrayList<>();
        for (Movie m : movies) {
            if (m.getMood().equalsIgnoreCase(mood)) results.add(m);
        }
        results.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return results;
    }

    public List<Movie> getHighestRated(int topN) {
        List<Movie> sorted = new ArrayList<>(movies);
        sorted.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return sorted.subList(0, Math.min(topN, sorted.size()));
    }

    public List<Movie> getTopViewed(int topN) {
        List<Movie> sorted = new ArrayList<>(movies);
        sorted.sort((a, b) -> Integer.compare(b.getViewCount(), a.getViewCount()));
        return sorted.subList(0, Math.min(topN, sorted.size()));
    }

    // --- 1. Content-Based Recommendations ---
    public List<Movie> getContentBasedRecommendations(User user) {
        List<Movie> recommendations = new ArrayList<>();
        if (user.getRatings().isEmpty()) return recommendations; // Cold start safeguard

        for (Movie m : movies) {
            // Do not recommend movies the user already rated
            if (user.getRatings().containsKey(m.getId())) continue;

            if (m.getGenre().equalsIgnoreCase(user.getFavoriteGenre()) || 
                m.getDirector().equalsIgnoreCase(user.getFavoriteDirector())) {
                recommendations.add(m);
            }
        }
        recommendations.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return recommendations;
    }

    // --- 2. Hybrid Recommendation System (Collaborative + Content-Based) ---
    public List<Movie> getHybridRecommendations(User currentUser, Map<String, User> allUsers) {
        // Handle Cold Start: If user hasn't rated anything, fallback to Trending Top Rated movies
        if (currentUser.getRatings().isEmpty()) {
            return getHighestRated(3);
        }

        Set<Movie> hybridSet = new LinkedHashSet<>();
        
        // Add Content-Based recommendations first
        hybridSet.addAll(getContentBasedRecommendations(currentUser));
        
        // Add Collaborative recommendations via Cosine Similarity
        hybridSet.addAll(getCollaborativeRecommendations(currentUser, allUsers));

        List<Movie> finalRecs = new ArrayList<>(hybridSet);
        finalRecs.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return finalRecs;
    }

    private List<Movie> getCollaborativeRecommendations(User currentUser, Map<String, User> allUsers) {
        String bestMatchUser = null;
        double maxSimilarity = -1.0;

        for (Map.Entry<String, User> entry : allUsers.entrySet()) {
            String otherName = entry.getKey();
            if (otherName.equals(currentUser.getUsername())) continue;
            User otherUser = entry.getValue();

            double similarity = computeCosineSimilarity(currentUser.getRatings(), otherUser.getRatings());
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestMatchUser = otherName;
            }
        }

        List<Movie> collabRecs = new ArrayList<>();
        if (bestMatchUser != null && maxSimilarity > 0.0) {
            User peer = allUsers.get(bestMatchUser);
            for (Map.Entry<Integer, Double> entry : peer.getRatings().entrySet()) {
                int mId = entry.getKey();
                if (entry.getValue() >= 4.0 && !currentUser.getRatings().containsKey(mId)) {
                    for (Movie m : movies) {
                        if (m.getId() == mId) collabRecs.add(m);
                    }
                }
            }
        }
        return collabRecs;
    }

    private double computeCosineSimilarity(Map<Integer, Double> r1, Map<Integer, Double> r2) {
        Set<Integer> commonMovies = new HashSet<>(r1.keySet());
        commonMovies.retainAll(r2.keySet());
        if (commonMovies.isEmpty()) return 0.0;

        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int mId : commonMovies) {
            dotProduct += r1.get(mId) * r2.get(mId);
        }
        for (double val : r1.values()) normA += Math.pow(val, 2);
        for (double val : r2.values()) normB += Math.pow(val, 2);

        return (normA == 0 || normB == 0) ? 0.0 : dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

// ==========================================
// 5. MAIN INTERFACE & ADMIN / THERAPIST SYSTEM
// ==========================================
public class Main {
    public static void main(String[] args) {
        List<Movie> database = MovieDatabaseLoader.loadFullDatabase();
        RecommenderEngine engine = new RecommenderEngine(database);
        Map<String, User> userDatabase = PersistenceManager.loadUsers();

        Scanner scanner = new Scanner(System.in);
        User currentUser = null;

        System.out.println("==================================================");
        System.out.println("🎬 CINEMATCH ENTERPRISE SECURE SYSTEM (V2.0) 🎬");
        System.out.println("==================================================");

        // --- AUTHENTICATION & LOGIN BUG FIX LOOP ---
        while (currentUser == null) {
            System.out.println("1. 🔐 Login to existing account");
            System.out.println("2. 📝 Register new account");
            System.out.print("Choose option (1 or 2): ");
            int authChoice = scanner.nextInt(); scanner.nextLine();

            System.out.print("Enter Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter Password: ");
            String password = scanner.nextLine().trim();

            if (authChoice == 1) {
                if (userDatabase.containsKey(username)) {
                    User storedUser = userDatabase.get(username);
                    // FIXED: Secure password verification
                    if (storedUser.getPassword().equals(password)) {
                        currentUser = storedUser;
                        currentUser.updateProfileMetrics(database);
                        System.out.println("✅ Welcome back, " + username + "! Login successful.");
                    } else {
                        System.out.println("❌ Invalid password! Access denied.");
                    }
                } else {
                    System.out.println("❌ Username not found. Please register first.");
                }
            } else {
                if (userDatabase.containsKey(username)) {
                    System.out.println("❌ Username already taken.");
                } else {
                    currentUser = new User(username, password);
                    userDatabase.put(username, currentUser);
                    PersistenceManager.saveUsers(userDatabase);
                    System.out.println("✅ Account created successfully and saved!");
                }
            }
        }

        // --- MAIN APPLICATION MENU ---
        boolean running = true;
        while (running) {
            System.out.println("\n==================================================");
            System.out.println("--- MAIN MENU (" + currentUser.getUsername() + ") ---");
            System.out.println("==================================================");
            System.out.println("1. 🏠 Home Feed (Hybrid & Profile Recommendations)");
            System.out.println("2. 🔍 Search Movies (Title, Actor, Genre, Director)");
            System.out.println("3. 😊 Mood-Based Recommendations");
            System.out.println("4. 🔥 Trending Dashboard (Top Rated & Most Viewed)");
            System.out.println("5. ⭐ Rate a Movie (Validated)");
            System.out.println("6. 📋 Manage Watchlist (ID-based)");
            System.out.println("7. 🧠 AI Movie Therapist (Emotional Companion)");
            System.out.println("8. 👤 View My User Profile & Stats");
            if (currentUser.getUsername().equalsIgnoreCase("admin")) {
                System.out.println("9. 🛠️ Admin Control Panel");
            }
            System.out.println("0. 🚪 Exit & Save Session");
            System.out.print("Select an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
            case 1:
                System.out.println("\n==================================================");
                System.out.println("--- 🏠 PERSONALIZED HYBRID HOME FEED ---");
                System.out.println("==================================================");
                System.out.println("🎯 Tailored Recommendations (Content + Collaborative):");
                List<Movie> hybridRecs = engine.getHybridRecommendations(currentUser, userDatabase);
                if (currentUser.getRatings().isEmpty()) {
                    System.out.println("   (Tip: Rate a few movies to unlock personalized AI matches! Showing Top Rated below)");
                }
                for (Movie m : hybridRecs) {
                    System.out.printf("   -> [%d] %s (⭐ %.1f / 5.0) | Genre: %s\n", m.getId(), m.getTitle(), m.getRating(), m.getGenre());
                }

                System.out.println("\n--------------------------------------------------");
                System.out.print("Would you like to explore a variety of options from the full catalog? (y/n): ");
                String exploreChoice = scanner.nextLine().trim().toLowerCase();

                if (exploreChoice.equals("y") || exploreChoice.equals("yes")) {
                    boolean browsingCatalog = true;
                    while (browsingCatalog) {
                        System.out.println("\n--- 📂 SELECT A MOVIE CATEGORY ---");
                        System.out.println("1. 🎬 Hollywood Blockbusters (IDs 1-10)");
                        System.out.println("2. 🍿 Bollywood Masterpieces (IDs 11-20)");
                        System.out.println("3. ⚡ Pan-Indian Epics (IDs 21-30)");
                        System.out.println("4. 🔙 Return to Main Menu");
                        System.out.print("Choose category (1-4): ");
                        int catChoice = scanner.nextInt(); scanner.nextLine();

                        if (catChoice == 4) {
                            browsingCatalog = false;
                            break;
                        }

                        List<Movie> subList = new ArrayList<>();
                        String catName = "";
                        if (catChoice == 1) {
                            catName = "Hollywood Blockbusters";
                            for (Movie m : database) { if (m.getId() >= 1 && m.getId() <= 10) subList.add(m); }
                        } else if (catChoice == 2) {
                            catName = "Bollywood Masterpieces";
                            for (Movie m : database) { if (m.getId() >= 11 && m.getId() <= 20) subList.add(m); }
                        } else if (catChoice == 3) {
                            catName = "Pan-Indian Epics";
                            for (Movie m : database) { if (m.getId() >= 21 && m.getId() <= 30) subList.add(m); }
                        }

                        if (!subList.isEmpty()) {
                            System.out.println("\n--- " + catName.toUpperCase() + " ---");
                            for (Movie m : subList) {
                                System.out.printf("   [%d] %s (⭐ %.1f) | %s\n", m.getId(), m.getTitle(), m.getRating(), m.getGenre());
                            }

                            System.out.println("\nEnter a Movie ID from above to inspect its details (or 0 to go back to categories): ");
                            int inspectId = scanner.nextInt(); scanner.nextLine();

                            if (inspectId != 0) {
                                Movie targetInspect = null;
                                for (Movie m : database) {
                                    if (m.getId() == inspectId) {
                                        targetInspect = m;
                                        break;
                                    }
                                }

                                if (targetInspect != null) {
                                    targetInspect.incrementViewCount();
                                    System.out.println("\n==================================================");
                                    System.out.println("🎬 MOVIE DEEP-DIVE INSPECTION");
                                    System.out.println("==================================================");
                                    System.out.println(targetInspect);
                                    System.out.println("==================================================");
                                } else {
                                    System.out.println("❌ Invalid Movie ID.");
                                }
                            }
                        }
                    }
                }
                break;

                case 2:
                    System.out.println("\n--- Search By: 1. Title | 2. Actor | 3. Genre | 4. Director ---");
                    int sType = scanner.nextInt(); scanner.nextLine();
                    System.out.print("Enter search keyword: ");
                    String keyword = scanner.nextLine();
                    
                    List<Movie> searchResults = new ArrayList<>();
                    if (sType == 1) searchResults = engine.searchByTitle(keyword);
                    else if (sType == 2) searchResults = engine.searchByActor(keyword);
                    else if (sType == 3) searchResults = engine.searchByGenre(keyword);
                    else if (sType == 4) searchResults = engine.searchByDirector(keyword);

                    System.out.println("\nFound " + searchResults.size() + " matches (Sorted Descending):");
                    for (Movie m : searchResults) {
                        m.incrementViewCount();
                        System.out.println("\n" + m);
                    }
                    break;

                case 3:
                    boolean exploringMood = true;
                    while (exploringMood) {
                        System.out.println("\n==================================================");
                        System.out.println("--- 😊 MOOD-BASED RECOMMENDATIONS ---");
                        System.out.println("==================================================");
                        System.out.println("Moods available: Happy, Sad, Excited, Romantic, Motivational");
                        System.out.print("Enter mood: ");
                        String mood = scanner.nextLine();
                        
                        List<Movie> moodList = engine.getMoodRecommendations(mood);
                        if (moodList.isEmpty()) {
                            System.out.println("❌ No movies found for mood: '" + mood + "'. Try another mood.");
                            break;
                        }

                        System.out.println("\nFound " + moodList.size() + " movies for mood '" + mood + "':");
                        for (Movie m : moodList) {
                            System.out.printf("   -> [%d] %s (⭐ %.1f) | Genre: %s\n", m.getId(), m.getTitle(), m.getRating(), m.getGenre());
                        }

                        boolean moodActionActive = true;
                        while (moodActionActive) {
                            System.out.println("\nOptions:");
                            System.out.println("  [Movie ID] Enter the ID of a movie above to view full details");
                            System.out.println("  [0] Change mood or go back");
                            System.out.print("Select choice: ");
                            
                            int inputId = scanner.nextInt(); scanner.nextLine();

                            if (inputId == 0) {
                                moodActionActive = false;
                                exploringMood = false;
                            } else {
                                Movie selectedMovie = null;
                                for (Movie m : database) {
                                    if (m.getId() == inputId) {
                                        selectedMovie = m;
                                        break;
                                    }
                                }

                                if (selectedMovie != null) {
                                    selectedMovie.incrementViewCount();
                                    System.out.println("\n==================================================");
                                    System.out.println("🎬 MOVIE DEEP-DIVE INSPECTION");
                                    System.out.println("==================================================");
                                    System.out.println(selectedMovie);
                                    System.out.println("==================================================");

                                    System.out.println("\nWhat would you like to do next?");
                                    System.out.println("1. Inspect another movie from this mood list");
                                    System.out.println("2. Return to Main Menu");
                                    System.out.print("Choose option (1 or 2): ");
                                    int nextStep = scanner.nextInt(); scanner.nextLine();

                                    if (nextStep == 2) {
                                        moodActionActive = false;
                                        exploringMood = false;
                                    }
                                } else {
                                    System.out.println("❌ Invalid Movie ID from this list. Try again.");
                                }
                            }
                        }
                    }
                    break;

                case 4:
                    System.out.println("\n==================================================");
                    System.out.println("🔥 TRENDING DASHBOARD 🔥");
                    System.out.println("==================================================");
                    System.out.println("🏆 Top Rated Podium:");
                    List<Movie> topMovies = engine.getHighestRated(3);
                    for (int i = 0; i < topMovies.size(); i++) {
                        Movie m = topMovies.get(i);
                        String medal = (i == 0) ? "🥇" : (i == 1) ? "🥈" : "🥉";
                        System.out.printf(" %s %s (⭐ %.1f) | Dir: %s\n", medal, m.getTitle(), m.getRating(), m.getDirector());
                    }
                    
                    System.out.println("\n👁️ Most Viewed Popular Movies:");
                    List<Movie> topViewed = engine.getTopViewed(3);
                    for (Movie m : topViewed) {
                        System.out.printf("   - %s (Views: %d)\n", m.getTitle(), m.getViewCount());
                    }
                    break;

                case 5:
                    System.out.println("\n--- ⭐ RATE A MOVIE ---");
                    System.out.println("📋 Quick Movie ID Reference List:");
                    for (Movie m : database) {
                        System.out.printf("   [%d] %s\n", m.getId(), m.getTitle());
                    }
                    
                    System.out.print("\nEnter Movie ID to rate (1-30): ");
                    int mId = scanner.nextInt(); scanner.nextLine();
                    
                    Movie targetMovie = null;
                    for (Movie m : database) {
                        if (m.getId() == mId) {
                            targetMovie = m;
                            break;
                        }
                    }

                    if (targetMovie == null) {
                        System.out.println("❌ Error: Invalid Movie ID. Please check the list above.");
                        break;
                    }

                    System.out.print("Rate '" + targetMovie.getTitle() + "' (1.0 to 5.0 stars): ");
                    double stars = scanner.nextDouble(); scanner.nextLine();
                    
                    currentUser.getRatings().put(targetMovie.getId(), stars);
                    currentUser.updateProfileMetrics(database); 
                    PersistenceManager.saveUsers(userDatabase);
                    System.out.println("✅ Saved review for '" + targetMovie.getTitle() + "' permanently!");
                    break;

                case 6:
                    System.out.println("\n--- 📋 WATCHLIST (ID-Based) ---");
                    System.out.print("Your Watchlist Movie IDs: " + currentUser.getWatchlist() + "\n");
                    System.out.println("1. Add Movie by ID\n2. View Watchlist Details");
                    int wChoice = scanner.nextInt(); scanner.nextLine();
                    if (wChoice == 1) {
                        System.out.print("Enter Movie ID to add: ");
                        int addId = scanner.nextInt(); scanner.nextLine();
                        currentUser.getWatchlist().add(addId);
                        PersistenceManager.saveUsers(userDatabase);
                        System.out.println("✅ Added movie ID " + addId + " to watchlist!");
                    } else {
                        System.out.println("Your Watchlist Titles:");
                        for (int id : currentUser.getWatchlist()) {
                            for (Movie m : database) {
                                if (m.getId() == id) System.out.println("   - " + m.getTitle() + " (" + m.getGenre() + ")");
                            }
                        }
                    }
                    break;

                case 7:
                    System.out.println("\n==================================================");
                    System.out.println("🧠 AI MOVIE THERAPIST 🧠");
                    System.out.println("==================================================");
                    System.out.println("Tell me how you are feeling or what you are going through (e.g., 'I failed my exam', 'I\\'m stressed', 'I want motivation'): ");
                    String userText = scanner.nextLine().toLowerCase();

                    String detectedMood = "Motivational";
                    String reason = "Here is a powerful film to help inspire and shift your perspective.";

                    if (userText.contains("exam") || userText.contains("fail") || userText.contains("study") || userText.contains("pressure") || userText.contains("stressed") || userText.contains("tension")) {
                        detectedMood = "Motivational";
                        reason = "You're under a lot of pressure. Stories like 3 Idiots, Chhichhore, and Dangal remind us that hard work pays off and setbacks don't define our future.";
                    } 
                    else if (userText.contains("sad") || userText.contains("lonely") || userText.contains("cry") || userText.contains("down") || userText.contains("heartbroken") || userText.contains("depressed")) {
                        detectedMood = "Motivational";
                        reason = "It's completely okay to feel low right now. These heartwarming films remind us that hope and inner strength can pull us through dark times.";
                    } 
                    else if (userText.contains("motivation") || userText.contains("inspire") || userText.contains("goal") || userText.contains("push") || userText.contains("lost")) {
                        detectedMood = "Motivational";
                        reason = "Channel that ambition! These films will ignite your drive and help you push past your limits.";
                    }
                    else if (userText.contains("happy") || userText.contains("joy") || userText.contains("great") || userText.contains("excited") || userText.contains("fun")) {
                        detectedMood = "Happy";
                        reason = "That's wonderful to hear! Keep that positive energy rolling with these lighthearted, feel-good hits.";
                    }
                    else if (userText.contains("bored") || userText.contains("action") || userText.contains("thrill") || userText.contains("adrenaline")) {
                        detectedMood = "Excited";
                        reason = "Let's shake off the boredom with some high-octane entertainment!";
                    }

                    System.out.println("\n🤖 AI Therapist Analysis: Detected emotional state -> [" + detectedMood.toUpperCase() + "]");
                    System.out.println("💡 " + reason);
                    System.out.println("\nRecommended Curated Watchlist:");
                    
                    List<Movie> therapistRecs = engine.getMoodRecommendations(detectedMood);
                    for (int i = 0; i < Math.min(3, therapistRecs.size()); i++) {
                        Movie m = therapistRecs.get(i);
                        System.out.printf("   ⭐ [%d] %s (%d) | Dir: %s\n     Plot: %s\n\n", m.getId(), m.getTitle(), m.getReleaseYear(), m.getDirector(), m.getDescription());
                    }
                    break;

                case 8:
                    System.out.println("\n==================================================");
                    System.out.println("👤 USER PROFILE & PREFERENCES");
                    System.out.println("==================================================");
                    System.out.println("Username: " + currentUser.getUsername());
                    System.out.println("❤️ Favorite Genre: " + currentUser.getFavoriteGenre());
                    System.out.println("🎭 Favorite Actor: " + currentUser.getFavoriteActor());
                    System.out.println("🎬 Favorite Director: " + currentUser.getFavoriteDirector());
                    System.out.println("⭐ Total Movies Rated: " + currentUser.getRatings().size());
                    System.out.println("📋 Watchlist Items Count: " + currentUser.getWatchlist().size());
                    break;

                case 9:
                    if (currentUser.getUsername().equalsIgnoreCase("admin")) {
                        System.out.println("\n==================================================");
                        System.out.println("🛠️ ADMIN CONTROL PANEL");
                        System.out.println("==================================================");
                        System.out.println("Total Registered Users: " + userDatabase.size());
                        System.out.println("Registered Usernames: " + userDatabase.keySet());
                        System.out.println("Total Database Size: " + database.size() + " Movies");
                        System.out.println("System Status: Active & Secured with File Persistence ✅");
                    } else {
                        System.out.println("❌ Unauthorized access.");
                    }
                    break;

                case 0:
                    PersistenceManager.saveUsers(userDatabase);
                    running = false;
                    System.out.println("💾 All progress successfully saved to disk. Goodbye! 👋");
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
        scanner.close();
    }
}