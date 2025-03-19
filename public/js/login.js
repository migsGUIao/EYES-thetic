import { auth, db } from './firebase-config.js';
import { getDocs, collection, query, where } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-firestore.js';
import { signInWithEmailAndPassword, onAuthStateChanged, signOut } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-auth.js';

onAuthStateChanged(auth, (user) => {
    const currentPath = window.location.pathname;
    const loginBtn = document.getElementById("loginBtn");
    const signupBtn = document.getElementById("signupBtn");
    const logoutBtn = document.getElementById("logoutBtn");
    const closetBtn = document.getElementById("closetBtn");
    const favBtn = document.getElementById("favBtn");
    const closetRecBtn = document.getElementById("closetRecBtn");

    if (user) {
        console.log(`User is logged in: ${user.email}`);
        if (![ "/login", "/signup"].includes(currentPath)) {
            loginBtn.classList.add("hidden");
            signupBtn.classList.add("hidden");
            logoutBtn.classList.remove("hidden");
            closetBtn.classList.remove("hidden");
            favBtn.classList.remove("hidden");
        }
        
        if (currentPath === "/homepage") {
            closetRecBtn.classList.remove("hidden");
        }

        // If the user is on the login page but already authenticated, redirect to homepage
        if (currentPath === "/login") {
            window.location.href = "/homepage";
        }

    } else {
        if (![ "/login", "/signup"].includes(currentPath)) {
            console.log("User is logged out.");
            loginBtn.classList.remove("hidden");
            signupBtn.classList.remove("hidden");
            logoutBtn.classList.add("hidden");
            closetBtn.classList.add("hidden");
            favBtn.classList.add("hidden");
            closetRecBtn.classList.add("hidden");
        }
        
        // Redirect to login **ONLY** if the user is on a protected page
        const protectedPages = [ "/closet", "/favorites"];
        if (protectedPages.includes(currentPath)) {
            window.location.href = "/login";
        }
    }
});

// Handle Login with email
document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    if (loginForm) {
        console.log("Found login form, adding event listener...");
        
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const email = document.getElementById("email").value.trim();
            const password = document.getElementById("password").value.trim();

            try {
                // Retrieve email from Firestore using username
                console.log(`Logging in with email: ${email}`);

                // Sign in with Firebase Auth
                const userCredential = await signInWithEmailAndPassword(auth, email, password);
                const user = userCredential.user;
                const token = await user.getIdToken();

                console.log("Firebase Token:", token);

                // Send token to backend for verification
                const response = await fetch("http://localhost:3000/login", {
                    method: "POST",
                    headers: { 
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${token}`
                    }
                });

                const result = await response.json();
                console.log("Server Response:", result);

                if (result.success) {
                    alert("Login successful!");
                    window.location.href = "/homepage";
                } else {
                    alert(result.message);
                }
            } catch (error) {
                console.error("Login error:", error);
                alert(error.message);
            }
        });
    } else {
        console.log("No login form found, skipping login logic.");
    }
});

document.addEventListener("DOMContentLoaded", () => {
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        console.log("Found logout button, adding event listener...");
        logoutBtn.addEventListener("click", async () => {
            try {
                await signOut(auth); // Logs user out of Firebase
                console.log("User logged out successfully.");

                // Notify the backend (optional)
                await fetch("http://localhost:3000/logout", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" }
                });

                // Redirect to login page
                window.location.href = "/login";
            } catch (error) {
                console.error("Logout error:", error);
                alert("Logout failed. Try again.");
            }
        });
    }
});

document.addEventListener("DOMContentLoaded", function() {

    if (window.location.pathname.toLowerCase().endsWith("/login")) {
        const emailField = document.getElementById("email");
        const passwordField = document.getElementById("password");
        const login = document.getElementById("login");

        emailField.focus();
        speakText("You are currently in the login page. Please enter your email. Press TAB to enter password.");
        
        passwordField.addEventListener("focus", function () {
            speakText("Please enter your password");
        });

        login.addEventListener("focus", function () {
            speakText("Click Login! If you still don't have an account, press 'S' to sign up!");
        });
    }
});

function speakText(text) {
    // TTS
    window.speechSynthesis.cancel(); // Addresses simultaneous TTS
    const utterance = new SpeechSynthesisUtterance(text);

    utterance.lang = 'en-US';  
    utterance.volume = 1;
    utterance.rate = 1;
    utterance.pitch = 1;

    window.speechSynthesis.speak(utterance);
}

document.addEventListener('keydown', (e) => {
    if (window.location.pathname.toLowerCase().endsWith("/login")) {
        const tag = document.activeElement.tagName;
        if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') {
            return;
        }

        if (e.key === "Escape") {
            resetKeyBuffer();
            return;
        }

        switch (e.key) {
            case "S":
            case "s":
            window.speechSynthesis.cancel();
            window.location.href = "/signup";
            return;
            default:
            break;
        }
    }
});