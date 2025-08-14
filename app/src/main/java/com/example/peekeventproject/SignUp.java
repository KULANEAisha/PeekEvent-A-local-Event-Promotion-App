package com.example.peekeventproject;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peekeventproject.SignIn;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.peekeventproject.SignIn;



import java.util.Calendar;

public class SignUp extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private TextInputEditText edtFullName, edtEmailAddress, edtPassword, edtPhoneNumber, edtDateCreated;
    private Button btnSignUp;
    private TextView txtSignInRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Link UI elements
        edtFullName = findViewById(R.id.edtFullName);
        edtEmailAddress = findViewById(R.id.edtEmailAddress);
        edtPassword = findViewById(R.id.edtPassword);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtDateCreated = findViewById(R.id.edtDateCreated);
        btnSignUp = findViewById(R.id.btnSignUp);
        txtSignInRedirect = findViewById(R.id.txtSignInRedirect);

        // Date Picker
        edtDateCreated.setOnClickListener(v -> showDatePicker());

        // Sign Up
        btnSignUp.setOnClickListener(v -> {
            String name = edtFullName.getText().toString().trim();
            String email = edtEmailAddress.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String phone = edtPhoneNumber.getText().toString().trim();
            String date = edtDateCreated.getText().toString().trim();

            if (validateInputs(name, email, password, phone, date)) {
                btnSignUp.setEnabled(false);
                btnSignUp.setText("Creating Account...");
                createUserWithEmailAndPassword(name, email, password, phone, date);
            }
        });

        // Go to SignIn
        txtSignInRedirect.setOnClickListener(v -> {
            startActivity(new Intent(SignUp.this, com.example.peekeventproject.SignIn.class));
            finish();
        });
    }

    private boolean validateInputs(String name, String email, String password, String phone, String date) {
        boolean isValid = true;

        if (name.isEmpty()) {
            edtFullName.setError("Name required");
            isValid = false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmailAddress.setError("Valid email required");
            isValid = false;
        }

        if (password.length() < 6) {
            edtPassword.setError("Minimum 6 characters");
            isValid = false;
        }

        if (phone.length() < 10) {
            edtPhoneNumber.setError("Valid phone number required");
            isValid = false;
        }

        if (date.isEmpty()) {
            edtDateCreated.setError("Date required");
            isValid = false;
        }

        return isValid;
    }

    private void createUserWithEmailAndPassword(String name, String email, String password, String phone, String date) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            btnSignUp.setEnabled(true);
            btnSignUp.setText("Sign Up");

            if (task.isSuccessful()) {
                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    storeUserData(userId, name, email, phone, date);
                }
            } else {
                Toast.makeText(SignUp.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void storeUserData(String userId, String name, String email, String phone, String date) {
        User user = new User(name, email, phone, date);
        databaseReference.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "User data saved to DB", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignUp.this, SignIn.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this, "Data save failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR), month = calendar.get(Calendar.MONTH), day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dialog = new DatePickerDialog(SignUp.this, (view, y, m, d) -> {
            edtDateCreated.setText(d + "/" + (m + 1) + "/" + y);
        }, year, month, day);
        dialog.show();
    }

    // User model class
    public static class User {
        public String name, email, phone, dateCreated;

        public User() {}

        public User(String name, String email, String phone, String dateCreated) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.dateCreated = dateCreated;
        }
    }
}

