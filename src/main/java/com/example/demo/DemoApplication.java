package com.example.demo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

interface UsersRepository extends JpaRepository<User, UUID> {
  Collection<User> findAllByEmailContainsIgnoreCase(@Param("email") String email);

  default Collection<User> findAny(String by) {
    return findAllByEmailContainsIgnoreCaseOrFullNameContainsIgnoreCase(by, by);
  }

  Collection<User> findAllByEmailContainsIgnoreCaseOrFullNameContainsIgnoreCase(
      @Param("email") String email,
      @Param("fullName") String fullName
  );
}

@Data
@Entity
@Table(name = "users", schema = "public")
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor//(access = AccessLevel.PROTECTED)
class User extends RepresentationModel<User> {

  @Id
  @GeneratedValue
  // @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_sequence")
  // @SequenceGenerator(name = "users_sequence", sequenceName = "users_sequence", allocationSize = 1)
  private UUID id;

  @NonNull
  private String email, fullName;

  @Version
  private Long version;
}

@RestController
@RequiredArgsConstructor
class UsersResource {

  final UsersRepository usersRepository;

  // static final BiFunction<HttpServletRequest, User, RepresentationModel<User>> linked = (request, user) -> {
  //   var uri = URI.create(request.getRequestURL().toString());
  //   return user.add(new Link(String.format("%s://%s/%s", uri.getScheme(), uri.getAuthority(), user.getId())));
  // };
  // static final Function<HttpServletRequest, Function<User, RepresentationModel<User>>> mapper =
  //     httpServletRequest -> user -> linked.apply(httpServletRequest, user);

  static final Function<HttpServletRequest, Function<User, RepresentationModel<User>>> mapper = request -> user -> {
    var uri = URI.create(request.getRequestURL().toString());
    return user.add(new Link(String.format("%s://%s/%s", uri.getScheme(), uri.getAuthority(), user.getId())));
  };

  @GetMapping("/collect")
  Collection<RepresentationModel<User>> collect(@RequestParam("by") String by, HttpServletRequest serverHttpRequest) {
    return usersRepository.findAny(by).stream()
                          .map(mapper.apply(serverHttpRequest))
                          .collect(Collectors.toList());
  }

  @GetMapping("/{id}")
  Optional<RepresentationModel<User>> getOne(@PathVariable UUID id, HttpServletRequest serverHttpRequest) {
    return usersRepository.findById(id)
                          .map(mapper.apply(serverHttpRequest));
  }

  @GetMapping("/")
  Collection<RepresentationModel<User>> getAll(HttpServletRequest serverHttpRequest) {
    return usersRepository.findAll().stream()
                          .map(mapper.apply(serverHttpRequest))
                          .collect(Collectors.toList());
  }
}

@Component
@Profile("!prod")
@RequiredArgsConstructor
class TestData implements InitializingBean {

  final UsersRepository usersRepository;

  @Override
  public void afterPropertiesSet() throws Exception {
    Stream.of("ololo", "trololo")
          .map(s -> User.of(s + "@example.com", s))
          .forEach(usersRepository::save);
  }
}

@SpringBootApplication
public class DemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }
}
