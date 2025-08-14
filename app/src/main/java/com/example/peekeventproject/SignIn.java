
package com.example.peekeventproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.peekeventproject.MainActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignIn extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLogin;
    private TextView txtForgotPassword, txtSignUpRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        // Link views
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        txtSignUpRedirect = findViewById(R.id.txtSignUpRedirect);

        // Pre-fill email from SignUp
        String emailFromSignUp = getIntent().getStringExtra("email");
        if (emailFromSignUp != null) {
            edtEmail.setText(emailFromSignUp);
        }

        // Login button listener
        btnLogin.setOnClickListener(v -> loginUser());

        // Forgot Password
        txtForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Sign Up Redirect
        txtSignUpRedirect.setOnClickListener(v -> {
            startActivity(new Intent(com.example.peekeventproject.SignIn.this, SignUp.class));
            finish();
        });
    }

    private void loginUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email is required");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Please enter a valid email");
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Password is required");
            edtPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            edtPassword.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(com.example.peekeventproject.SignIn.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(com.example.peekeventproject.SignIn.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(com.example.peekeventproject.SignIn.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(com.example.peekeventproject.SignIn.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleForgotPassword() {
        String email = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email is required to reset password");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Please enter a valid email");
            edtEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(com.example.peekeventproject.SignIn.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(com.example.peekeventproject.SignIn.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
