import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Test {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("ChangeMe@2025!");
        System.out.println("Hash: " + hash);
        boolean match = encoder.matches("ChangeMe@2025!", hash);
        System.out.println("Match: " + match);
        
        try {
            org.springframework.security.core.userdetails.User u = new org.springframework.security.core.userdetails.User(
                "user", hash, true, true, true, true, java.util.Collections.emptyList()
            );
            System.out.println("User created: " + u.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
