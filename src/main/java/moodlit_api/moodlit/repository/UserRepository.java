package moodlit_api.moodlit.repository;

import moodlit_api.moodlit.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Email/password auth
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Apple Sign In
    Optional<User> findByAppleUserId(String appleUserId);
    boolean existsByAppleUserId(String appleUserId);
}