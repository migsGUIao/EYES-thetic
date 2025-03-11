//import { auth } from "/firebase-config.js";
//import { createUserWithEmailAndPassword } from "firebase/auth";
document.addEventListener("DOMContentLoaded", function () {
    const signupForm = document.getElementById("signupForm");

    if (!signupForm) {
        console.error("Signup form not found!");
        return;
    }

    signupForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const displayName = document.getElementById("displayName").value.trim();
        const username = document.getElementById("username").value.trim(); 
        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value.trim();

        if (!username || !email || !password) {
            alert("Email and Password are required!");
            return;
        }

        try {
            const response = await fetch("/signup", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ displayName, username, email, password }),
            });

            const result = await response.json();
            if (result.success) {
                alert("Signup successful!");
                window.location.href = "/login.html";
            } else {
                alert(`Signup error: ${result.message}`);
            }
        } catch (error) {
            console.error("Error:", error);
            alert("Something went wrong.");
        }
    });
});

