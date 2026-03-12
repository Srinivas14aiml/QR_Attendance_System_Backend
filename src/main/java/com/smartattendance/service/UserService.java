package com.smartattendance.service;

import com.smartattendance.model.User;
import com.smartattendance.model.UserRole;
import com.smartattendance.model.Student;
import com.smartattendance.model.Teacher;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.repository.TeacherRepository;
import com.smartattendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerNewUser(String username, String rawPassword, String fullName, UserRole role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        String encoded = passwordEncoder.encode(rawPassword);
        User user = new User(username, encoded, fullName, role);
        User saved = userRepository.save(user);
        if (role == UserRole.ROLE_TEACHER) {
            teacherRepository.save(new Teacher(saved));
        } else {
            studentRepository.save(new Student(fullName, username, saved));
        }
        return saved;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
