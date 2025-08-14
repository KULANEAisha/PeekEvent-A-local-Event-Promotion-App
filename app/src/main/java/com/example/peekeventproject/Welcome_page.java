package com.example.peekeventproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class Welcome_page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_page);

        // Initialize buttons
        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        Button btnSignIn = findViewById(R.id.btnSignIn);

        // Get Started Button Click Listener
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Welcome_page.this, SignUp.class));
            }
        });

        // Sign In Button Click Listener
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Welcome_page.this, SignIn.class));
            }
        });
    }
}
